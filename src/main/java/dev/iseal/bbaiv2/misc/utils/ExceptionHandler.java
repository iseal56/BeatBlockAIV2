package dev.iseal.bbaiv2.misc.utils;

public class ExceptionHandler {

    public static void panic(Exception e) {
        System.err.println("An error occurred, panicking! error: " + e.getMessage());
        e.printStackTrace();
        Runtime.getRuntime().halt(-1);
    }

    public static void error(Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
        e.printStackTrace();
        System.exit(-1);
    }

}
