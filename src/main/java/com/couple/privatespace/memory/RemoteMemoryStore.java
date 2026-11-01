package com.couple.privatespace.memory;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.springframework.core.io.Resource;

/** HOST-01: authorization remains in MemoryService, before resource acquisition. */
public interface RemoteMemoryStore {
    record ObjectKey(UUID id, boolean thumbnail, Instant modified) { }
    void put(UUID id, boolean thumbnail, Path source) throws IOException;
    void delete(UUID id, boolean thumbnail) throws IOException;
    boolean exists(UUID id, boolean thumbnail) throws IOException;
    Resource resource(UUID id, boolean thumbnail) throws IOException;
    List<ObjectKey> scan() throws IOException;
}
