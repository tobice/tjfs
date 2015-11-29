package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.master.IMasterStorage;

public class GetLatestSnapshotsResponseArgs implements IMessageArgs {
    public final IMasterStorage.Snapshot snapshot;

    public GetLatestSnapshotsResponseArgs(IMasterStorage.Snapshot snapshot) {
        this.snapshot = snapshot;
    }
}
