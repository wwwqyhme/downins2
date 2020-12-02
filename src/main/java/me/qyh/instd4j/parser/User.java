package me.qyh.instd4j.parser;

public class User {
    private final String username;
    private final String id;
    private final Link avatar;

    public User(String username, String id, Link avatar) {
        this.username = username;
        this.id = id;
        this.avatar = avatar;
    }

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public Link getAvatar() {
        return avatar;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", id='" + id + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}
