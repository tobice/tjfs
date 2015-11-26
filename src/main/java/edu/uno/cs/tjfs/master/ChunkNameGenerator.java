package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.BaseLogger;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import sun.nio.ch.IOUtil;

import java.security.MessageDigest;
import java.util.*;

public class ChunkNameGenerator {
    final static Logger logger = BaseLogger.getLogger(ChunkNameGenerator.class);
    // TODO: test that you get n different strings
    public static Set<String> generate(int number) {
        Set<String> names = new HashSet<>();
        while (names.size() != number) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String name = Thread.currentThread().getName() + System.currentTimeMillis() + UUID.randomUUID();
                names.add(IOUtils.toString(md.digest(name.getBytes()), "UTF-8"));
            } catch (Exception e) {
                logger.error("ChunkNameGenerator.generate - error while generating chunks");
            }
        }
        return names;
    }
}
