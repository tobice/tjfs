package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.CustomGson;
import edu.uno.cs.tjfs.common.ILocalFsClient;
import edu.uno.cs.tjfs.common.TjfsException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** Thin layer for storing and restoring the snapshots */
public class SnapshotStorage {
    protected final ILocalFsClient localFsClient;
    protected final Gson gson = CustomGson.create();
    protected final Path snapshotFolder;

    public SnapshotStorage(ILocalFsClient localFsClient, Path snapshotFolder) {
        this.localFsClient = localFsClient;
        this.snapshotFolder = snapshotFolder;
    }

    public void store(IMasterStorage.Snapshot snapshot) throws TjfsException {
        try {
            byte[] data = gson.toJson(snapshot).getBytes("UTF-8");
            localFsClient.writeBytesToFile(snapshotFolder.resolve(snapshot.version + ""), data);
        } catch (IOException e) {
            throw new TjfsException("Unable to store snapshot. " + e.getMessage(), e);
        }
    }

    public IMasterStorage.Snapshot restore(int version) throws TjfsException {
        try {
            Path path = snapshotFolder.resolve(version + "");
            if (!localFsClient.exists(path)) {
                throw new TjfsException("Snapshot does not exist");
            }

            byte[] data = localFsClient.readBytesFromFile(path);
            return gson.fromJson(new String(data, "UTF-8"), IMasterStorage.Snapshot.class);
        } catch (IOException e) {
            throw new TjfsException("Unable to store snapshot. " + e.getMessage(), e);
        }
    }

    public IMasterStorage.Snapshot getLatest() throws TjfsException {
        Integer version = Arrays.asList(localFsClient.list(snapshotFolder)).stream()
            .map(Integer::parseInt)
            .max(Integer::compare)
            .get();

        if (version == null) {
            return null;
        } else {
            return restore(version);
        }
    }
}
