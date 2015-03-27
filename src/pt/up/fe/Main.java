package pt.up.fe;

import pt.up.fe.Filesystem.*;
import pt.up.fe.Messaging.ChunkBackupMessage;
import pt.up.fe.Networking.MessageReceiver;
import pt.up.fe.Networking.MessageSender;
import pt.up.fe.Networking.ProtocolController;
import pt.up.fe.Networking.UDPMulticast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Scanner;

public class Main {

    public static String kAppVersion = "1.0";
    public static int kReplicationDeg = 5;
    public static int kMaxTriesPerChunk = 5;

    public static void main(String[] args) {
        try {

            /*

                So, if I understood this correctly.
                Chunk Backup -> PUTCHUNK sent on the MDB, STORED sent on the MC.
                Chunk Restore -> GETCHUNK sent on the MC, CHUNK sent on the MDR.
                Chunk Delete -> DELETE sent on the MC.
                Chunk Removed -> REMOVED sent on the MC.

             */

            DataStorage ds = DataStorage.getInstance();

            ds.setDataStorePath("/Users/MegaEduX/DataStorage/");

            final ProtocolController pc = new ProtocolController(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));

            final MessageReceiver rec = new MessageReceiver(pc);
            final MessageSender snd = new MessageSender(pc);

            /*

                MC Thread

             */

            Thread controlChannelThread = new Thread("MC Thread") {
                public void run() {
                    UDPMulticast mcSocket = pc.getMCSocket();

                    while (true) {
                        DatagramPacket packet = mcSocket.receive();

                        String outStr = new String(packet.getData(), 0, packet.getLength());

                        try {
                            rec.parseMessage(outStr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            controlChannelThread.start();

            /*

                MDB Thread

             */

            Thread dataBackupThread = new Thread("MDB Thread") {
                public void run() {
                    UDPMulticast mdbSocket = pc.getMDBSocket();

                    while (true) {
                        DatagramPacket packet = mdbSocket.receive();

                        String outStr = new String(packet.getData(), 0, packet.getLength());

                        try {
                            rec.parseMessage(outStr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            dataBackupThread.start();

            /*

                MDR Thread

             */

            Thread dataRestoreThread = new Thread("MDR Thread") {
                public void run() {
                    UDPMulticast mdrSocket = pc.getMDRSocket();

                    while (true) {
                        DatagramPacket packet = mdrSocket.receive();

                        String outStr = new String(packet.getData(), 0, packet.getLength());

                        try {
                            rec.parseMessage(outStr);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            /*

                User Interface

             */

            while (true) {
                System.out.println("##APP_NAME_HERE## is running.");
                System.out.println("");
                System.out.println("[1] Backup File...");
                System.out.println("[2] Restore File...");
                System.out.println("[0] Clean Up and Exit");
                System.out.println("");
                System.out.print("Choice: ");

                Scanner reader = new Scanner(System.in);

                switch (Integer.parseInt(reader.next())) {
                    case 1:

                        System.out.println("");

                        System.out.print("Path to file to backup: ");

                        String filePath = reader.next();

                        BackedUpFile f = null;

                        try {
                            f = new BackedUpFile(filePath);
                        } catch (IOException e) {
                            System.out.println("Unable to initiate a backup for the specified file.");

                            e.printStackTrace();

                            continue;
                        }

                        String fileId = f.getId();

                        for (int i = 0; i < f.getNumberOfChunks(); i++) {
                            System.out.println("Backing up chunk " + i + "/" + f.getNumberOfChunks() + "...");

                            ChunkBackupMessage m = new ChunkBackupMessage(kAppVersion, fileId, i, kReplicationDeg, f.getChunk(i));

                            for (int tries = 0; f.getReplicationCountForChunk(i) < kReplicationDeg && tries < kMaxTriesPerChunk; tries++) {
                                snd.sendMessage(m);

                                try {
                                    Thread.sleep((long) (Math.random() * 400));
                                } catch (InterruptedException e) {
                                    //  Ignoring.
                                }
                            }
                        }

                        break;

                    case 2:

                        System.out.println("");
                        System.out.println("Backed Up File List:");
                        System.out.println("");

                        int i = 0;

                        for (BackedUpFile bf : DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles())
                            System.out.println("[" + ++i + "] " + bf.getPath() + " (" + bf.getLastModified() + ")");

                        System.out.println("");
                        System.out.println("Showing " + i + " results.");
                        System.out.println("");
                        System.out.println("Which file do you wish to restore? (0 to cancel): ");

                        int choice = Integer.parseInt(reader.next());

                        while (choice > i || choice < 0) {
                            System.out.println("");
                            System.out.print("Invalid choice. Please retry: ");

                            choice = Integer.parseInt(reader.next());
                        }

                        //  TODO: A lot of code here.

                        if (choice == 0) {

                        } else {

                        }

                        break;

                    case 0:

                        break;

                    default:

                        System.out.println("");
                        System.out.println("Invalid choice!");
                        System.out.println("");

                        continue;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
