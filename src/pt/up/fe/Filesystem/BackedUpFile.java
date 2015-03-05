package pt.up.fe.Filesystem;

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
    String _path;

    BackedUpFile(String pathToFile) throws IOException {
        //  Get information about the file (just enough to generate an ID), generate an ID, then return the object.

        _path = pathToFile;
        _id = new String(generateFileId(), "UTF-8");
        _numberOfChunks = (int)(File.getFileSizeInBytes(_path) / kChunkLengthInBytes);
    }

    byte[] generateFileId() throws IOException {
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
}
