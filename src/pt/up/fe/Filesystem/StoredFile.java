package pt.up.fe.Filesystem;

import java.util.Vector;

/**
 *      A file belonging to other systems that is backed up locally.
 */

public class StoredFile extends File {
    Vector<Integer> _chunksStored;

    public StoredFile(String identifier, Vector<Integer> fileChunksStored) {
        _id = identifier;
        _chunksStored = fileChunksStored;
        _numberOfChunks = _chunksStored.size();
    }
}
