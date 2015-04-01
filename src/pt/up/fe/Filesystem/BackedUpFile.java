package pt.up.fe.Filesystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

/**
 *      A file that is also backed up on other systems.
 */

public class BackedUpFile extends File {
    private String _path;
    private String _lastModified;

    public BackedUpFile(String pathToFile) throws IOException {
        super();

        //  Get information about the file (just enough to generate an ID), generate an ID, then return the object.

        _path = pathToFile;

        _id = new String(generateFileId(), "UTF-8");
        _numberOfChunks = (int) Math.ceil((File.getFileSizeInBytes(_path) / (double) kChunkLengthInBytes));

        System.out.println(File.getFileSizeInBytes(_path) + " " + kChunkLengthInBytes + " " + _numberOfChunks);

        Path file = Paths.get(_path);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

        _lastModified = attr.lastModifiedTime().toString();
    }

    public String getPath() {
        return _path;
    }

    public String getLastModified() {
        return _lastModified;
    }

    private byte[] generateFileId() throws IOException {
        String idBeforeSHA = _path + _lastModified;

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


}
