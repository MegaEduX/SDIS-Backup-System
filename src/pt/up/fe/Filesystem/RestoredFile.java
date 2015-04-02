package pt.up.fe.Filesystem;

import pt.up.fe.Messaging.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

/**
 *      A file that we just restored from other peers.
 */

public class RestoredFile extends File {
    Set<byte[]> chunkData;

    public RestoredFile(Set<byte[]> chunks) {
        chunkData = chunks;
    }

    public void saveToDisk(String path) throws IOException {
        byte[] data = new byte[]{};

        for (byte[] c : chunkData)
            data = Message.concatByteArrays(data, c);

        Files.write(Paths.get(path), data);
    }
}
