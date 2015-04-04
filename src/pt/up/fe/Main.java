package pt.up.fe;

import pt.up.fe.Filesystem.BackedUpFile;
import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.RestoredFile;
import pt.up.fe.Filesystem.StoredFile;
import pt.up.fe.Messaging.*;
import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.MessageSender;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Threading.MC;
import pt.up.fe.Threading.MDB;
import pt.up.fe.Threading.MDR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Observable;
import java.util.Scanner;

public class Main {

    private static class AtomicBoolean {
        boolean bool;

        public AtomicBoolean(boolean val) {
            bool = val;
        }

        public synchronized void set(boolean val) {
            bool = val;
        }

        public synchronized boolean get() {
            return bool;
        }
    }

    private static class RestoreFailedException extends Exception {
        public RestoreFailedException() {
            super();
        }
    }

    public static class OperationCompleteException extends Exception {
        public OperationCompleteException() {
            super();
        }
    }

    public static void main(String[] args) {
        MC controlChannelThread = null;
        MDB dataBackupThread = null;
        MDR dataRestoreThread = null;

        try {

            if (args.length != 6 && args.length != 7) {
                System.out.println("Usage: java -jar Backupify.jar <MC Multicast IP Address> <MC Port> <MDB Multicast IP Address> <MDB Port> <MDR Multicast IP Address> <MDR Port> (<Data Store Path>)");

                return;
            }

            /*

                So, if I understood this correctly.
                Chunk Backup -> PUTCHUNK sent on the MDB, STORED sent on the MC.
                Chunk Restore -> GETCHUNK sent on the MC, CHUNK sent on the MDR.
                Chunk Delete -> DELETE sent on the MC.
                Chunk Removed -> REMOVED sent on the MC.

             */

            System.out.println(Globals.AppName + " is running.");

            Scanner reader = new Scanner(System.in);

            if (args.length == 6) {
                System.out.println("");

                while (true) {
                    try {
                        System.out.print("Data Store Path: ");

                        String path = reader.next();

                        System.out.println("");

                        DataStorage.getInstance().setDataStorePath(path);
                        DataStorage.getInstance().synchronize();

                        break;
                    } catch (IOException exc) {
                        System.out.println("Unable to create system data. Please choose another directory.");
                        System.out.println("");
                    }
                }
            } else {
                try {
                    DataStorage.getInstance().setDataStorePath(args[6]);
                    DataStorage.getInstance().synchronize();

                    System.out.println("");
                } catch (IOException exc) {
                    System.out.println("Invalid Data Store path.");

                    System.exit(1);
                }

            }

            final ProtocolController pc = new ProtocolController(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));

            final MessageReceiver rec = new MessageReceiver(pc);
            final MessageSender snd = new MessageSender(pc);

            /*

                MC Thread

             */

            controlChannelThread = new MC(pc, rec);

            new Thread(controlChannelThread).start();

            /*

                MDB Thread

             */

            dataBackupThread = new MDB(pc, rec);

            new Thread(dataBackupThread).start();

            /*

                MDR Thread

             */

            dataRestoreThread = new MDR(pc, rec);

            new Thread(dataRestoreThread).start();

            //  Not started!

            /*

                User Interface

             */

            while (true) {
                System.out.println("[1] Backup File...");
                System.out.println("[2] Restore File...");
                System.out.println("[3] Remove Local (Backed Up) File...");
                System.out.println("[4] Free Up Space (Remove Stored Chunks)...");

                System.out.println("");

                System.out.println("[0] Clean Up and Exit");
                System.out.println("");
                System.out.print("Choice: ");

                int c;

                try {
                    c = Integer.parseInt(reader.next());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input!");
                    System.out.println("");

                    continue;
                }

                switch (c) {
                    case 1: {

                        System.out.println("");

                        System.out.print("Path to file to backup: ");

                        String filePath = reader.next();

                        String id;

                        try {
                            if (Files.isDirectory(Paths.get(filePath)) || !Files.exists(Paths.get(filePath)))
                                throw new IOException();

                            BackedUpFile f = new BackedUpFile(filePath);

                            DataStorage.getInstance().getBackedUpDatabase().add(f);

                            id = f.getId();
                        } catch (IOException e) {
                            System.out.println("");
                            System.out.println("Unable to initiate a backup for the desired file.");
                            System.out.println("");

                            continue;
                        }

                        BackedUpFile f = DataStorage.getInstance().getBackedUpDatabase().getFileWithId(id);     //  I am aware this may not make much sense. But on JVM's eyes... It does.

                        String fileId = f.getId();

                        System.out.print("Desired Replication Count (0 to cancel): ");

                        int replicationCount;

                        while (true) {
                            try {
                                replicationCount = reader.nextInt();

                                break;
                            } catch (NumberFormatException e) {
                                System.out.println("");
                                System.out.print("Invalid choice! Please retry: ");
                            }
                        }

                        f.setDesiredReplicationCount(replicationCount);

                        System.out.println("");

                        for (int i = 0; i < f.getNumberOfChunks(); i++) {
                            System.out.print("Backing up chunk " + (i + 1) + "/" + f.getNumberOfChunks() + "...");    //  Peasants start counting at 1...

                            f.resetPeerList();

                            ChunkBackupMessage m = new ChunkBackupMessage(Globals.AppVersion, fileId, i, replicationCount, f.getChunk(i));

                            for (int tries = 0; f.getPeerCount() < replicationCount && tries < Globals.MaxTriesPerChunk; tries++) {
                                try {
                                    snd.sendMessage(m);
                                } catch (MessageSender.UnknownMessageException e) {
                                    System.out.println("Unknown message discarded...");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                try {
                                    Thread.sleep((long) (Math.random() * 400));
                                } catch (InterruptedException e) {
                                    //  Ignoring.
                                }
                            }

                            f.setReplicationCountForChunk(i, f.getPeerCount());

                            System.out.println(" - Replication Count " + f.getPeerCount() + "/" + replicationCount + ".");
                        }

                        System.out.println("");
                        System.out.println("Operation Complete.");

                        break;
                    }

                    case 2: {

                        if (DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles().size() == 0) {
                            System.out.println("");
                            System.out.println("There are no backed up files.");
                            System.out.println("");

                            continue;
                        }

                        System.out.println("");
                        System.out.println("Backed Up File List:");
                        System.out.println("");

                        int i = 0;

                        for (BackedUpFile bf : DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles())
                            System.out.println("[" + ++i + "] " + bf.getPath() + " (" + bf.getLastModified() + ")");

                        System.out.println("");
                        System.out.println("Showing " + i + " results.");
                        System.out.println("");
                        System.out.print("Which file do you wish to restore? (0 to cancel): ");

                        int choice = -1;

                        try {
                            choice = Integer.parseInt(reader.next());
                        } catch (NumberFormatException e) {
                            //  Entering while...
                        }

                        while (choice > i || choice < 0) {
                            System.out.println("");
                            System.out.print("Invalid choice. Please retry: ");

                            try {
                                choice = Integer.parseInt(reader.next());
                            } catch (NumberFormatException e) {
                                //  Let's just continue.
                            }
                        }

                        if (choice == 0) {
                            System.out.println("");

                            continue;
                        } else {
                            dataRestoreThread.setActive(true);

                            BackedUpFile bf = DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles().get(choice - 1);

                            System.out.println("");
                            System.out.println("Restoring " + bf.getPath() + "...");

                            try {
                                HashMap<Integer, byte[]> restoredChunks = new HashMap<>();

                                AtomicBoolean gotChunk = new AtomicBoolean(false);   //  Eww.

                                for (int j = 0; j < bf.getNumberOfChunks(); j++) {
                                    gotChunk.set(false);

                                    dataRestoreThread.cleanUp();

                                    System.out.println("Restoring chunk " + (j + 1) + "/" + bf.getNumberOfChunks() + "...");

                                    for (int k = 0; k < 5 && !gotChunk.get(); k++) {
                                        try {
                                            snd.sendMessage(new ChunkRestoreMessage(Globals.AppVersion, bf.getId(), j));

                                            dataRestoreThread.addObserver((Observable obj, Object arg) -> {
                                                ChunkRestoreAnswerMessage answer = (ChunkRestoreAnswerMessage) arg;

                                                if (answer.getId().equals(bf.getId()) && !restoredChunks.containsKey(answer.getChunkNo())) {
                                                    restoredChunks.put(answer.getChunkNo(), answer.getRawData());

                                                    gotChunk.set(true);
                                                }
                                            });
                                        } catch (MessageSender.UnknownMessageException e) {
                                            System.out.println("Unknown message discarded...");
                                        }

                                        if (!gotChunk.get())
                                            Thread.sleep(1000 * (k + 1));
                                    }

                                    if (!gotChunk.get())
                                        throw new RestoreFailedException();

                                    try {
                                        Thread.sleep((long) (Math.random() * 400));
                                    } catch (InterruptedException e) {
                                        //  Ignoring.
                                    }
                                }

                                RestoredFile f = new RestoredFile(restoredChunks);

                                System.out.println("");
                                System.out.println("Data acquired successfully.");

                                while (true) {
                                    try {
                                        System.out.println("");
                                        System.out.print("Save Path: ");

                                        f.saveToDisk(reader.next());

                                        break;
                                    } catch (IOException exc) {
                                        System.out.println("");
                                        System.out.println("An error has occoured while saving the file.");

                                        exc.printStackTrace();
                                    }
                                }
                            } catch (RestoreFailedException e) {
                                System.out.println("Unable to get chunk after 5 tries. Aborting...");
                            }

                            dataRestoreThread.setActive(false);
                        }

                        break;
                    }

                    case 3: {

                        System.out.println("");
                        System.out.println("Use this interface to remove a file from your file system and from peers who have it backed up.");
                        System.out.println("");

                        if (DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles().size() == 0) {
                            System.out.println("There are no files backed up.");
                            System.out.println("");

                            continue;
                        }

                        System.out.println("Backed Up File List:");
                        System.out.println("");

                        int i = 0;

                        for (BackedUpFile bf : DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles())
                            System.out.println("[" + ++i + "] " + bf.getPath() + " (" + bf.getId() + ")");

                        System.out.println("");
                        System.out.println("Showing " + i + " results.");
                        System.out.println("");
                        System.out.print("Which file do you wish to delete (permanently)? (0 to cancel): ");

                        int choice = Integer.parseInt(reader.next());

                        while (choice > i || choice < 0) {
                            System.out.println("");
                            System.out.print("Invalid choice. Please retry: ");

                            choice = Integer.parseInt(reader.next());
                        }

                        if (choice == 0) {
                            System.out.println("");

                            continue;
                        } else {
                            new Thread(dataRestoreThread).start();

                            BackedUpFile bf = DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles().get(i - 1);

                            System.out.println("Deleting  " + bf.getPath() + "...");

                            try {
                                Files.delete(Paths.get(bf.getPath()));
                            } catch (NoSuchFileException e) {
                                //  That's fine.
                            } catch (IOException e) {
                                System.out.println("An error has occoured. Proceeding anyway...");
                            }

                            try {
                                snd.sendMessage(new FileDeletionMessage(Globals.AppVersion, bf.getId()));

                                DataStorage.getInstance().getBackedUpDatabase().remove(bf);
                            } catch (MessageSender.UnknownMessageException e) {
                                System.out.println("Unknown Message Type - this is an internal inconsistency error.");
                            }
                        }

                        System.out.println("Operation Complete.");

                        break;
                    }

                    case 4: {

                        System.out.println("");
                        System.out.println("Use this interface to remove a number of chunks stored on your system in order to free up disk space.");
                        System.out.println("");

                        System.out.print("Maximum number of chunks to remove (0 to cancel): ");

                        int choice = Integer.parseInt(reader.next());

                        while (choice < 0) {
                            System.out.println("");
                            System.out.print("Invalid choice. Please retry: ");

                            choice = Integer.parseInt(reader.next());
                        }

                        if (choice == 0)
                            continue;
                        else {
                            int i = 0;

                            try {
                                while (i < choice) {
                                    int maxRepCount = 0;

                                    for (StoredFile sf : DataStorage.getInstance().getStoredDatabase().getStoredFiles())
                                        for (Integer chunk : sf.getChunksStored())
                                            if (maxRepCount < sf.getReplicationCountForChunk(chunk))
                                                maxRepCount = sf.getReplicationCountForChunk(chunk);

                                    for (StoredFile sf : DataStorage.getInstance().getStoredDatabase().getStoredFiles()) {
                                        for (int j = sf.getChunksStored().size() - 1; j > -1; j--) {
                                            int chunkId = sf.getChunksStored().get(j);

                                            if (sf.getReplicationCountForChunk(chunkId) == maxRepCount) {
                                                sf.removeChunk(chunkId);

                                                snd.sendMessage(new SpaceReclaimMessage(Globals.AppVersion, sf.getId(), chunkId));

                                                i++;
                                            }

                                            if (i == choice)
                                                throw new OperationCompleteException();
                                        }

                                        if (i == choice)
                                            throw new OperationCompleteException();
                                    }

                                    if (maxRepCount == 0)
                                        throw new OperationCompleteException();
                                }

                            } catch (OperationCompleteException e) {
                                System.out.println("Operation Complete. Removed " + i + " chunks.");
                            }
                        }

                        break;
                    }

                    case 0: {

                        System.out.println("");

                        System.out.println("Closing sockets...");

                        controlChannelThread.terminate();
                        dataBackupThread.terminate();
                        dataRestoreThread.terminate();

                        System.out.println("Synchronizing data...");

                        DataStorage.getInstance().synchronize();

                        System.out.println("Terminating...");

                        System.exit(0);

                        break;
                    }

                    default: {

                        System.out.println("");
                        System.out.println("Invalid choice!");
                        System.out.println("");

                        break;
                    }
                }

                System.out.println("");
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (controlChannelThread != null)
                try {
                    controlChannelThread.terminate();
                } catch (IOException exc) {
                    System.out.println("An error has occoured while closing one of the auxiliary threads. Proceeding anyway...");
                }

            if (dataBackupThread != null)
                try {
                    dataBackupThread.terminate();
                } catch (IOException exc) {
                    System.out.println("An error has occoured while closing one of the auxiliary threads. Proceeding anyway...");
                }

            if (dataRestoreThread != null)
                try {
                    dataRestoreThread.terminate();
                } catch (IOException exc) {
                    System.out.println("An error has occoured while closing one of the auxiliary threads. Proceeding anyway...");
                }

            System.out.println();

            System.out.println("Program exited abnormally.");

            System.exit(1);
        }
    }
}
