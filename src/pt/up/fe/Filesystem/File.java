package pt.up.fe.Filesystem;

public class File {
    static int kChunkLengthInBytes = 64000;

    String _id;
    int _numberOfChunks;

    //  Borrowed from http://www.java2s.com/Code/Java/File-Input-Output/GetFileSizeInMB.htm

    public String getId() {
        return _id;
    }

    public int getNumberOfChunks() {
        return _numberOfChunks;
    }

    public static long getFileSizeInBytes(String fileName) {
        long ret = 0;

        java.io.File f = new java.io.File(fileName);

        if (f.isFile())
            return f.length();
        else if (f.isDirectory()) {
            java.io.File[] contents = f.listFiles();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isFile()) {
                    ret += contents[i].length();
                } else if (contents[i].isDirectory())
                    ret += getFileSizeInBytes(contents[i].getPath());
            }
        }

        return ret;
    }
}
