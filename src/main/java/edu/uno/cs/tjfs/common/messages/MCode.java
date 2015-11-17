package edu.uno.cs.tjfs.common.messages;

import java.util.HashMap;
import java.util.Map;

public enum MCode {
    SUCCESS("90"),
    ERROR("91");

    public String value;
    MCode (String value){
        this.value = value;
    }

    private final static Map<String, MCode> map =
            new HashMap<>(MCode.values().length, 1.0f);
    static {
        for (MCode c : MCode.values()) {
            map.put(c.value, c);
        }
    }

    public static MCode of(String name) {
        MCode result = map.get(name);
        if (result == null) {
            throw new IllegalArgumentException("No Code Exists");
        }
        return result;
    }
}
