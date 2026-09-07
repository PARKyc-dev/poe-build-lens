package com.parkyc.poelens.build.service;

import com.parkyc.poelens.config.exception.PoeLensException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.parkyc.poelens.common.code.ErrorCode.INVALID_POBB_IN_LINK;
import static com.parkyc.poelens.common.code.ErrorCode.POBB_IN_FETCH_FAILED;

@Component
public class PobbInClient {
    private static final String VALID_ID = "[A-Za-z0-9_-]+";
    private final HttpClient client;

    public PobbInClient() {
        this(HttpClient.newHttpClient());
    }

    PobbInClient(HttpClient client) {
        this.client = client;
    }

    public String fetch(String id) {
        if (id == null || !id.matches(VALID_ID)) {
            throw new PoeLensException(INVALID_POBB_IN_LINK);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://pobb.in/" + id + "/raw"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                throw new PoeLensException(POBB_IN_FETCH_FAILED);
            }
            return response.body().trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PoeLensException(POBB_IN_FETCH_FAILED);
        } catch (IOException exception) {
            throw new PoeLensException(POBB_IN_FETCH_FAILED);
        }
    }
}
