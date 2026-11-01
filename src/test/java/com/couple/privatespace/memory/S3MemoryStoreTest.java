package com.couple.privatespace.memory;

import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.jupiter.api.Assertions.*;

/** SDK contract double, not proof of a live Neon bucket or Docker runtime. */
class S3MemoryStoreTest {
    @TempDir Path root;
    private final Map<String,byte[]> objects=new HashMap<>();
    private boolean failDelete;
    private String requestedRange;
    private S3MemoryStore store(){
        var client=(S3Client)Proxy.newProxyInstance(S3Client.class.getClassLoader(),new Class[]{S3Client.class},(p,method,args)->{
            switch(method.getName()){
                case "putObject": {var req=(PutObjectRequest)args[0];assertEquals("private-test",req.bucket());assertNull(req.acl());assertEquals("no-store, private",req.cacheControl());try(var in=((RequestBody)args[1]).contentStreamProvider().newStream()){objects.put(req.key(),in.readAllBytes());}return PutObjectResponse.builder().build();}
                case "headObject": {var req=(HeadObjectRequest)args[0];if(!objects.containsKey(req.key()))throw S3Exception.builder().statusCode(404).build();return HeadObjectResponse.builder().contentLength((long)objects.get(req.key()).length).build();}
                case "getObject": {var req=(GetObjectRequest)args[0];requestedRange=req.range();byte[] data=objects.get(req.key());int start=req.range()==null?0:Integer.parseInt(req.range().substring(6,req.range().length()-1));return new ResponseInputStream<>(GetObjectResponse.builder().build(),AbortableInputStream.create(new ByteArrayInputStream(data,start,data.length-start)));}
                case "deleteObject": {if(failDelete)throw S3Exception.builder().statusCode(503).message("server detail must not escape").build();objects.remove(((DeleteObjectRequest)args[0]).key());return DeleteObjectResponse.builder().build();}
                case "listObjectsV2": {var req=(ListObjectsV2Request)args[0];assertEquals(100,req.maxKeys());return ListObjectsV2Response.builder().isTruncated(false).contents(objects.keySet().stream().map(key->S3Object.builder().key(key).lastModified(Instant.now().minusSeconds(90000)).build()).toList()).build();}
                case "close": return null;
                default: throw new UnsupportedOperationException(method.getName());
            }
        });
        return new S3MemoryStore(client,"private-test");
    }
    @Test void privateStreamingObjectsSurviveFreshAdapterAndDeleteIsIdempotent()throws Exception{
        var store=store();var id=UUID.randomUUID();Path source=root.resolve("source");Files.writeString(source,"မြန်မာ 💕");
        store.put(id,false,source);assertTrue(store.exists(id,false));
        try(var stream=store().resource(id,false).getInputStream()){assertEquals("မြန်မာ 💕",new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        store.delete(id,false);store.delete(id,false);assertFalse(store.exists(id,false));
    }
    @Test void promotionAndOrphanMaintenanceUseRemoteBytesWithoutLocalObjects()throws Exception{
        var remote=store();var factory=new DefaultListableBeanFactory();factory.registerSingleton("remote",remote);
        var files=new MemoryFiles(root.toString(),"unused","unused",factory.getBeanProvider(RemoteMemoryStore.class));
        var id=UUID.randomUUID();Files.writeString(files.stage.resolve(id+".data"),"validated fixture");
        files.promote(new MemoryFiles.Validated(id,"fixture.pdf","DOCUMENT","application/pdf",17,"unused",null,null,null));
        assertTrue(files.available(id));assertFalse(Files.exists(files.path(id,false)));
        files.reconcileRemote(key->true);assertTrue(files.available(id));
        files.reconcileRemote(key->false);assertFalse(files.available(id));
    }
    @Test void deletionFailureRemainsRetryableAndDoesNotExposeProviderError()throws Exception{
        var remote=store();failDelete=true;
        var error=assertThrows(IOException.class,()->remote.delete(UUID.randomUUID(),false));
        assertFalse(error.getMessage().contains("server detail"));assertNull(error.getCause());
    }
    @Test void remoteResourceSupportsExistingSpringVideoRangeResponse()throws Exception{
        var remote=store();var id=UUID.randomUUID();Path source=root.resolve("range-fixture");Files.writeString(source,"0123456789");remote.put(id,false,source);
        var resource=remote.resource(id,false);
        var output=new org.springframework.mock.http.MockHttpOutputMessage();
        var region=new org.springframework.core.io.support.ResourceRegion(resource,2,4);
        new org.springframework.http.converter.ResourceRegionHttpMessageConverter().write(region,org.springframework.http.MediaType.APPLICATION_OCTET_STREAM,output);
        assertEquals("2345",output.getBodyAsString());
        assertEquals("bytes 2-5/10",output.getHeaders().getFirst("Content-Range"));
        assertEquals("bytes=2-",requestedRange);
    }
}
