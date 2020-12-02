package me.qyh.instd4j.parser.auth;

import org.apache.http.impl.client.BasicCookieStore;

import java.util.Optional;

public interface CookieStorePersistence {

    void store(String username, BasicCookieStore cookieStore);

    Optional<BasicCookieStore> loadCookieStore(String username);
}
