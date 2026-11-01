package com.couple.privatespace;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.*;
import org.apache.pdfbox.pdmodel.*;

/** Reproducible synthetic files only. Native media tools produce actual playable clips. */
class MemoryFixtures {
 static final Path root=Path.of("target/memory-test-fixtures");
 static synchronized void prepare()throws Exception{
  Files.createDirectories(root);if(Files.exists(root.resolve("ready.txt")))return;
  var image=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(225,234,222));g.fillRect(0,0,640,480);g.setColor(new Color(255,236,191));g.fillOval(440,50,90,90);g.setColor(new Color(160,180,165));g.fillOval(-100,230,700,480);g.setColor(new Color(117,151,141));g.fillOval(240,280,650,450);g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.PLAIN,28));g.drawString("A moment for two",35,425);g.dispose();ImageIO.write(image,"png",root.resolve("moment.png").toFile());ImageIO.write(image,"jpg",root.resolve("moment.jpg").toFile());
  ff("-i",root.resolve("moment.png").toString(),root.resolve("moment.webp").toString());
  ff("-f","lavfi","-i","color=c=0x92aea0:s=160x120:r=1","-t","2","-c:v","libx264","-pix_fmt","yuv420p",root.resolve("short.mp4").toString());
  ff("-f","lavfi","-i","color=c=0x92aea0:s=16x16:r=1","-t","180","-c:v","libx264","-preset","ultrafast","-pix_fmt","yuv420p",root.resolve("180-seconds.mp4").toString());
  ff("-f","lavfi","-i","color=c=0x92aea0:s=16x16:r=1","-t","181","-c:v","libx264","-preset","ultrafast","-pix_fmt","yuv420p",root.resolve("181-seconds.mp4").toString());
  try(var pdf=new PDDocument()){pdf.addPage(new PDPage());pdf.save(root.resolve("note.pdf").toFile());}
  for(String ext:List.of("docx","xlsx","pptx"))Files.write(root.resolve("note."+ext),office(ext,false));
  Files.writeString(root.resolve("ready.txt"),"Synthetic memory fixtures v1");
 }
 static void ff(String...args)throws Exception{
  var command=new ArrayList<String>();command.add(".tools/ffmpeg/ffmpeg-8.1.2-essentials_build/bin/ffmpeg.exe");command.addAll(List.of("-v","error","-nostdin","-y","-threads","1"));command.addAll(Arrays.asList(args));var process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(root.resolve("fixture-tool.log").toFile()).start();if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();throw new IOException("Fixture tool timeout");}if(process.exitValue()!=0)throw new IOException("Fixture generation failed: "+Files.readString(root.resolve("fixture-tool.log")));
 }
 static byte[] office(String ext,boolean malicious)throws Exception{
  String main=ext.equals("docx")?"word/document.xml":ext.equals("xlsx")?"xl/workbook.xml":"ppt/presentation.xml";
  String type=ext.equals("docx")?"wordprocessingml.document":ext.equals("xlsx")?"spreadsheetml.sheet":"presentationml.presentation";
  String xml=ext.equals("docx")?"<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>Our synthetic memory</w:t></w:r></w:p></w:body></w:document>":ext.equals("xlsx")?"<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheets/></workbook>":"<p:presentation xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\"><p:sldIdLst/></p:presentation>";
  var output=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(output)){
   write(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/"+main+"\" ContentType=\"application/vnd.openxmlformats-officedocument."+type+".main+xml\"/></Types>");
   write(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\""+main+"\"/></Relationships>");write(zip,main,xml);if(malicious)write(zip,"word/vbaProject.bin","not permitted");
  }return output.toByteArray();
 }
 static void write(ZipOutputStream zip,String name,String value)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}
 static Path paddedVideo(long size)throws Exception{Path path=root.resolve("video-"+size+".mp4");Files.copy(root.resolve("short.mp4"),path,StandardCopyOption.REPLACE_EXISTING);try(var file=new RandomAccessFile(path.toFile(),"rw")){long old=file.length();file.seek(old);file.writeInt((int)(size-old));file.writeBytes("free");file.setLength(size);}return path;}
}
