package me.qyh.instd4j.parser.exception;

public class LogicException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public LogicException(String message) {
        super(message, null, false, false);
    }

}
