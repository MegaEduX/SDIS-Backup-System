package pt.up.fe.Filesystem;

import java.io.IOException;
import java.util.Vector;

/**
 *      Handles all IO operations.
 */

public class DataStorage {

    private String _dataStorePath;

    DataStorage(String path) throws IOException {

    }

    Vector<String> chunkList() {
        return null;
    }

    boolean chunkExists(String fileId, int chunkId) {
        return false;
    }

    boolean storeChunk(String fileId, int chunkId) {
        return false;
    }

    void retrieveChunk(String fileId, int chunkId) {

    }

}
