package me.qyh.instd4j.config;

import me.qyh.instd4j.parser.auth.AuthenticationProvider;
import me.qyh.instd4j.parser.auth.CookieStorePersistence;
import me.qyh.instd4j.parser.auth.DefaultAuthenticationProvider;
import me.qyh.instd4j.parser.auth.TwoFactorCodeProvider;

public class ConfigBuilder {

    private AuthenticationProvider authenticationProvider;
    private QueryHashLoader queryHashLoader;
    private int sleepMill = 1000;
    private HttpConfig httpConfig;
    private final DefaultHttpConfig defaultHttpConfig = new DefaultHttpConfig();

    private ConfigBuilder() {
        super();
    }

    public static ConfigBuilder create() {
        return new ConfigBuilder();
    }

    public ConfigBuilder httpConfig(HttpConfig httpConfig) {
        this.httpConfig = httpConfig;
        return this;
    }

    public ConfigBuilder authenticationProvider(String username, String password, TwoFactorCodeProvider twoFactorCodeProvider, CookieStorePersistence persistence) {
        this.authenticationProvider = new DefaultAuthenticationProvider(username, password, twoFactorCodeProvider,persistence);
        return this;
    }

    public ConfigBuilder authenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
        return this;
    }

    public ConfigBuilder maxConnection(int maxConnections) {
        this.defaultHttpConfig.setMaxConnections(maxConnections);
        return this;
    }

    public ConfigBuilder proxyConfig(ProxyConfig config) {
        this.defaultHttpConfig.setProxyConfig(config);
        return this;
    }

    public ConfigBuilder proxy(String proxyAddr, Integer proxyPort) {
        this.defaultHttpConfig.setProxyConfig(new DefaultProxyConfig(proxyAddr, proxyPort));
        return this;
    }

    public ConfigBuilder queryHashLoader(QueryHashLoader queryHashLoader) {
        this.queryHashLoader = queryHashLoader;
        return this;
    }

    public ConfigBuilder sleepMill(int sleepMill) {
        this.sleepMill = sleepMill;
        return this;
    }

    public ConfigBuilder connectTimeout(int connectTimeout) {
        this.defaultHttpConfig.setConnectTimeout(connectTimeout);
        return this;
    }

    public ConfigBuilder socketTimeout(int socketTimeout) {
        this.defaultHttpConfig.setSocketTimeout(socketTimeout);
        return this;
    }

    public ConfigBuilder connectionRequestTimeout(int connectionRequestTimeout) {
        this.defaultHttpConfig.setConnectionRequestTimeout(connectionRequestTimeout);
        return this;
    }

    public Config build() {
        return new Config() {

            @Override
            public AuthenticationProvider getAuthenticationProvider() {
                return authenticationProvider;
            }

            @Override
            public int getSleepMill() {
                return sleepMill;
            }

            @Override
            public HttpConfig getHttpConfig() {
                return httpConfig == null ? defaultHttpConfig : httpConfig;
            }

            @Override
            public QueryHashLoader getQueryHashLoader() {
                return queryHashLoader == null ? new DefaultQueryHashLoader() : queryHashLoader;
            }
        };
    }


}
