package pt.up.fe.Filesystem;

import java.util.Vector;

/**
 *      A file belonging to other systems that is backed up locally.
 */

public class StoredFile {
    String owner;
    Vector<String> chunksStored;
}
