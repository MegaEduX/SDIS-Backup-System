package pt.up.fe.Filesystem;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class File implements Serializable {
    transient static int kChunkLengthInBytes = 64000;

    int desiredReplicationCount;

    HashMap<Integer, Integer> _replicationStatus;

    String _id;
    int _numberOfChunks;

    public File() {
        _replicationStatus = new HashMap<>();
    }

    //  Borrowed from http://www.java2s.com/Code/Java/File-Input-Output/GetFileSizeInMB.htm

    public String getId() {
        return _id;
    }

    public int getNumberOfChunks() {
        return _numberOfChunks;
    }

    public static long getFileSizeInBytes(String fileName) {
        long ret = 0;

        java.io.File f = new java.io.File(fileName);

        if (f.isFile())
            return f.length();
        else if (f.isDirectory()) {
            java.io.File[] contents = f.listFiles();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isFile()) {
                    ret += contents[i].length();
                } else if (contents[i].isDirectory())
                    ret += getFileSizeInBytes(contents[i].getPath());
            }
        }

        return ret;
    }

    public int getReplicationCountForChunk(int chunk) {
        try {
            return _replicationStatus.get(chunk);
        } catch (Exception e) {

        }

        return 0;
    }

    public void setReplicationCountForChunk(int chunk, int count) {
        _replicationStatus.remove(chunk);
        _replicationStatus.put(chunk, count);
    }

    public void increaseReplicationCountForChunk(int chunk) {
        int count = 0;

        try {
            count = _replicationStatus.get(chunk);
        } catch (Exception e) {

        }

        _replicationStatus.remove(chunk);
        _replicationStatus.put(chunk, count + 1);
    }

    public boolean decreaseReplicationCountForChunk(int chunk) {
        int count = 0;

        try {
            count = _replicationStatus.get(chunk);
        } catch (Exception e) {

        }

        if (count < 1)
            return false;

        _replicationStatus.remove(chunk);
        _replicationStatus.put(chunk, count - 1);

        return true;
    }

    public boolean chunkNeedsReplication(int chunk) {
        return _replicationStatus.get(chunk) < desiredReplicationCount;
    }

    public int getDesiredReplicationCount() {
        return desiredReplicationCount;
    }

    public void setDesiredReplicationCount(int c) {
        desiredReplicationCount = c;
    }
}
