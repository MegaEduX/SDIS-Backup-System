package pt.up.fe.Networking;

import pt.up.fe.Filesystem.BackedUpFile;
import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.File;
import pt.up.fe.Filesystem.StoredFile;
import pt.up.fe.Globals;
import pt.up.fe.Messaging.ChunkBackupAnswerMessage;
import pt.up.fe.Messaging.ChunkBackupMessage;
import pt.up.fe.Messaging.ChunkRestoreAnswerMessage;

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

    private static final String kAppVersion = "1.0";

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

        String parsedMessage[] = new String(message, "UTF-8").split(" ");

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

        ChunkRestoreAnswerMessage ram = new ChunkRestoreAnswerMessage(parsedMessage[1], parsedMessage[2], Integer.parseInt(parsedMessage[3]), message);

        setChanged();
        notifyObservers(ram);
    }

    public void parseRawMessageMDB(byte[] message) throws IOException {
        //  This is only called by the MDB.

        //  PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>

        String parsedMessage[] = new String(message, "UTF-8").split(" ");

        try {
            File f = DataStorage.getInstance().getBackedUpDatabase().getFileWithId(parsedMessage[2]);

            f.refrainFromStartingPropagation();

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
                f = DataStorage.getInstance().getStoredDatabase().getFileWithId(parsedMessage[2]);
            } catch (FileNotFoundException e) {
                f = new StoredFile(parsedMessage[2]);

                DataStorage.getInstance().getStoredDatabase().add(f);
            }

            f.refrainFromStartingPropagation();
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

        try {
            Thread.sleep((long) (Math.random() * 400));
        } catch (InterruptedException e) {
            //  Eh.
        }

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

                    f.addPeer(sender);

                    sender = null;
                } catch (FileNotFoundException e) {
                    //  File not found, proceeding anyway...
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
                    DataStorage.getInstance().getStoredDatabase().removeFileWithId(parsedMessage[2]);
                } catch (FileNotFoundException e) {
                    //  Chunk not found, probably we don't have it. Let's just ignore and proceed.
                }

                break;
            }

            case kMessageTypeRemoved: {
                File file = null;

                for (StoredFile f : DataStorage.getInstance().getStoredDatabase().getStoredFiles())
                    if (f.getId().equals(parsedMessage[2]))
                        file = f;

                if (file == null)
                    for (BackedUpFile f : DataStorage.getInstance().getBackedUpDatabase().getBackedUpFiles())
                        if (f.getId().equals(parsedMessage[2]))
                            file = f;

                if (file != null) {
                    int chunk = Integer.parseInt(parsedMessage[3]);

                    file.decreaseReplicationCountForChunk(chunk);

                    if (file.chunkNeedsReplication(chunk)) {
                        try {
                            Thread.sleep((long) (Math.random() * 400));
                        } catch (InterruptedException e) {
                            //  Eh.
                        }

                        if (file.getRefrainFromStartingPropagation())
                            return;

                        for (int i = 0; i < file.getNumberOfChunks(); i++) {
                            file.resetPeerList();

                            ChunkBackupMessage m = new ChunkBackupMessage(Globals.AppVersion,
                                    file.getId(),
                                    i,
                                    file.getDesiredReplicationCount(),
                                    file.getChunk(i));

                            for (int tries = 0;
                                 file.getPeerCount() < file.getDesiredReplicationCount() && tries < Globals.MaxTriesPerChunk;
                                 tries++) {
                                pc.getMDBSocket().sendRaw(m.getMessageData());

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
