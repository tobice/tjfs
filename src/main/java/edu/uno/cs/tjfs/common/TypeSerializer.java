package edu.uno.cs.tjfs.common;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.nio.file.Path;

public class TypeSerializer implements JsonSerializer<Path> {

    @Override
    public JsonElement serialize(Path path, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject result = new JsonObject();
        result.add("id", new JsonPrimitive(path.toString()));
        return result;
    }
}
