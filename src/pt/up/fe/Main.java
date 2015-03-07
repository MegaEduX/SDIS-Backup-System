package pt.up.fe;

import pt.up.fe.Filesystem.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        //  Some test code...

        try {
            DataStorage ds = new DataStorage("/Users/MegaEduX/DataStorage/");

            BackedUpFile f = new BackedUpFile("/Users/MegaEduX/Dudu/Tempo.java");

            byte[] fileId = f.generateFileId();

            System.out.println("File Id: " + new String(fileId));

            System.out.println("Chunk 1: " + new String(f.getChunk(0)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
