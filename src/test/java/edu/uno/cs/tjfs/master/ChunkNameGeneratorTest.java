package edu.uno.cs.tjfs.master;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChunkNameGeneratorTest {
    @Test
    public void generateChunkNamesTest(){
        for (int counter = 0; counter < 100; counter++){
            Set<String> names = ChunkNameGenerator.generate(10);
            assertTrue(names.size() == 10);
            String[] namesArray = new String[10];
            names.toArray(namesArray);
            ArrayList<String> namesList = new ArrayList<>(Arrays.asList(namesArray));
            for (String name : names){
                namesList.remove(name);
                assertFalse(namesList.contains(name));
            }
        }
    }
}
