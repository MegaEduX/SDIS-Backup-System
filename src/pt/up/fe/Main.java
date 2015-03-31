package pt.up.fe;

import pt.up.fe.Filesystem.*;
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
import java.util.Observable;
import java.util.Scanner;
import java.util.Vector;

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

    public static final String kAppName = "##APP_NAME_HERE##";
    public static final String kAppVersion = "1.0";

    public static final int kReplicationDeg = 5;
    public static final int kMaxTriesPerChunk = 5;

    public static void main(String[] args) {
        MC controlChannelThread = null;
        MDB dataBackupThread = null;

        try {

            /*  if (args.length != 6) {
                System.out.println("Usage: project <MC Multicast IP Address> <MC Port> <MDB Multicast IP Address> <MDB Port> <MDR Multicast IP Address> <MDR Port>");

                return;
            }   */

            /*

                So, if I understood this correctly.
                Chunk Backup -> PUTCHUNK sent on the MDB, STORED sent on the MC.
                Chunk Restore -> GETCHUNK sent on the MC, CHUNK sent on the MDR.
                Chunk Delete -> DELETE sent on the MC.
                Chunk Removed -> REMOVED sent on the MC.

             */

            System.out.println(kAppName + " is running.");

            System.out.println("");

            Scanner reader = new Scanner(System.in);

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

            //  final ProtocolController pc = new ProtocolController(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));

            final ProtocolController pc = new ProtocolController("224.1.1.1", 1234, "224.2.2.2", 2345, "224.3.3.3", 3456);

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

            MDR dataRestoreThread = new MDR(pc, rec);

            //  Not started!

            /*

                User Interface

             */

            while (true) {
                System.out.println("[1] Backup File...");
                System.out.println("[2] Restore File...");
                System.out.println("[3] Remove Local (Backed Up) File...");
                System.out.println("[4] Free Up Space (Remove Stored Chunks)...");
                System.out.println("[0] Clean Up and Exit");
                System.out.println("");
                System.out.print("Choice: ");

                switch (Integer.parseInt(reader.next())) {
                    case 1: {

                        System.out.println("");

                        System.out.print("Path to file to backup: ");

                        String filePath = reader.next();

                        BackedUpFile f;

                        try {
                            f = new BackedUpFile(filePath);
                        } catch (IOException e) {
                            System.out.println("Unable to initiate a backup for the specified file.");

                            //  e.printStackTrace();

                            continue;
                        }

                        String fileId = f.getId();

                        System.out.println("");

                        for (int i = 0; i < f.getNumberOfChunks(); i++) {
                            System.out.println("Backing up chunk " + (i + 1) + "/" + f.getNumberOfChunks() + "...");    //  Peasants start counting at 1...

                            ChunkBackupMessage m = new ChunkBackupMessage(kAppVersion, fileId, i, kReplicationDeg, f.getChunk(i));

                            for (int tries = 0; f.getReplicationCountForChunk(i) < kReplicationDeg && tries < kMaxTriesPerChunk; tries++) {
                                try {
                                    snd.sendMessage(m);
                                } catch (MessageSender.UnknownMessageException e) {
                                    System.out.println("Unknown message discarded...");
                                }

                                try {
                                    Thread.sleep((long) (Math.random() * 400));
                                } catch (InterruptedException e) {
                                    //  Ignoring.
                                }
                            }
                        }

                        System.out.println("Operation Complete.");

                        break;
                    }

                    case 2: {

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

                        int choice = Integer.parseInt(reader.next());

                        while (choice > i || choice < 0) {
                            System.out.println("");
                            System.out.print("Invalid choice. Please retry: ");

                            choice = Integer.parseInt(reader.next());
                        }

                        if (choice == 0)
                            continue;
                        else {
                            new Thread(dataRestoreThread).start();

                            BackedUpFile bf = DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles().get(i - 1);

                            System.out.println("Restoring " + bf.getPath() + "...");

                            try {
                                Vector<String> restoredChunks = new Vector<>();

                                AtomicBoolean gotChunk = new AtomicBoolean(false);   //  Eww.

                                for (int j = 0; j < bf.getNumberOfChunks(); j++) {
                                    gotChunk.set(false);

                                    System.out.println("Restoring chunk " + j + "/" + bf.getNumberOfChunks() + "...");

                                    try {
                                        snd.sendMessage(new ChunkRestoreMessage(kAppVersion, bf.getId(), j));

                                        dataRestoreThread.addObserver((Observable obj, Object arg) -> {
                                            restoredChunks.add((String) arg);

                                            gotChunk.set(true);
                                        });
                                    } catch (MessageSender.UnknownMessageException e) {
                                        System.out.println("Unknown message discarded...");
                                    }

                                    for (int k = 0; k < 5 && !gotChunk.get(); k++)
                                        Thread.sleep(1000);

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
                                        System.out.println("Save Path: ");

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

                            dataRestoreThread.terminate();
                        }

                        break;
                    }

                    case 3: {

                        System.out.println("");
                        System.out.println("Use this interface to remove a file from your file system and from peers who have it backed up.");
                        System.out.println("");

                        System.out.println("Backed Up File List:");
                        System.out.println("");

                        int i = 0;

                        for (BackedUpFile bf : DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles())
                            System.out.println("[" + ++i + "] " + bf.getPath() + " (" + bf.getLastModified() + ")");

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

                        if (choice == 0)
                            continue;
                        else {
                            new Thread(dataRestoreThread).start();

                            BackedUpFile bf = DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles().get(i - 1);

                            System.out.println("Restoring " + bf.getPath() + "...");

                            try {
                                Files.delete(Paths.get(bf.getPath()));
                            } catch (NoSuchFileException e) {
                                System.err.format("%s: no such" + " file or directory%n", bf.getPath());

                                System.out.println("An error has occoured. Proceeding anyway...");
                            } catch (IOException e) {
                                System.out.println("An error has occoured. Proceeding anyway...");
                            }

                            try {
                                snd.sendMessage(new FileDeletionMessage(kAppVersion, bf.getId()));
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

                            for (StoredFile sf : DataStorage.getInstance().getStoredDatabase().getStoredFiles()) {
                                for (Integer chunk : sf.getChunksStored()) {
                                    DataStorage.getInstance().removeChunk(sf.getId(), chunk);

                                    try {
                                        snd.sendMessage(new SpaceReclaimMessage(kAppVersion, sf.getId(), chunk));
                                    } catch (MessageSender.UnknownMessageException e) {
                                        System.out.println("Unknown Message Type - this is an internal inconsistency error.");
                                    }

                                    i++;

                                    if (i == choice)
                                        break;
                                }

                                if (i == choice)
                                    break;
                            }

                            System.out.println("Operation Complete. Removed " + i + " chunks.");
                        }

                        break;
                    }

                    case 0: {

                        System.out.println("");

                        System.out.println("Closing sockets...");

                        controlChannelThread.terminate();
                        dataBackupThread.terminate();

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
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (controlChannelThread != null)
                controlChannelThread.terminate();

            if (dataBackupThread != null)
                dataBackupThread.terminate();

            System.out.println("Program exited abnormally.");

            System.exit(0);
        }
    }
}
