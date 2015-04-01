package pt.up.fe.Networking;

import pt.up.fe.Filesystem.BackedUpFile;
import pt.up.fe.Filesystem.DataStorage;
import pt.up.fe.Filesystem.StoredFile;
import pt.up.fe.Messaging.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Observable;
import java.util.Vector;

/**
 *      Maintains the receiving socket and parses incoming messages.
 */

public class MessageReceiver extends Observable {
    ProtocolController pc;

    private static final String kMessageTypePutChunk = "PUTCHUNK";
    private static final String kMessageTypeStored = "STORED";
    private static final String kMessageTypeGetChunk = "GETCHUNK";
    private static final String kMessageTypeChunk = "CHUNK";
    private static final String kMessageTypeDelete = "DELETE";
    private static final String kMessageTypeRemoved = "REMOVED";

    public MessageReceiver(ProtocolController controller) {
        pc = controller;
    }

    //  Borrowed from http://stackoverflow.com/questions/642897/removing-an-element-from-an-array-java

    public void removeElement(byte[] a, int del) {
        System.arraycopy(a, del + 1, a, del, a.length - 1 - del);
    }

    //  TODO: The return of this function isn't specified well.

    public void parseMessage(String Message) throws IOException {
        String parsedMessage[] = Message.split(" ");

        //  Chunk Backup Protocol - PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>

        if (parsedMessage[0].equals(kMessageTypePutChunk)) {
            String[] dataStr = Arrays.copyOfRange(parsedMessage, 5, parsedMessage.length - 1);

            StringBuffer res = new StringBuffer();

            for (String s : dataStr)
                res.append(s);

            byte[] data = res.toString().getBytes();

            for (int i = 0; i < 4; i++)
                removeElement(data, 0);

            //  System.out.println("Storing chunk...");

            if (DataStorage.getInstance().storeChunk(parsedMessage[2], Integer.parseInt(parsedMessage[3]), data)) {
                try {
                    StoredFile f = DataStorage.getInstance().getStoredDatabase().getFileWithChunkId(parsedMessage[2]);

                    f.increaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));
                } catch (FileNotFoundException e) {
                    DataStorage.getInstance().getStoredDatabase().add(
                            new StoredFile(parsedMessage[2], new Vector<>(Integer.parseInt(parsedMessage[3]))));
                }

                ChunkBackupAnswerMessage msg = new ChunkBackupAnswerMessage(parsedMessage[1],
                        parsedMessage[2],
                        Integer.parseInt(parsedMessage[3]));

                pc.getMCSocket().send(new String(msg.getMessageData(), "UTF-8"));
            }

            return;
        }

        //  STORED <Version> <FileId> <ChunkNo> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeStored)) {
            System.out.println("STORED Confirmation Message Received.");

            try {
                BackedUpFile f = DataStorage.getInstance().
                        getBackedUpDatabase().
                        getFileWithChunkId(parsedMessage[2]);

                f.increaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));

                /*  System.out.println("Increased replication count for chunk " +
                        Integer.parseInt(parsedMessage[3]) +
                        " - current count: " +
                        f.getReplicationCountForChunk(Integer.parseInt(parsedMessage[3]))); */
            } catch (FileNotFoundException e) {
                System.out.println("File not found, proceeding anyway...");
            }
        }

        //  Chunk Restore Protocol - GETCHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeGetChunk)) {
            System.out.println("Chunk recover requested.");

            ChunkRestoreAnswerMessage msg = new ChunkRestoreAnswerMessage(parsedMessage[1],
                    parsedMessage[2],
                    Integer.parseInt(parsedMessage[3]),
                    DataStorage.getInstance().retrieveChunk(parsedMessage[2],
                            Integer.parseInt(parsedMessage[3])
                    )
            );

            pc.getMDRSocket().send(new String(msg.getMessageData(), "UTF-8"));

            //  return CHUNK_OK;

            return;
        }

        //  CHUNK <Version> <FileId> <ChunkNo> <CRLF><CRLF><Body>

        if (parsedMessage[0].equals(kMessageTypeChunk)) {
            //  We should only receive this message if we are subscribed to the MDR.
            //  So, if we receive it, we want it.

            //  TODO: CHUNK - It would be useful if the return of this function returned the received chunk data... Or something.

            System.out.println("Chunk recover done.");

            String body = parsedMessage[parsedMessage.length - 1];

            setChanged();
            notifyObservers(body);

            return;
        }

        //  File deletion Protocol - DELETE <Version> <FileId> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeDelete)) {
            try {
                DataStorage.getInstance().getStoredDatabase().removeFileWithChunkId(parsedMessage[2]);

                //  return DELETE_OK;

                return;
            } catch (FileNotFoundException e) {
                System.out.println("Chunk not found, ignoring...");
            }

            //  return ERROR;

            return;
        }

        //  Space Reclaiming Protocol - REMOVED <Version> <FileId> <ChunkNo> <CRLF><CRLF>

        if (parsedMessage[0].equals(kMessageTypeRemoved)) {
            boolean removed = false;

            System.out.println("Updating local counter...");

            for (StoredFile f : DataStorage.getInstance().getStoredDatabase().getStoredFiles()) {
                if (f.getId().equals(parsedMessage[2])) {
                    f.decreaseReplicationCountForChunk(Integer.parseInt(parsedMessage[3]));

                    removed = true;

                    break;
                }
            }

            if (removed)
                System.out.println("Decreased replication count for " + parsedMessage[2] + ".");
            else
                System.out.println("Unable to decrease replication count for " + parsedMessage[2] + ".");

            return;
        }
    }
}
