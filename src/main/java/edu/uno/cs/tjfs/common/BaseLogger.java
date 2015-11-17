package edu.uno.cs.tjfs.common;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BaseLogger {
    final static Logger logger = Logger.getLogger(BaseLogger.class);
    public static void info(String message){
        logger.info(message);
    }

    public static void error(String message){
        logger.error(message);
    }

    public static void error(String sender, Throwable throwable){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);

        error(sender + sw.toString());
    }

    public static void debug(String message){
        logger.debug(message);
    }

    public static void trace(String message){
        logger.trace(message);
    }
}
