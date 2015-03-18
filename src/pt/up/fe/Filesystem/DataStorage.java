package pt.up.fe.Filesystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

/**
 *      Handles all IO operations.
 */

public class DataStorage {
    private static final DataStorage Instance = new DataStorage();

    private DataStorage() {
        if (Instance != null) {
            throw new IllegalStateException("Already instantiated!");
        }
    }

    public static DataStorage getInstance() {
        return Instance;
    }

    private String _dataStorePath;

    private void _consistencyCheck() {
        if (_dataStorePath != null)
            throw new IllegalStateException("Data Store wasn't instantiated!");
    }

    public String getDataStorePath() {
        return _dataStorePath;
    }

    public void setDatStorePath(String path) {
        _dataStorePath = path;
    }

    //  Borrowed from http://stackoverflow.com/questions/711993/does-java-have-a-path-joining-method

    public static String appendPaths(String path1, String path2) {
        java.io.File file1 = new java.io.File(path1);
        java.io.File file2 = new java.io.File(file1, path2);

        return file2.getPath();
    }

    public static String chunkFileName(String fileId, int chunkId) {
        return fileId + "-" + chunkId;
    }

    public Vector<String> chunkList() {
        _consistencyCheck();

        java.io.File currentDir = new java.io.File(_dataStorePath);

        java.io.File[] fileList = currentDir.listFiles();

        Vector<String> chunks = new Vector<String>();

        for (java.io.File file : fileList)
            chunks.add(file.getName());

        return chunks;
    }

    public boolean chunkExists(String fileId, int chunkId) {
        _consistencyCheck();

        java.io.File f = new java.io.File(chunkFileName(fileId, chunkId));

        return f.exists();
    }

    public boolean storeChunk(String fileId, int chunkId, byte[] data) {
        _consistencyCheck();

        try {
            FileOutputStream fos = new FileOutputStream(chunkFileName(fileId, chunkId));

            fos.write(data);
            fos.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public byte[] retrieveChunk(String fileId, int chunkId) throws IOException {
        _consistencyCheck();

        String pathToChunk = appendPaths(_dataStorePath, chunkFileName(fileId, chunkId));

        Path path = Paths.get(pathToChunk);

        return Files.readAllBytes(path);
    }

}
