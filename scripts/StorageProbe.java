import com.couple.privatespace.memory.S3MemoryStore;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** HOST-01: real provider proof, only a new random probe key is written/deleted. */
class StorageProbe {
    public static void main(String[] args)throws Exception {
        UUID id=UUID.randomUUID();Path file=Files.createTempFile("couple-storage-probe-",".tmp");
        String text="Couple Private Space — မြန်မာ 💕";
        Files.writeString(file,text,StandardCharsets.UTF_8);
        S3MemoryStore storage=null;
        try{
            storage=new S3MemoryStore(System.getenv("AWS_ENDPOINT_URL_S3"),System.getenv("AWS_REGION"),System.getenv("AWS_ACCESS_KEY_ID"),System.getenv("AWS_SECRET_ACCESS_KEY"),System.getenv().getOrDefault("MEMORY_S3_BUCKET","couple-memories"));
            storage.put(id,false,file);System.out.println("PASS private bucket upload (owned random probe)");
            try(var stream=storage.resource(id,false).getInputStream()){
                if(!text.equals(new String(stream.readAllBytes(),StandardCharsets.UTF_8)))throw new IllegalStateException("Provider readback mismatch");
            }
            System.out.println("PASS private bucket Unicode download/readback");
            var endpoint=System.getenv("AWS_ENDPOINT_URL_S3").replaceFirst("/$","");
            var bucket=System.getenv().getOrDefault("MEMORY_S3_BUCKET","couple-memories");
            var anonymous=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(endpoint+"/"+bucket+"/couple-private-space/objects/"+id+".data")).timeout(java.time.Duration.ofSeconds(15)).GET().build();
            int status=anonymous.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            if(!Set.of(401,403,404).contains(status))throw new IllegalStateException("Anonymous storage denial not confirmed; HTTP "+status);
            System.out.println("PASS anonymous object access denied HTTP "+status);
            storage.delete(id,false);if(storage.exists(id,false))throw new IllegalStateException("Provider deletion not confirmed");
            System.out.println("PASS probe deletion confirmed");
        }finally{
            Files.deleteIfExists(file);
            if(storage!=null){try{storage.delete(id,false);}catch(Exception error){System.err.println("Probe cleanup pending for owned key: "+id);}storage.close();}
        }
    }
}
