package me.qyh.instd4j.parser;

import me.qyh.instd4j.http.HttpUtils;
import me.qyh.instd4j.util.JsonExecutor;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphqlQuery {

    private static final String URL = "https://www.instagram.com/graphql/query/";

    private final Map<String, String> headers = new HashMap<>();
    private final List<NameValuePair> pairs = new ArrayList<>();
    private final CloseableHttpClient client;

    public GraphqlQuery(CloseableHttpClient client) {
        this.client = client;
    }

    public static GraphqlQuery create(CloseableHttpClient client) {
        return new GraphqlQuery(client);
    }

    public GraphqlQuery queryHash(String hash) {
        this.pairs.add(new BasicNameValuePair("query_hash", hash));
        return this;
    }

    public GraphqlQuery variables(String variables) {
        this.pairs.add(new BasicNameValuePair("variables", variables));
        return this;
    }

    public GraphqlQuery addHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public GraphqlQuery addParameter(String name, String value) {
        this.pairs.add(new BasicNameValuePair(name, value));
        return this;
    }

    public GraphqlQuery referer(String url) {
        this.headers.put("referer", encodeUrl(url));
        return this;
    }

    public JsonExecutor execute() {
        HttpGet get = buildRequest();
        String content = HttpUtils.toString(client, get, null);
        JsonExecutor ee = new JsonExecutor(content);
        String status = ee.execute("status").getAsString();
        if ("ok".equals(status)) {
            return ee;
        }
        throw new RuntimeException("地址：" + get.getURI() + "返回错误内容：" + content);
    }

    private HttpGet buildRequest() {
        URI uri;
        try {
            URIBuilder builder = new URIBuilder(URL);
            builder.addParameters(pairs);
            uri = builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        HttpGet get = new HttpGet(uri);
        get.addHeader("x-requested-with", "XMLHttpRequest");
        for (Map.Entry<String, String> it : headers.entrySet()) {
            get.addHeader(it.getKey(), it.getValue());
        }
        return get;
    }

    private String encodeUrl(String urlString2Decode) {
        try {
            String decodedURL = URLDecoder.decode(urlString2Decode, StandardCharsets.UTF_8);
            java.net.URL url = new URL(decodedURL);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                    url.getQuery(), url.getRef());
            return uri.toASCIIString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
