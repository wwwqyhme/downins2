package me.qyh.instd4j.parser.exception;

public class PostNotFoundException extends LogicException {

    private final String shortcode;

    public PostNotFoundException(String shortcode) {
        super(null);
        this.shortcode = shortcode;
    }

    public String getShortcode() {
        return shortcode;
    }
}
