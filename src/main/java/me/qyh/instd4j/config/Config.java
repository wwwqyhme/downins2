package me.qyh.instd4j.config;

import me.qyh.instd4j.parser.auth.AuthenticationProvider;

public interface Config {

    AuthenticationProvider getAuthenticationProvider();

    int getSleepMill();

    HttpConfig getHttpConfig();

    QueryHashLoader getQueryHashLoader();
}
