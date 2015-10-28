package edu.uno.cs.tjfs.client;

public enum CommandName {
    GET("get"),
    PUT("put"),
    DELETE("delete"),
    GET_SIZE("getsize"),
    GET_TIME("gettime"),
    LIST("list"),
    CD("cd");

    private String value;

    private CommandName(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
