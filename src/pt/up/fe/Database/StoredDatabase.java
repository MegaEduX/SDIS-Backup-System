package pt.up.fe.Database;

import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.StoredFile;

import java.io.Serializable;
import java.util.Vector;

/**
 *      Contains information about the files from other systems that are backed up locally.
 */

public class StoredDatabase extends Database implements Serializable {
    transient DataStorage _ds;

    Vector<StoredFile> _files;

    public StoredDatabase(DataStorage ds) {
        _ds = ds;

        load();
    }

    public Vector<StoredFile> getStoredFiles() {
        return _files;
    }

    public StoredFile getFileWithChunkId(String cId) {
        for (StoredFile f : _files)
            if (f.getId().equals(cId))
                return f;

        return null;
    }

    public void load() {
        _files = new Vector<StoredFile>();

        Vector<String> cl = _ds.chunkList();

        for (String chunk : cl) {
            Vector<Integer> chunks = new Vector<Integer>();

            String[] chunkDetails = chunk.split("-");

            String fileId = chunkDetails[0];

            chunks.add(Integer.parseInt(chunkDetails[1]));

            cl.remove(chunk);

            for (String innerChunk : cl) {
                String[] splittedInnerChunk = innerChunk.split("-");

                if (splittedInnerChunk[0].equals(fileId))
                    chunks.add(Integer.parseInt(splittedInnerChunk[1]));

                cl.remove(innerChunk);
            }

            _files.add(new StoredFile(fileId, chunks));
        }
    }
}
