package pt.up.fe.Filesystem;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;

public class File implements Serializable {
    transient protected static int kChunkLengthInBytes = 64000;
    transient private Vector<InetAddress> _peersWithFile = new Vector<>();

    transient boolean _refrainFromStartingBackup;

    private int desiredReplicationCount;

    private HashMap<Integer, Integer> _replicationStatus;

    protected String _id;
    protected int _numberOfChunks;

    public File() {
        _replicationStatus = new HashMap<>();
        _refrainFromStartingBackup = false;
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
            return 0;
        }
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
            //  If it doesn't exist, it's zero.
        }

        _replicationStatus.remove(chunk);
        _replicationStatus.put(chunk, count + 1);
    }

    public boolean decreaseReplicationCountForChunk(int chunk) {
        int count = 0;

        try {
            count = _replicationStatus.get(chunk);
        } catch (Exception e) {
            //  If it doesn't exist, it's zero.
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

    public void resetPeerList() {
        _peersWithFile = new Vector<>();
    }

    public void addPeer(InetAddress peer) {
        try {
            if (_peersWithFile.contains(peer))
                return;
        } catch (NullPointerException e) {
            resetPeerList();
        }

        _peersWithFile.add(peer);
    }

    public int getPeerCount() {
        return _peersWithFile.size();
    }

    public void refrainFromStartingPropagation() {
        _refrainFromStartingBackup = true;

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //  Eh.
                }

                _refrainFromStartingBackup = false;
            }
        }.start();
    }

    public boolean getRefrainFromStartingPropagation() {
        return _refrainFromStartingBackup;
    }

    public byte[] getChunk(int chunkId) throws IOException {
        throw new IOException();
    }
}
