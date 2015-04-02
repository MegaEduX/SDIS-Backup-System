package pt.up.fe.Networking;

import pt.up.fe.Filesystem.BackedUpFile;
import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.StoredFile;
import pt.up.fe.Messaging.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Observable;

/**
 *      Maintains the receiving socket and parses incoming messages.
 */

public class MessageReceiver extends Observable {
    private ProtocolController pc = null;
    private InetAddress sender = null;

    private static final String kMessageTypeStored = "STORED";
    private static final String kMessageTypeGetChunk = "GETCHUNK";
    private static final String kMessageTypeDelete = "DELETE";
    private static final String kMessageTypeRemoved = "REMOVED";

    public MessageReceiver(ProtocolController controller) {
        pc = controller;
    }

    //  Borrowed from http://stackoverflow.com/questions/642897/removing-an-element-from-an-array-java

    public void removeElement(byte[] a, int del) {
        System.arraycopy(a, del + 1, a, del, a.length - 1 - del);
    }

    public void setSender(InetAddress snd) {
        sender = snd;
    }

    public void parseRawMessageMDR(byte[] message) throws IOException {
        //  This is only called by the MDR.

        //  CHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF><Body>

        System.out.println("Chunk recover done.");

        int bytesRemoved = 0;

        for (int i = 0; i < message.length; i++) {
            if (message[i] == (byte)'\r' &&
                    message[i + 1] == (byte)'\n' &&
                    message[i + 2] == (byte)'\r' &&
                    message[i + 3] == (byte)'\n') {
                for (int j = 0; j <= i + 3; j++, bytesRemoved++)
                    removeElement(message, 0);

                break;
            }
        }

        message = Arrays.copyOf(message, message.length - bytesRemoved);

        setChanged();
        notifyObservers(message);
    }

    public void parseRawMessageMDB(byte[] message) throws IOException {
        //  This is only called by the MDB.

        //  PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>

        String parsedMessage[] = new String(message, "UTF-8").split(" ");

        System.out.println("(1) Message length: " + message.length);

        int bytesRemoved = 0;

        for (int i = 0; i < message.length; i++) {
            if (message[i] == (byte)'\r' &&
                    message[i + 1] == (byte)'\n' &&
                    message[i + 2] == (byte)'\r' &&
                    message[i + 3] == (byte)'\n') {
                for (int j = 0; j <= i + 3; j++, bytesRemoved++)
                    removeElement(message, 0);

                break;
            }
        }

        message = Arrays.copyOf(message, message.length - bytesRemoved);

        System.out.println("(2) Message length: " + message.length);

        if (DataStorage.getInstance().storeChunk(parsedMessage[2], Integer.parseInt(parsedMessage[3]), message)) {
            StoredFile f;

            try {
                f = DataStorage.getInstance().getStoredDatabase().getFileWithChunkId(parsedMessage[2]);
            } catch (FileNotFoundException e) {
                f = new StoredFile(parsedMessage[2]);

                DataStorage.getInstance().getStoredDatabase().add(f);
            }

            f.setDesiredReplicationCount(Integer.parseInt(parsedMessage[4]));

            try {
                f.setChunkStoredStatus(Integer.parseInt(parsedMessage[3]), true);
                f.increaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));
            } catch (NumberFormatException e) {
                System.out.println("Okay, we can't keep going like this... Is someone messing up with us?");
            }
        }

        ChunkBackupAnswerMessage msg = new ChunkBackupAnswerMessage(parsedMessage[1],
                parsedMessage[2],
                Integer.parseInt(parsedMessage[3]));

        pc.getMCSocket().send(new String(msg.getMessageData(), "UTF-8"));
    }

    public void parseMessage(String Message) throws IOException {
        String parsedMessage[] = Message.split(" ");

        if (parsedMessage[0].equals(kMessageTypeStored)) {
            //  STORED <Version> <FileId> <ChunkNo> <CRLF><CRLF>

            System.out.println("STORED Confirmation Message Received.");

            try {
                BackedUpFile f = DataStorage.getInstance().
                        getBackedUpDatabase().
                        getFileWithChunkId(parsedMessage[2]);

                f.increaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));
                f.addPeer(sender);

                sender = null;
            } catch (FileNotFoundException e) {
                System.out.println("File not found, proceeding anyway...");
            }
        } else if (parsedMessage[0].equals(kMessageTypeGetChunk)) {
            //  Chunk Restore Protocol - GETCHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF>

            System.out.println("Chunk recover requested.");

            ChunkRestoreAnswerMessage msg = new ChunkRestoreAnswerMessage(parsedMessage[1],
                    parsedMessage[2],
                    Integer.parseInt(parsedMessage[3]),
                    DataStorage.getInstance().retrieveChunk(parsedMessage[2],
                            Integer.parseInt(parsedMessage[3])
                    )
            );

            pc.getMDRSocket().sendRaw(msg.getMessageData());
        } else if (parsedMessage[0].equals(kMessageTypeDelete)) {
            //  File deletion Protocol - DELETE <Version> <FileId> <CRLF><CRLF>

            try {
                DataStorage.getInstance().getStoredDatabase().removeFileWithChunkId(parsedMessage[2]);
            } catch (FileNotFoundException e) {
                System.out.println("Chunk not found, ignoring...");
            }
        } else if (parsedMessage[0].equals(kMessageTypeRemoved)) {
            //  Space Reclaiming Protocol - REMOVED <Version> <FileId> <ChunkNo> <CRLF><CRLF>

            System.out.println("Updating local counter...");

            for (StoredFile f : DataStorage.getInstance().getStoredDatabase().getStoredFiles()) {
                if (f.getId().equals(parsedMessage[2])) {
                    int chunk = Integer.parseInt(parsedMessage[3]);

                    f.decreaseReplicationCountForChunk(chunk);

                    if (f.chunkNeedsReplication(chunk)) {
                        ChunkBackupMessage m = new ChunkBackupMessage("1.0",
                                f.getId(),
                                chunk,
                                f.getDesiredReplicationCount(),
                                DataStorage.getInstance().retrieveChunk(f.getId(), chunk)
                        );

                        for (int tries = 0; f.getReplicationCountForChunk(chunk) < f.getDesiredReplicationCount() && tries < 5; tries++) {
                            System.out.println("Current replication count: " + f.getReplicationCountForChunk(chunk));

                            try {
                                pc.getMDBSocket().sendRaw(m.getMessageData());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            try {
                                Thread.sleep((long) (Math.random() * 400));
                            } catch (InterruptedException e) {
                                //  Ignoring.
                            }
                        }
                    }
                }
            }
        }
    }
}
