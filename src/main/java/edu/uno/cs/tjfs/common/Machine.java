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

    public String toString() {
        // TODO: test this!
        return ip + ":" + port;
    }

    public byte[] getBytes() {
        // TODO: test this!
        return toString().getBytes();
    }

    public static Machine fromString(String machine) {
        int colonPosition = machine.lastIndexOf(":");
        if (colonPosition == -1) {
            throw new IllegalArgumentException("This doesn't look like a connection string");
        }

        return new Machine(machine.substring(0, colonPosition),
                Integer.parseInt(machine.substring(colonPosition + 1)));
    }

    public static Machine fromBytes(byte[] bytes) {
        return fromString(new String(bytes));
    }
}
