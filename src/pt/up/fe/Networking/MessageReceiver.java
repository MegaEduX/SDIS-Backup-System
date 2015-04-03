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

        try {
            DataStorage.getInstance().getBackedUpDatabase().getFileWithId(parsedMessage[2]);

            return;     //  We are the owner of this file - it makes no sense at all to back it up too.
        } catch (FileNotFoundException e) {
            //  All good.
        }

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

        switch (parsedMessage[0]) {
            case kMessageTypeStored: {
                try {
                    BackedUpFile f = DataStorage.getInstance().
                            getBackedUpDatabase().
                            getFileWithId(parsedMessage[2]);

                    f.increaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));
                    f.addPeer(sender);

                    sender = null;
                } catch (FileNotFoundException e) {
                    System.out.println("File not found, proceeding anyway...");
                }

                break;
            }

            case kMessageTypeGetChunk: {
                try {
                    ChunkRestoreAnswerMessage msg = new ChunkRestoreAnswerMessage(parsedMessage[1],
                            parsedMessage[2],
                            Integer.parseInt(parsedMessage[3]),
                            DataStorage.getInstance().retrieveChunk(parsedMessage[2],
                                    Integer.parseInt(parsedMessage[3])
                            )
                    );

                    pc.getMDRSocket().sendRaw(msg.getMessageData());
                } catch (IOException e) {
                    //  We don't have the file. :(
                }

                break;
            }

            case kMessageTypeDelete: {
                try {
                    DataStorage.getInstance().getStoredDatabase().removeFileWithChunkId(parsedMessage[2]);
                } catch (FileNotFoundException e) {
                    System.out.println("Chunk not found, ignoring...");
                }

                break;
            }

            case kMessageTypeRemoved: {
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

                break;
            }

            default:

                break;
        }
    }
}
