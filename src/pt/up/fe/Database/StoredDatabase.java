package pt.up.fe.Database;

import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.StoredFile;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Vector;

/**
 *      Contains information about the files from other systems that are backed up locally.
 */

public class StoredDatabase extends Database implements Serializable {
    private Vector<StoredFile> _files;

    public StoredDatabase() {
        _files = new Vector<>();
    }

    public Vector<StoredFile> getStoredFiles() {
        return _files;
    }

    public boolean add(StoredFile f) {
        try {
            getFileWithId(f.getId());

            return false;
        } catch (FileNotFoundException e) {
            _files.add(f);

            return true;
        }
    }

    public StoredFile getFileWithId(String cId) throws FileNotFoundException {
        for (StoredFile f : _files)
            if (f.getId().equals(cId))
                return f;

        throw new FileNotFoundException();
    }

    public void removeFileWithId(String cId) throws FileNotFoundException {
        StoredFile f = getFileWithId(cId);

        for (int i = 0; i < f.getNumberOfChunks(); i++) {
            DataStorage.getInstance().removeChunk(cId, i);
        }

        _files.remove(f);
    }
}
