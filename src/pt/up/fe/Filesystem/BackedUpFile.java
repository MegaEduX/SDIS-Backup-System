package pt.up.fe.Filesystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 *      A file that is also backed up on other systems.
 */

public class BackedUpFile extends File {
    static int kMinimumChunkReplicationCount = 5;

    String _path;

    HashMap<Integer, Integer> _replicationStatus;

    public BackedUpFile(String pathToFile) throws IOException {
        //  Get information about the file (just enough to generate an ID), generate an ID, then return the object.

        _path = pathToFile;
        _replicationStatus = new HashMap<>();
        _id = new String(generateFileId(), "UTF-8");
        _numberOfChunks = (int)(File.getFileSizeInBytes(_path) / kChunkLengthInBytes);
    }

    public byte[] generateFileId() throws IOException {
        Path file = Paths.get(_path);

        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

        String lmt = attr.lastModifiedTime().toString();

        String idBeforeSHA = _path + lmt;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return digest.digest(idBeforeSHA.getBytes("UTF-8"));
        } catch (Exception e) {

        }

        return null;
    }

    //  Borrowed some code from http://stackoverflow.com/questions/9588348/java-read-file-by-chunks

    public byte[] getChunk(int chunkId) throws IOException {
        byte[] buffer = new byte[kChunkLengthInBytes];
        FileInputStream in = new FileInputStream(_path);

        while (in.read(buffer) != -1) {
            if (chunkId > 0) {
                chunkId--;

                continue;
            }

            return buffer;
        }

        return null;
    }

    public int getReplicationCountForChunk(int chunk) {
        return _replicationStatus.get(chunk);
    }

    public void increaseReplicationCountForChunk(int chunk) {
        int count = _replicationStatus.get(chunk);

        _replicationStatus.remove(chunk);
        _replicationStatus.put(chunk, count + 1);
    }

    public boolean decreaseReplicationCountForChunk(int chunk) {
        int count = _replicationStatus.get(chunk);

        if (count < 1)
            return false;

        _replicationStatus.remove(chunk);
        _replicationStatus.put(chunk, count - 1);

        return true;
    }

    public Vector<Integer> chunksNeedingReplication() {
        Vector<Integer> chunks = new Vector<Integer>();

        for (Map.Entry<Integer, Integer> e : _replicationStatus.entrySet())
            if (e.getValue() < kMinimumChunkReplicationCount)
                chunks.add(e.getKey());

        return chunks;
    }
}
