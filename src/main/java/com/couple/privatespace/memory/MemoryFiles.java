package com.couple.privatespace.memory;

import com.couple.privatespace.identity.ApiFailure;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

/** DEV-07 MEM-01/02: private bytes, bounded native probing, no filename-derived storage paths. */
@Component
public class MemoryFiles {
    public record Validated(UUID id,String name,String kind,String mime,long size,String sha,Double duration,Integer width,Integer height) { }
    public static final Map<String,String> TYPES=Map.of("jpg","image/jpeg","jpeg","image/jpeg","png","image/png","webp","image/webp","mp4","video/mp4","pdf","application/pdf","docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document","xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
    final Path root,stage,objects;
    private final String ffprobe,ffmpeg;
    private final Semaphore workers=new Semaphore(2);
    private final JsonMapper json=new JsonMapper();
    private final RemoteMemoryStore remote;
    public MemoryFiles(@Value("${app.memories.storage-root}") String root,@Value("${app.memories.ffprobe}") String ffprobe,@Value("${app.memories.ffmpeg}") String ffmpeg,ObjectProvider<RemoteMemoryStore> remote)throws IOException{
        this.remote=remote.getIfAvailable();
        this.root=Path.of(root).toAbsolutePath().normalize();this.stage=this.root.resolve("staging");this.objects=this.root.resolve("objects");this.ffprobe=ffprobe;this.ffmpeg=ffmpeg;
        Files.createDirectories(stage);Files.createDirectories(objects);
        for(Path p=this.root;p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Private storage must not use symbolic links");
    }
    public Path path(UUID id,boolean thumb){return objects.resolve(id+(thumb?".jpg":".data"));}
    private Path staged(UUID id,boolean thumb){return stage.resolve(id+(thumb?".jpg":".data"));}
    public Validated validate(MultipartFile file){
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);
        if(name.isBlank()||name.codePointCount(0,name.length())>180||name.codePoints().anyMatch(Character::isISOControl))throw failure("INVALID_FILE_NAME","Choose a file with a short, valid name.");
        String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT),mime=TYPES.get(ext);
        if(mime==null)throw failure("UNSUPPORTED_FILE","Use JPG, PNG, WebP, MP4, PDF, DOCX, XLSX or PPTX.");
        String kind=ext.equals("mp4")?"VIDEO":Set.of("jpg","jpeg","png","webp").contains(ext)?"IMAGE":"DOCUMENT";
        long limit=kind.equals("VIDEO")?100000000L:kind.equals("IMAGE")?10000000L:20000000L;
        if(file.getSize()<1||file.getSize()>limit)throw failure("FILE_TOO_LARGE","This file exceeds its upload limit or is empty.");
        String supplied=file.getContentType();
        if(supplied!=null&&!supplied.equals(mime)&&!supplied.equals("application/octet-stream"))throw failure("FILE_TYPE_MISMATCH","The file content type does not match its extension.");
        UUID id=UUID.randomUUID();boolean acquired=false;
        try{
            if(!workers.tryAcquire(1,TimeUnit.SECONDS))throw new ApiFailure(429,"UPLOAD_BUSY","Two uploads are being checked. Try again shortly.");acquired=true;
            if(Files.getFileStore(root).getUsableSpace()<file.getSize()+50000000L)throw new ApiFailure(507,"STORAGE_FULL","Local storage is full. Your existing memories are safe.");
            var digest=MessageDigest.getInstance("SHA-256");long size=0;
            try(var in=file.getInputStream();var out=Files.newOutputStream(staged(id,false),StandardOpenOption.CREATE_NEW)){
                byte[] bytes=new byte[16384];int n;while((n=in.read(bytes))!=-1){size+=n;if(size>limit)throw failure("FILE_TOO_LARGE","This file exceeds its upload limit.");digest.update(bytes,0,n);out.write(bytes,0,n);}
            }
            if(size==0)throw failure("INVALID_FILE","Choose a nonempty file.");
            Double duration=null;Integer width=null,height=null;
            if(kind.equals("DOCUMENT"))validateDocument(staged(id,false),ext);
            else{
                // Reject indirect playlists before invoking any demuxer that can open local references.
                byte[] magic;
                try(var in=Files.newInputStream(staged(id,false))){magic=in.readNBytes(12);}
                boolean signature=magic.length>=12&&switch(mime){
                    case "image/jpeg" -> (magic[0]&255)==255&&(magic[1]&255)==216&&(magic[2]&255)==255;
                    case "image/png" -> Arrays.equals(Arrays.copyOf(magic,8),new byte[]{(byte)137,80,78,71,13,10,26,10});
                    case "image/webp" -> new String(magic,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(magic,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
                    case "video/mp4" -> new String(magic,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp")&&!new String(magic,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("qt  ");
                    default -> false;
                };
                if(!signature)throw failure("FILE_TYPE_MISMATCH","The media content does not match its extension.");
                var data=json.readTree(run(id,List.of(ffprobe,"-v","error","-protocol_whitelist","file,pipe","-show_entries","stream=codec_name,codec_type,width,height,pix_fmt,duration:format=duration,format_name","-of","json",staged(id,false).toString()),20));
                var streams=data.get("streams");if(streams==null||!streams.isArray())throw failure("INVALID_MEDIA","This media file could not be read.");
                int videoCount=0;String codec="";
                for(var stream:streams){String type=stream.path("codec_type").asText();if(type.equals("video")){videoCount++;codec=stream.path("codec_name").asText();width=stream.path("width").asInt();height=stream.path("height").asInt();}
                    else if(!type.equals("audio")||!kind.equals("VIDEO")||!stream.path("codec_name").asText().equals("aac"))throw failure("UNSUPPORTED_MEDIA","Use MP4 video with H.264 and optional AAC audio.");}
                if(videoCount!=1||width==null||width<1||height<1||(long)width*height>40000000L)throw failure("INVALID_MEDIA","Choose a supported image or video resolution.");
                if(kind.equals("VIDEO")){
                    byte[] header=new byte[12];try(var in=Files.newInputStream(staged(id,false))){if(in.read(header)!=12||!new String(header,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp")||new String(header,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("qt  "))throw failure("UNSUPPORTED_MEDIA","Use an MP4 file with H.264 video.");}
                    if(!codec.equals("h264"))throw failure("UNSUPPORTED_MEDIA","Use MP4 video with H.264 and optional AAC audio.");
                    for(var stream:streams)if(stream.path("codec_type").asText().equals("video")&&!Set.of("yuv420p","yuvj420p").contains(stream.path("pix_fmt").asText()))throw failure("UNSUPPORTED_MEDIA","Use an 8-bit H.264 MP4 video for browser playback.");
                    duration=Double.parseDouble(data.path("format").path("duration").asText());if(!Double.isFinite(duration)||duration<=0||duration>180)throw failure("VIDEO_TOO_LONG","Keep videos within 3 minutes.");
                }else if(!(mime.equals("image/jpeg")&&codec.equals("mjpeg")||mime.equals("image/png")&&Set.of("png","apng").contains(codec)||mime.equals("image/webp")&&codec.equals("webp")))throw failure("FILE_TYPE_MISMATCH","The image content does not match its extension.");
                run(id,List.of(ffmpeg,"-v","error","-nostdin","-y","-threads","1","-protocol_whitelist","file,pipe","-i",staged(id,false).toString(),"-map","0:v:0","-frames:v","1","-vf","scale=640:640:force_original_aspect_ratio=decrease","-threads","1","-q:v","3",staged(id,true).toString()),30);
                if(!Files.isRegularFile(staged(id,true),LinkOption.NOFOLLOW_LINKS))throw failure("INVALID_MEDIA","This media file could not be decoded.");
            }
            return new Validated(id,name,kind,mime,size,HexFormat.of().formatHex(digest.digest()),duration,width,height);
        }catch(ApiFailure error){discardStaged(id);throw error;}
        catch(InterruptedException error){Thread.currentThread().interrupt();discardStaged(id);throw new ApiFailure(503,"UPLOAD_INTERRUPTED","Upload checking was interrupted. Retry.");}
        catch(Exception error){discardStaged(id);throw failure("INVALID_FILE","This file could not be validated. Check its format or try another file.");}
        finally{if(acquired)workers.release();}
    }
    private String run(UUID id,List<String> command,int seconds)throws IOException,InterruptedException{
        if(!Files.isRegularFile(Path.of(command.getFirst())))throw new ApiFailure(503,"MEDIA_TOOLS_MISSING","Media tools are not installed. Run scripts/setup-media-tools.ps1.");
        Path log=stage.resolve(id+".log");Process p=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try{if(!p.waitFor(seconds,TimeUnit.SECONDS)){p.destroyForcibly();p.waitFor(2,TimeUnit.SECONDS);throw failure("MEDIA_TIMEOUT","This media file took too long to check.");}if(p.exitValue()!=0||Files.size(log)>65536)throw failure("INVALID_MEDIA","This media file could not be read.");return Files.readString(log);}
        finally{if(p.isAlive())p.destroyForcibly();Files.deleteIfExists(log);}
    }
    private void validateDocument(Path file,String ext)throws Exception{
        if(ext.equals("pdf")){try(var pdf=Loader.loadPDF(file.toFile())){if(pdf.isEncrypted()||pdf.getNumberOfPages()<1||pdf.getNumberOfPages()>10000)throw failure("INVALID_DOCUMENT","Choose an unencrypted, readable PDF.");}return;}
        String main=switch(ext){case "docx"->"word/document.xml";case "xlsx"->"xl/workbook.xml";default->"ppt/presentation.xml";};
        String mainType=switch(ext){case "docx"->"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";case "xlsx"->"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";default->"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml";};
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
        try(var zip=new ZipFile(file.toFile())){
            if(zip.size()>2000||zip.getEntry(main)==null||zip.getEntry("[Content_Types].xml")==null||zip.getEntry("_rels/.rels")==null)throw failure("INVALID_DOCUMENT","Choose a valid Office document.");
            long total=0;var names=new HashSet<String>();var entries=zip.entries();while(entries.hasMoreElements()){
                var entry=entries.nextElement();String name=entry.getName().toLowerCase(Locale.ROOT);
                if(!names.add(name)||name.contains("..")||name.startsWith("/")||name.contains("\\")||name.contains("vbaproject")||name.endsWith(".exe")||name.endsWith(".js")||name.endsWith(".vbs")||name.contains("/embeddings/"))throw failure("UNSUPPORTED_DOCUMENT","Use a document without macros or embedded programs.");
                if(entry.isDirectory())continue;long part=0;try(var in=zip.getInputStream(entry)){byte[] bytes=new byte[16384];int n;while((n=in.read(bytes))!=-1){part+=n;total+=n;if(part>64000000||total>200000000||Set.of("[content_types].xml","_rels/.rels").contains(name)&&part>1000000)throw failure("INVALID_DOCUMENT","This Office file is too complex to validate.");}}
            }
            var builder=factory.newDocumentBuilder();org.w3c.dom.Document types;
            try(var in=zip.getInputStream(zip.getEntry("[Content_Types].xml"))){types=builder.parse(in);}boolean found=false;
            var overrides=types.getElementsByTagNameNS("*","Override");for(int i=0;i<overrides.getLength();i++){var e=(org.w3c.dom.Element)overrides.item(i);if(e.getAttribute("PartName").equals("/"+main)&&e.getAttribute("ContentType").equals(mainType))found=true;}
            if(!found)throw failure("FILE_TYPE_MISMATCH","The Office document does not match its extension.");
            boolean linked=false;try(var in=zip.getInputStream(zip.getEntry("_rels/.rels"))){var rels=builder.parse(in).getElementsByTagNameNS("*","Relationship");for(int i=0;i<rels.getLength();i++){var e=(org.w3c.dom.Element)rels.item(i);if(e.getAttribute("Type").endsWith("/officeDocument")&&!e.getAttribute("TargetMode").equals("External")&&e.getAttribute("Target").replaceFirst("^/","").equals(main))linked=true;}}
            if(!linked)throw failure("INVALID_DOCUMENT","The Office package does not contain its main document relationship.");
            var sax=javax.xml.parsers.SAXParserFactory.newInstance();sax.setNamespaceAware(true);sax.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);sax.setFeature("http://xml.org/sax/features/external-general-entities",false);sax.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            String expectedRoot=ext.equals("docx")?"document":ext.equals("xlsx")?"workbook":"presentation";
            try(var in=zip.getInputStream(zip.getEntry(main))){sax.newSAXParser().parse(in,new org.xml.sax.helpers.DefaultHandler(){boolean first=true;public void startElement(String uri,String local,String q,org.xml.sax.Attributes attrs)throws org.xml.sax.SAXException{if(first){first=false;String family=ext.equals("docx")?"wordprocessingml":ext.equals("xlsx")?"spreadsheetml":"presentationml";if(!local.equals(expectedRoot)||!Set.of("http://schemas.openxmlformats.org/"+family+"/2006/main","http://purl.oclc.org/ooxml/"+family+"/main").contains(uri))throw new org.xml.sax.SAXException("Wrong document root");}}});}
        }
    }
    /** CHAT-05: audio-only browser recordings normalized to inline-playable AAC/MP4. */
    public Validated validateVoice(MultipartFile file){
        if(file.getSize()<1||file.getSize()>10000000L)throw failure("VOICE_TOO_LARGE","Keep voice messages within 10 MB.");
        UUID id=UUID.randomUUID();Path encoded=stage.resolve(id+".m4a");boolean acquired=false;
        try{
            if(!workers.tryAcquire(1,TimeUnit.SECONDS))throw new ApiFailure(429,"UPLOAD_BUSY","Media checking is busy. Retry shortly.");acquired=true;
            long size=0;try(var in=file.getInputStream();var out=Files.newOutputStream(staged(id,false),StandardOpenOption.CREATE_NEW)){byte[] bytes=new byte[16384];int n;while((n=in.read(bytes))!=-1){size+=n;if(size>10000000L)throw failure("VOICE_TOO_LARGE","Keep voice messages within 10 MB.");out.write(bytes,0,n);}}
            byte[] head;try(var in=Files.newInputStream(staged(id,false))){head=in.readNBytes(12);}
            boolean signature=head.length>=12&&((head[0]&255)==0x1a&&(head[1]&255)==0x45&&(head[2]&255)==0xdf&&(head[3]&255)==0xa3||new String(head,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("OggS")||new String(head,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp"));
            if(!signature)throw failure("INVALID_VOICE","This recording is not a supported audio container.");
            var probe=json.readTree(run(id,List.of(ffprobe,"-v","error","-protocol_whitelist","file,pipe","-show_entries","stream=codec_type,codec_name,channels:format=duration","-of","json",staged(id,false).toString()),20));
            var streams=probe.path("streams");if(!streams.isArray()||streams.size()!=1||!streams.get(0).path("codec_type").asText().equals("audio")||!Set.of("aac","opus","vorbis").contains(streams.get(0).path("codec_name").asText())||streams.get(0).path("channels").asInt()<1||streams.get(0).path("channels").asInt()>2)throw failure("INVALID_VOICE","Record one audio track without video.");
            // Browser WebM may omit duration until decoded; normalize with a hard output bound, then probe it.
            String duration=probe.path("format").path("duration").asText();if(!duration.isBlank()&&!duration.equals("N/A")&&Double.parseDouble(duration)>300.5)throw failure("VOICE_TOO_LONG","Keep voice messages within 5 minutes.");
            run(id,List.of(ffmpeg,"-v","error","-nostdin","-y","-threads","1","-protocol_whitelist","file,pipe","-i",staged(id,false).toString(),"-map","0:a:0","-vn","-map_metadata","-1","-ac","1","-ar","48000","-c:a","aac","-b:a","64k","-t","301","-movflags","+faststart","-threads","1",encoded.toString()),90);
            var checked=json.readTree(run(id,List.of(ffprobe,"-v","error","-show_entries","format=duration","-of","json",encoded.toString()),20));double seconds=Double.parseDouble(checked.path("format").path("duration").asText());
            if(!Double.isFinite(seconds)||seconds<=0||seconds>300.5)throw failure("VOICE_TOO_LONG","Keep voice messages within 5 minutes.");
            Files.move(encoded,staged(id,false),StandardCopyOption.REPLACE_EXISTING);byte[] bytes=Files.readAllBytes(staged(id,false));if(bytes.length<1||bytes.length>10000000)throw failure("VOICE_TOO_LARGE","Keep voice messages within 10 MB.");
            return new Validated(id,"Voice message.m4a","VOICE","audio/mp4",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),seconds,null,null);
        }catch(ApiFailure error){discardStaged(id);throw error;}catch(InterruptedException error){Thread.currentThread().interrupt();discardStaged(id);throw new ApiFailure(503,"UPLOAD_INTERRUPTED","Recording check interrupted. Retry.");}
        catch(Exception error){discardStaged(id);throw failure("INVALID_VOICE","This recording could not be checked. Record again.");}
        finally{try{Files.deleteIfExists(encoded);}catch(IOException ignored){}if(acquired)workers.release();}
    }
    public void promote(Validated file)throws IOException{
        if(remote!=null){remote.put(file.id(),false,staged(file.id(),false));if(Set.of("IMAGE","VIDEO").contains(file.kind()))remote.put(file.id(),true,staged(file.id(),true));return;}
        Files.move(staged(file.id(),false),path(file.id(),false),StandardCopyOption.ATOMIC_MOVE);if(Set.of("IMAGE","VIDEO").contains(file.kind()))Files.move(staged(file.id(),true),path(file.id(),true),StandardCopyOption.ATOMIC_MOVE);
    }
    public boolean available(UUID id)throws IOException{return remote!=null?remote.exists(id,false):Files.isRegularFile(path(id,false),LinkOption.NOFOLLOW_LINKS);}
    public Resource resource(UUID id,boolean thumb)throws IOException{
        if(remote!=null)return remote.resource(id,thumb);
        Path p=path(id,thumb);if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new IOException("Private file unavailable");return new FileSystemResource(p);
    }
    public void reconcileRemote(java.util.function.Predicate<UUID> referenced)throws IOException{
        if(remote==null)return;
        var cutoff=java.time.Instant.now().minus(java.time.Duration.ofHours(24));
        for(var object:remote.scan())if(object.modified()!=null&&object.modified().isBefore(cutoff)&&!referenced.test(object.id()))remote.delete(object.id(),object.thumbnail());
    }
    public void discardStaged(UUID id){for(boolean thumb:new boolean[]{false,true})try{Files.deleteIfExists(staged(id,thumb));}catch(IOException ignored){/* aged staging maintenance retries */}}
    public void delete(UUID id)throws IOException{for(boolean thumb:new boolean[]{false,true}){if(remote!=null)remote.delete(id,thumb);Path p=path(id,thumb);if(Files.isSymbolicLink(p))throw new IOException("Unsafe storage entry");Files.deleteIfExists(p);}}
    private ApiFailure failure(String code,String message){return new ApiFailure(400,code,message);}
}
