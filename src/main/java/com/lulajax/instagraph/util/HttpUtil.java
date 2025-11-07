package com.lulajax.instagraph.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Objects;

public class HttpUtil {

    private static final OkHttpClient client = new OkHttpClient();

    public static HttpRequest createGet(String url) {
        return new HttpRequest(url);
    }

    public static class HttpRequest {
        private final Request.Builder requestBuilder;

        public HttpRequest(String url) {
            this.requestBuilder = new Request.Builder().url(url);
        }

        public HttpRequest header(String name, String value) {
            requestBuilder.header(name, value);
            return this;
        }

        public HttpResponse execute() {
            try {
                Response response = client.newCall(requestBuilder.build()).execute();
                return new HttpResponse(response);
            } catch (IOException e) {
                throw new RuntimeException("HTTP request failed", e);
            }
        }
    }

    public static class HttpResponse {
        private final Response response;

        public HttpResponse(Response response) {
            this.response = response;
        }

        public String body() {
            try {
                ResponseBody body = response.body();
                return Objects.requireNonNull(body).string();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read response body", e);
            }
        }
    }
}
