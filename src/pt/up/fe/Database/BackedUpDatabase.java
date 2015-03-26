package pt.up.fe.Database;

import pt.up.fe.Filesystem.BackedUpFile;

import java.io.Serializable;
import java.util.Vector;

/**
 *      Contains information about the local files that are backed up on other systems.
 */

public class BackedUpDatabase implements Serializable {
    Vector<BackedUpFile> _files;

    public BackedUpDatabase() {

    }


}
