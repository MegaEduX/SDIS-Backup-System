package pt.up.fe.Filesystem;

import pt.up.fe.Database.BackedUpDatabase;
import pt.up.fe.Database.StoredDatabase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

/**
 *      Handles all IO operations.
 */

public class DataStorage {
    private static final DataStorage Instance = new DataStorage();

    private StoredDatabase storedDatabase = null;
    private BackedUpDatabase backedUpDatabase = null;

    private static final String kStoredDatabaseSerializedFileName = "storedDatabase.sdis";
    private static final String kBackedUpDatabaseSerializedFileName = "backedUpDatabase.sdis";

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
        if (_dataStorePath == null)
            throw new IllegalStateException("Data Store wasn't instantiated!");
    }

    /*  public String getDataStorePath() {
        return _dataStorePath;
    }   */

    public StoredDatabase getStoredDatabase() {
        return storedDatabase;
    }

    public BackedUpDatabase getBackedUpDatabase() {
        return backedUpDatabase;
    }

    public void setDataStorePath(String path) {
        _dataStorePath = path;

        try {
            Files.createDirectory(Paths.get(path));
        } catch (IOException exc) {

        }

        try {
            FileInputStream fis = new FileInputStream(appendPaths(_dataStorePath, kStoredDatabaseSerializedFileName));
            ObjectInputStream ois = new ObjectInputStream(fis);

            storedDatabase = (StoredDatabase) ois.readObject();

            ois.close();
            fis.close();

            System.out.println("Loaded StoredDatabase from disk...");
        } catch (Exception e) {
            storedDatabase = new StoredDatabase();

            System.out.println("Created a new StoredDatabase instance...");
        }

        try {
            FileInputStream fis = new FileInputStream(appendPaths(_dataStorePath, kBackedUpDatabaseSerializedFileName));
            ObjectInputStream ois = new ObjectInputStream(fis);

            backedUpDatabase = (BackedUpDatabase) ois.readObject();

            ois.close();
            fis.close();

            System.out.println("Loaded BackedUpDatabase from disk...");
        } catch (Exception e) {
            backedUpDatabase = new BackedUpDatabase();

            System.out.println("Created a new BackedUpDatabase instance...");
        }

        System.out.println("");
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

        if (fileList == null)
            return new Vector<>();

        Vector<String> chunks = new Vector<>();

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

    public boolean removeChunk(String fileId, int chunkId) {
        _consistencyCheck();

        try {
            Files.delete(Paths.get(appendPaths(_dataStorePath, chunkFileName(fileId, chunkId))));

            return true;
        } catch (IOException e) {

        }

        return false;
    }

    public byte[] retrieveChunk(String fileId, int chunkId) throws IOException {
        _consistencyCheck();

        String pathToChunk = appendPaths(_dataStorePath, chunkFileName(fileId, chunkId));

        Path path = Paths.get(pathToChunk);

        return Files.readAllBytes(path);
    }

    public void synchronize() throws IOException {
        //  Synchronize the data on RAM to the disk.

        {
            FileOutputStream fos = new FileOutputStream(appendPaths(_dataStorePath, kStoredDatabaseSerializedFileName));
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(storedDatabase);

            oos.close();
            fos.close();
        }

        {
            FileOutputStream fos = new FileOutputStream(appendPaths(_dataStorePath, kBackedUpDatabaseSerializedFileName));
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(backedUpDatabase);

            oos.close();
            fos.close();
        }
    }

}
