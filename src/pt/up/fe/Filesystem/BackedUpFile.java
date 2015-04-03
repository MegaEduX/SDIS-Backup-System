package pt.up.fe.Filesystem;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import pt.up.fe.Utilities.Security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Vector;

/**
 *      A file that is also backed up on other systems.
 */

public class BackedUpFile extends File implements Serializable {
    private String _path;
    private String _lastModified;

    transient private Vector<InetAddress> _peersWithFile = new Vector<>();

    public BackedUpFile(String pathToFile) throws IOException, NoSuchAlgorithmException {
        super();

        //  Get information about the file (just enough to generate an ID), generate an ID, then return the object.

        _path = pathToFile;

        _lastModified = Long.toString(new java.io.File(_path).lastModified());

        _id = generateFileId();

        _numberOfChunks = (int) Math.ceil((File.getFileSizeInBytes(_path) / (double) kChunkLengthInBytes));

        _peersWithFile = new Vector<>();
    }

    public String getPath() {
        return _path;
    }

    public String getLastModified() {
        return _lastModified;
    }

    @NotNull private String generateFileId() throws NoSuchAlgorithmException {
        String idBeforeSHA = _path + _lastModified;

        return Security.hashSHA256(idBeforeSHA);
    }

    //  Borrowed some code from http://stackoverflow.com/questions/9588348/java-read-file-by-chunks

    public byte[] getChunk(int chunkId) throws IOException {
        byte[] buffer = new byte[kChunkLengthInBytes];
        FileInputStream in = new FileInputStream(_path);

        int bytesRead = in.read(buffer);

        while (bytesRead != -1) {
            if (chunkId > 0) {
                chunkId--;

                bytesRead = in.read(buffer);

                continue;
            }

            //  The chunk may need to be truncated here.

            return Arrays.copyOf(buffer, bytesRead);
        }

        return null;
    }

    public void resetPeerList() {
        _peersWithFile = new Vector<>();
    }

    public void addPeer(InetAddress peer) {
        if (_peersWithFile.contains(peer))
            return;

        _peersWithFile.add(peer);
    }

    public int getPeerCount() {
        return _peersWithFile.size();
    }
}
