package me.qyh.instd4j.parser.exception;

public class UserNotFoundException extends LogicException {

    private final String username;

    public UserNotFoundException(String username) {
        super(null);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
