package pt.up.fe.Database;

import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.StoredFile;

import javax.xml.crypto.Data;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Vector;

/**
 *      Contains information about the files from other systems that are backed up locally.
 */

public class StoredDatabase extends Database implements Serializable {
    Vector<StoredFile> _files;

    public StoredDatabase() {
        _files = new Vector<>();
    }

    public Vector<StoredFile> getStoredFiles() {
        return _files;
    }

    public boolean add(StoredFile f) {
        try {
            getFileWithChunkId(f.getId());

            return false;
        } catch (FileNotFoundException e) {
            if (f == null)
                return false;

            _files.add(f);

            return true;
        }
    }

    public StoredFile getFileWithChunkId(String cId) throws FileNotFoundException {
        for (StoredFile f : _files)
            if (f.getId().equals(cId))
                return f;

        throw new FileNotFoundException();
    }

    public void removeFileWithChunkId(String cId) throws FileNotFoundException {
        StoredFile f = getFileWithChunkId(cId);

        for (int i = 0; i < f.getNumberOfChunks(); i++) {
            DataStorage.getInstance().removeChunk(cId, i);
        }

        _files.remove(f);
    }
}
