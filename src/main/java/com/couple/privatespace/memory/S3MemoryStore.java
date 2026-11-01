package com.couple.privatespace.memory;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import jakarta.annotation.PreDestroy;

/** Private bucket only; no presigned/public URLs are returned to the browser. */
@Component
@ConditionalOnProperty(name="app.memories.backend", havingValue="s3")
public class S3MemoryStore implements RemoteMemoryStore {
    private static final String PREFIX="couple-private-space/objects/";
    private final S3Client client;
    private final String bucket;
    private String continuation;

    @org.springframework.beans.factory.annotation.Autowired
    public S3MemoryStore(@Value("${AWS_ENDPOINT_URL_S3:}") String endpoint,
            @Value("${AWS_REGION:}") String region, @Value("${AWS_ACCESS_KEY_ID:}") String access,
            @Value("${AWS_SECRET_ACCESS_KEY:}") String secret,
            @Value("${MEMORY_S3_BUCKET:}") String bucket) {
        if(endpoint.isBlank()||region.isBlank()||access.isBlank()||secret.isBlank()||!bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]"))
            throw new IllegalStateException("Private S3 configuration is incomplete; hosted media cannot use ephemeral storage.");
        URI uri=URI.create(endpoint);
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null)
            throw new IllegalStateException("S3 endpoint must be an HTTPS origin without credentials or query.");
        this.bucket=bucket;
        client=S3Client.builder().endpointOverride(uri).region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(60)))
            .overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(120)).apiCallAttemptTimeout(Duration.ofSeconds(60)))
            .build();
        try{client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());}
        catch(RuntimeException error){client.close();throw new IllegalStateException("Private media bucket is not reachable; verify its branch, endpoint and storage credentials.");}
    }
    // Package-visible constructor for contract tests without cloud credentials.
    S3MemoryStore(S3Client client,String bucket){this.client=client;this.bucket=bucket;}
    private String key(UUID id,boolean thumb){return PREFIX+id+(thumb?".jpg":".data");}
    private IOException failure(){return new IOException("Private object storage is unavailable; retry later.");}
    @Override public void put(UUID id,boolean thumb,Path source)throws IOException {
        try{client.putObject(PutObjectRequest.builder().bucket(bucket).key(key(id,thumb))
            .contentType(thumb?"image/jpeg":"application/octet-stream").cacheControl("no-store, private").build(),RequestBody.fromFile(source));}
        catch(RuntimeException error){throw failure();}
    }
    @Override public void delete(UUID id,boolean thumb)throws IOException {
        try{client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key(id,thumb)).build());}
        catch(S3Exception error){if(error.statusCode()!=404)throw failure();}
        catch(RuntimeException error){throw failure();}
    }
    @Override public boolean exists(UUID id,boolean thumb)throws IOException {
        try{head(id,thumb);return true;}catch(S3Exception error){if(error.statusCode()==404)return false;throw failure();}
        catch(RuntimeException error){throw failure();}
    }
    private HeadObjectResponse head(UUID id,boolean thumb){return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key(id,thumb)).build());}
    @Override public Resource resource(UUID id,boolean thumb)throws IOException {
        final long length;
        try{length=head(id,thumb).contentLength();}catch(RuntimeException error){throw failure();}
        if(length<1||length>100000000L)throw failure();
        return new AbstractResource(){
            @Override public String getDescription(){return "Private memory object";}
            @Override public long contentLength(){return length;}
            @Override public InputStream getInputStream()throws IOException {
                // Spring's ResourceRegion converter skips before reading. Defer
                // opening the S3 stream so video seeks become upstream ranges.
                return new InputStream(){
                    long offset;boolean closed;
                    software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> stream;
                    private void open()throws IOException{
                        if(closed)throw new IOException("Stream closed");
                        if(stream==null)try{var request=GetObjectRequest.builder().bucket(bucket).key(key(id,thumb));
                            if(offset>0)request.range("bytes="+offset+"-");stream=client.getObject(request.build());}
                        catch(RuntimeException error){throw failure();}
                    }
                    @Override public long skip(long count)throws IOException{
                        if(closed)throw new IOException("Stream closed");
                        if(count<=0)return 0;
                        if(stream!=null){long skipped=stream.skip(count);offset+=skipped;return skipped;}
                        long skipped=Math.min(count,length-offset);offset+=skipped;return skipped;
                    }
                    @Override public int read()throws IOException{byte[] one=new byte[1];return read(one,0,1)<0?-1:one[0]&255;}
                    @Override public int read(byte[] bytes,int start,int count)throws IOException{
                        Objects.checkFromIndexSize(start,count,bytes.length);if(count==0)return 0;
                        if(offset>=length)return -1;open();int result=stream.read(bytes,start,count);if(result>0)offset+=result;return result;
                    }
                    @Override public void close(){closed=true;if(stream!=null)stream.abort();}
                };
            }
        };
    }
    @Override public synchronized List<ObjectKey> scan()throws IOException {
        try{
            var page=client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(PREFIX).maxKeys(100).continuationToken(continuation).build());
            continuation=Boolean.TRUE.equals(page.isTruncated())?page.nextContinuationToken():null;
            var result=new ArrayList<ObjectKey>();
            for(var object:page.contents()){
                String name=object.key().substring(PREFIX.length());
                if(!name.matches("[a-f0-9-]{36}\\.(data|jpg)"))continue;
                try{result.add(new ObjectKey(UUID.fromString(name.substring(0,36)),name.endsWith(".jpg"),object.lastModified()));}
                catch(IllegalArgumentException ignored){/* another application's key is never deleted */}
            }
            return result;
        }catch(RuntimeException error){throw failure();}
    }
    @PreDestroy public void close(){client.close();}
}
