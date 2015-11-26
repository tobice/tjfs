package edu.uno.cs.tjfs.common;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BaseLogger extends Logger {
    protected BaseLogger(String name) {
        super(name);
    }

    public void error(String sender, Throwable throwable){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        super.error(sender + sw.toString());
    }
}
