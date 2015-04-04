package pt.up.fe.Filesystem;

import java.io.IOException;
import java.io.Serializable;
import java.util.Vector;

/**
 *      A file belonging to other systems that is backed up locally.
 */

public class StoredFile extends File implements Serializable {
    private Vector<Integer> _chunksStored;

    public StoredFile(String identifier) {
        super();

        _id = identifier;
        _chunksStored = new Vector<>();
        _numberOfChunks = 0;
    }

    public Vector<Integer> getChunksStored() {
        return _chunksStored;
    }

    public void setChunkStoredStatus(Integer chunk, boolean status) {
        if (status) {
            for (Integer i : _chunksStored)
                if (chunk.equals(i))
                    return;

            _chunksStored.add(chunk);
        } else
            for (Integer i : _chunksStored)
                if (chunk.equals(i)) {
                    _chunksStored.remove(i);

                    return;
                }
    }

    public void removeChunk(Integer chunk) {
        setChunkStoredStatus(chunk, false);
        decreaseReplicationCountForChunk(chunk);

        DataStorage.getInstance().removeChunk(_id, chunk);
    }

    public void removeAllChunks() {
        while (_chunksStored.size() != 0)
            removeChunk(_chunksStored.get(_chunksStored.size() - 1));
    }

    @Override
    public byte[] getChunk(int chunkId) throws IOException {
        return DataStorage.getInstance().retrieveChunk(_id, chunkId);
    }
}
