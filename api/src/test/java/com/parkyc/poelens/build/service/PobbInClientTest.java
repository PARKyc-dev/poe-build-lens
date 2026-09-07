package com.parkyc.poelens.build.service;

import com.parkyc.poelens.config.exception.PoeLensException;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PobbInClientTest {
    @Test
    void fetchesRawEndpointForValidatedShareId() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("eNrawPobCode");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        PobbInClient client = new PobbInClient(httpClient);

        assertThat(client.fetch("AbC_123-xy")).isEqualTo("eNrawPobCode");
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().uri()).hasToString("https://pobb.in/AbC_123-xy/raw");
    }

    @Test
    void rejectsInvalidShareIdWithoutRequestingUpstream() {
        HttpClient httpClient = mock(HttpClient.class);
        PobbInClient client = new PobbInClient(httpClient);

        assertThatThrownBy(() -> client.fetch("../other-host"))
                .isInstanceOf(PoeLensException.class)
                .hasMessage("올바른 pobb.in 공유 링크를 입력해 주세요.");
    }

    @Test
    void mapsUpstreamFailureToPoeLensError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        PobbInClient client = new PobbInClient(httpClient);

        assertThatThrownBy(() -> client.fetch("not-found"))
                .isInstanceOf(PoeLensException.class)
                .hasMessage("pobb.in 빌드 코드를 불러올 수 없습니다.");
    }
}
