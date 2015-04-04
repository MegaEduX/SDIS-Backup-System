package pt.up.fe.Filesystem;

import pt.up.fe.Messaging.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 *      A file that we just restored from other peers.
 */

public class RestoredFile extends File {
    HashMap<Integer, byte[]> chunkData;

    public RestoredFile(HashMap<Integer, byte[]> chunks) {
        chunkData = chunks;
    }

    public void saveToDisk(String path) throws IOException {
        byte[] data = new byte[]{};

        for (int i = 0; chunkData.containsKey(i); i++)
            data = Message.concatByteArrays(data, chunkData.get(i));

        Files.write(Paths.get(path), data);
    }
}
