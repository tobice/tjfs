package edu.uno.cs.tjfs.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Custom GsonBuilder that adds adapters for some problematic types */
public class CustomGson {
    protected static GsonBuilder gsonBuilder;

    /** Adapter for java.nio.Path that contains cyclic reference */
    protected static class PathTypeAdapter extends TypeAdapter<Path> {
        @Override
        public void write(JsonWriter jsonWriter, Path path) throws IOException {
            jsonWriter.value(path.toString());
        }

        @Override
        public Path read(JsonReader jsonReader) throws IOException {
            return Paths.get(jsonReader.nextString());
        }
    }

    /** Get new instance of Gson with custom adapters */
    public static Gson create() {
        // Initialize the builder (only once) and register all custom adapters
        if (gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Path.class, new PathTypeAdapter());
        }

        return gsonBuilder.create();
    }
}
