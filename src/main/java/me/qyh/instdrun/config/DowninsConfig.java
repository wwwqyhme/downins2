package me.qyh.instdrun.config;

import me.qyh.instd4j.config.*;
import me.qyh.instd4j.parser.auth.AuthenticationProvider;
import me.qyh.instd4j.parser.auth.CookieStorePersistence;
import me.qyh.instd4j.parser.auth.DefaultAuthenticationProvider;
import me.qyh.instd4j.parser.auth.ScannerTwoFactorCodeProvider;
import org.apache.http.impl.client.BasicCookieStore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class DowninsConfig implements Config {
    private String location;
    private int threadNum;
    private String proxyAddr;
    private Integer proxyPort;
    private String username;
    private String password;
    private String storyQueryHash;
    private String channelQueryHash;
    private String userQueryHash;
    private String tagQueryHash;
    private String highlightStoriesQueryHash;

    private Integer connTimeout;
    private Integer getConnTimeout;
    private Integer socketTimeout;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public String getProxyAddr() {
        return proxyAddr;
    }

    public void setProxyAddr(String proxyAddr) {
        this.proxyAddr = proxyAddr;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setStoryQueryHash(String queryHash) {
        this.storyQueryHash = queryHash;
    }

    public String getStoryQueryHash() {
        return this.storyQueryHash;
    }

    public String getChannelQueryHash() {
        return channelQueryHash;
    }

    public void setChannelQueryHash(String channelQueryHash) {
        this.channelQueryHash = channelQueryHash;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getHighlightStoriesQueryHash() {
        return highlightStoriesQueryHash;
    }

    public void setHighlightStoriesQueryHash(String highlightStoriesQueryHash) {
        this.highlightStoriesQueryHash = highlightStoriesQueryHash;
    }

    @Override
    public int getSleepMill() {
        return 1000;
    }

    @Override
    public HttpConfig getHttpConfig() {
        return new HttpConfig() {
            @Override
            public int getMaxConnections() {
                return 20;
            }

            @Override
            public int getConnectTimeout() {
                return connTimeout * 1000;
            }

            @Override
            public int getSocketTimeout() {
                return socketTimeout * 1000;
            }

            @Override
            public int getConnectionRequestTimeout() {
                return getConnTimeout * 1000;
            }

            @Override
            public ProxyConfig getProxyConfig() {
                return new DefaultProxyConfig(proxyAddr, proxyPort);
            }
        };
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        final Path dir = Paths.get(System.getProperty("user.home")).resolve("downins")
                .resolve("auth");
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return new DefaultAuthenticationProvider(username, password, new ScannerTwoFactorCodeProvider(), new CookieStorePersistence() {
            @Override
            public void store(String username, BasicCookieStore cookieStore) {
                Path file = dir.resolve(username);
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (ObjectOutputStream outputStream = new ObjectOutputStream(buffer)) {
                    outputStream.writeObject(cookieStore);
                    final byte[] raw = buffer.toByteArray();
                    Files.write(file, raw);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public Optional<BasicCookieStore> loadCookieStore(String username) {
                Path file = dir.resolve(username);
                if (Files.exists(file)) {
                    try {
                        byte[] raw = Files.readAllBytes(file);
                        final ByteArrayInputStream buffer = new ByteArrayInputStream(raw);
                        try (ObjectInputStream inputStream = new ObjectInputStream(buffer)) {
                            return Optional.of((BasicCookieStore) inputStream.readObject());
                        } catch (ClassNotFoundException ignored) {
                        }
                    } catch (IOException ignored) {

                    }
                }
                return Optional.empty();
            }
        });
    }

    @Override
    public QueryHashLoader getQueryHashLoader() {
        return new DefaultQueryHashLoader(channelQueryHash, tagQueryHash, storyQueryHash, userQueryHash, highlightStoriesQueryHash);
    }

    public void setUserQueryHash(String userQueryHash) {
        this.userQueryHash = userQueryHash;
    }

    public String getTagQueryHash() {
        return tagQueryHash;
    }

    public void setTagQueryHash(String tagQueryHash) {
        this.tagQueryHash = tagQueryHash;
    }

    public String getUserQueryHash() {
        return userQueryHash;
    }

    public Path getDownloadDir() {
        Path dir = Paths.get(location == null ? System.getProperty("user.home") + "/downins" : location);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir;
    }


    public void store() {
        try {
            Configure.get().store(this);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Integer getConnTimeout() {
        return connTimeout;
    }

    public void setConnTimeout(Integer connTimeout) {
        this.connTimeout = connTimeout;
    }

    public Integer getGetConnTimeout() {
        return getConnTimeout;
    }

    public void setGetConnTimeout(Integer getConnTimeout) {
        this.getConnTimeout = getConnTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }


    @Override
    public String toString() {
        return "DowninsConfig{" +
                "location='" + location + '\'' +
                ", threadNum=" + threadNum +
                ", proxyAddr='" + proxyAddr + '\'' +
                ", proxyPort=" + proxyPort +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", storyQueryHash='" + storyQueryHash + '\'' +
                ", channelQueryHash='" + channelQueryHash + '\'' +
                ", userQueryHash='" + userQueryHash + '\'' +
                ", tagQueryHash='" + tagQueryHash + '\'' +
                ", highlightStoriesQueryHash='" + highlightStoriesQueryHash + '\'' +
                ", connTimeout=" + connTimeout +
                ", getConnTimeout=" + getConnTimeout +
                ", socketTimeout=" + socketTimeout +
                '}';
    }
}
