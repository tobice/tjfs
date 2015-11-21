package edu.uno.cs.tjfs.master;

import java.util.*;

public class ChunkNameGenerator {
    // TODO: test that you get n different strings
    public Set<String> generater(int number) {
        Set<String> names = new HashSet<>();
        while (names.size() != number) {
            // TODO: make hash (md5?)
            names.add(Thread.currentThread().getName() + System.currentTimeMillis() + UUID.randomUUID());
        }

        return names;
    }


}
