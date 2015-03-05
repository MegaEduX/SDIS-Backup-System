package pt.up.fe.Filesystem;

import java.util.Vector;

/**
 *      A file belonging to other systems that is backed up locally.
 */

public class StoredFile extends File {
    String _owner;
    Vector<String> _chunksStored;

    StoredFile(String fileOwner, Vector<String> fileChunksStored) {
        _owner = fileOwner;
        _chunksStored = fileChunksStored;
        _numberOfChunks = _chunksStored.size();
    }
}
