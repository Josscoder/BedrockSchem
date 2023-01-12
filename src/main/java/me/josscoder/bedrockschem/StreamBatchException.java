package me.josscoder.bedrockschem;

public class StreamBatchException extends Exception {
    public StreamBatchException(String className, String value) {
        super(String.format("Error to complete %s:%s", className, value));
    }
}
