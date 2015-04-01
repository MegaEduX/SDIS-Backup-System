package pt.up.fe.Utilities;

import com.sun.istack.internal.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Security {
    @NotNull public static String hashSHA256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }

    @NotNull private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
