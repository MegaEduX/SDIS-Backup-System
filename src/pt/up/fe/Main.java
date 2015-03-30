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
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static final String kAppName = "##APP_NAME_HERE##";
    public static final String kAppVersion = "1.0";

    public static final int kReplicationDeg = 5;
    public static final int kMaxTriesPerChunk = 5;

    public static void main(String[] args) {
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

            DataStorage ds = DataStorage.getInstance();

            ds.setDataStorePath("/Users/MegaEduX/DataStorage/");

            //  final ProtocolController pc = new ProtocolController(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));

            final ProtocolController pc = new ProtocolController("224.1.1.1", 1234, "224.2.2.2", 2345, "224.3.3.3", 3456);

            final MessageReceiver rec = new MessageReceiver(pc);
            final MessageSender snd = new MessageSender(pc);

            /*

                MC Thread

             */

            MC controlChannelThread = new MC(pc, rec);

            new Thread(controlChannelThread).start();

            /*

                MDB Thread

             */

            MDB dataBackupThread = new MDB(pc, rec);

            new Thread(dataBackupThread).start();

            /*

                MDR Thread

             */

            MDR dataRestoreThread = new MDR(pc, rec);

            //  Not started!

            /*

                User Interface

             */

            System.out.println(kAppName + " is running.");

            while (true) {
                System.out.println("");
                System.out.println("[1] Backup File...");
                System.out.println("[2] Restore File...");
                System.out.println("[3] Remove Local (Backed Up) File...");
                System.out.println("[4] Free Up Space (Remove Stored Chunks)...");
                System.out.println("[0] Clean Up and Exit");
                System.out.println("");
                System.out.print("Choice: ");

                Scanner reader = new Scanner(System.in);

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

                            for (int j = 0; j < bf.getNumberOfChunks(); j++) {
                                System.out.println("Restoring chunk " + j + "/" + bf.getNumberOfChunks() + "...");

                                try {
                                    snd.sendMessage(new ChunkRestoreMessage(kAppVersion, bf.getId(), j));
                                } catch (MessageSender.UnknownMessageException e) {
                                    System.out.println("Unknown message discarded...");
                                }

                                try {
                                    Thread.sleep((long) (Math.random() * 400));
                                } catch (InterruptedException e) {
                                    //  Ignoring.
                                }
                            }

                            try {
                                Thread.sleep((long) 2000);
                            } catch (InterruptedException e) {
                                //  Ignoring.
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

                            java.io.File f = new java.io.File(bf.getPath());

                            if (!f.delete())
                                System.out.println("Unable to delete it locally - proceeding anyway and deleting from remote peers...");

                            try {
                                snd.sendMessage(new FileDeletionMessage(kAppVersion, bf.getId()));
                            } catch (MessageSender.UnknownMessageException e) {

                            }
                        }

                        System.out.println("Operation Complete.");

                        break;
                    }

                    case 4: {

                        System.out.println("");
                        System.out.println("Use this interface to remove a number of chunks stored on your system in order to free up disk space.");
                        System.out.println("");

                        System.out.println("Number of chunks to remove (0 to cancel):");

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

                            for (StoredFile sf : ds.getStoredDatabase().getStoredFiles()) {
                                for (Integer chunk : sf.getChunksStored()) {
                                    ds.removeChunk(sf.getId(), chunk);

                                    try {
                                        snd.sendMessage(new SpaceReclaimMessage(kAppVersion, sf.getId(), chunk));
                                    } catch (MessageSender.UnknownMessageException e) {

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

                        continue;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
