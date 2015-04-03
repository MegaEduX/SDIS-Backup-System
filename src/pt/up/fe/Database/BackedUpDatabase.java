package pt.up.fe.Database;

import pt.up.fe.Filesystem.BackedUpFile;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Vector;

/**
 *      Contains information about the local files that are backed up on other systems.
 */

public class BackedUpDatabase implements Serializable {
    Vector<BackedUpFile> _files;

    public BackedUpDatabase() {
        _files = new Vector<>();
    }

    public Vector<BackedUpFile> getBackedUpFiles() {
        return _files;
    }

    public BackedUpFile getFileWithId(String cId) throws FileNotFoundException {
        for (BackedUpFile f : _files)
            if (f.getId().equals(cId))
                return f;

        throw new FileNotFoundException();
    }

    public boolean add(BackedUpFile f) {
        for (BackedUpFile bf : _files)
            if (bf.getId().equals(f.getId()))
                return false;

        return _files.add(f);
    }
}
