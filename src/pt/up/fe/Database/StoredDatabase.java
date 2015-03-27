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
        load();
    }

    public Vector<StoredFile> getStoredFiles() {
        return _files;
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

    public void load() {
        _files = new Vector<StoredFile>();

        Vector<String> cl = DataStorage.getInstance().chunkList();

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
