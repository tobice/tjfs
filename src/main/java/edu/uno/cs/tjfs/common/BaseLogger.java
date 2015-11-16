package edu.uno.cs.tjfs.common;

import org.apache.log4j.Logger;

public class BaseLogger {
    final static Logger logger = Logger.getLogger(BaseLogger.class);
    public static void info(String message){
        System.out.println(message);
        logger.info(message);
    }

    public static void error(String message){
        logger.error(message);
    }

    public static void debug(String message){
        logger.debug(message);
    }

    public static void trace(String message){
        logger.trace(message);
    }
}
