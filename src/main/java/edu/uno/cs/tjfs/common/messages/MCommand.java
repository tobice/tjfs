package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.*;

import java.util.HashMap;
import java.util.Map;

public enum MCommand {
    GET_CHUNK("01", GetChunkRequestArgs.class, GetChunkResponseArgs.class),
    PUT_CHUNK("02", PutChunkRequestArgs.class, PutChunkResponseArgs.class),
    DELETE_CHUNK("03", DeleteChunkRequestArgs.class, DeleteChunkResponseArgs.class),
    LIST_CHUNK("04", ListChunkRequestArgs.class, DeleteChunkResponseArgs.class),
    REPLICATE_CHUNK("05", ReplicateChunkRequestArgs.class, null),
    GET_FILE("06", GetFileRequestArgs.class, GetFileResponseArgs.class),
    PUT_FILE("07", PutFileRequestArgs.class, PutFileResponseArgs.class),
    ALLOCATE_CHUNKS("08", AllocateChunksRequestArgs.class, AllocateChunkResponseArgs.class),
    GET_LOG("09", GetLogRequestArgs.class, GetLogResponseArgs.class),
    REPLICATE_CHUNK_SYNC("10", ReplicateChunkRequestArgs.class, ErrorResponseArgs.class);

    public String value;
    public Class requestClass;
    public Class responseClass;

    MCommand(String value, Class requestClass, Class responseClass){
        this.value = value;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
    }

    private final static Map<String, MCommand> map =
            new HashMap<>(MCommand.values().length, 1.0f);
    static {
        for (MCommand c : MCommand.values()) {
            map.put(c.value, c);
        }
    }

    public static MCommand of(String name) {
        MCommand result = map.get(name);
        if (result == null) {
            throw new IllegalArgumentException("No Command Exists");
        }
        return result;
    }
}
