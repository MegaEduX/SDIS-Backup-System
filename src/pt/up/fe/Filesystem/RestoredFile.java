package pt.up.fe.Filesystem;

import pt.up.fe.Messaging.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

/**
 *      A file that we just restored from other peers.
 */

public class RestoredFile extends File {
    Vector<String> chunkData;

    public RestoredFile(Vector<String> chunks) {
        chunkData = chunks;
    }

    public void saveToDisk(String path) throws IOException {
        byte[] data = new byte[]{};

        for (String c : chunkData)
            data = Message.concatByteArrays(data, c.getBytes("UTF-8"));

        Files.write(Paths.get(path), data);
    }
}
