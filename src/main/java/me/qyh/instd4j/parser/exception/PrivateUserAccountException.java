package me.qyh.instd4j.parser.exception;

public class PrivateUserAccountException extends LogicException {

    private final String username;

    public PrivateUserAccountException(String username) {
        super(null);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
