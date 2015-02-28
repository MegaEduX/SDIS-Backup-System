package pt.up.fe.Database;

import pt.up.fe.Filesystem.File;

import java.util.Vector;

public class Database {
    Vector<File> files;

    /*
     *      This function should be overridden by a subclass.
     */

    boolean load() {
        return false;
    }
}
