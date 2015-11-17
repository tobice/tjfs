package edu.uno.cs.tjfs.common;

public class Machine {
    public final String ip;
    public final int port;

    public Machine(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public boolean equals(Object object) {
        if (!(object instanceof Machine)) {
            return false;
        }
        Machine otherMachine = (Machine) object;
        return this.ip.equals(otherMachine.ip) && this.port == otherMachine.port;
    }

    public int hashCode() {
        // This *should* generate a unique code for this machine
        return 37 * ip.hashCode() + port;
    }
}
