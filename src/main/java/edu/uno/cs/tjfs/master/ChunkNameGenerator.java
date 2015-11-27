package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.BaseLogger;
import org.apache.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ChunkNameGenerator {
    final static Logger logger = BaseLogger.getLogger(ChunkNameGenerator.class);

    public static Set<String> generate(int number) {
        Set<String> names = new HashSet<>();
        while (names.size() != number) {
            try {
                String name = Thread.currentThread().getName() + System.currentTimeMillis() + UUID.randomUUID();
                names.add(md5(name));
            } catch (Exception e) {
                logger.error("ChunkNameGenerator.generate - error while generating chunks");
            }
        }
        return names;
    }

    protected static String md5(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(value.getBytes());
            byte byteData[] = md.digest();

            // Convert it to hex
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // As fallback use base64
            return Base64.getEncoder().encodeToString(value.getBytes());
        }
    }
}
