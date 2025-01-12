package org.zalando.logbook.httpclient;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.core.DefaultHttpLogFormatter;
import org.zalando.logbook.core.DefaultSink;

import java.io.IOException;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.PUT;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class LogbookHttpInterceptorsPutTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final HttpLogWriter writer = mock(HttpLogWriter.class);

    @BeforeEach
    void defaultBehaviour() {
        when(writer.isActive()).thenReturn(true);
    }

    private final Logbook logbook = Logbook.builder()
            .sink(new DefaultSink(new DefaultHttpLogFormatter(), writer))
            .build();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .addInterceptorFirst(new LogbookHttpRequestInterceptor(logbook))
            .addInterceptorFirst(new LogbookHttpResponseInterceptor(false))
            .build();

    @AfterEach
    void stop() throws IOException {
        client.close();
    }

    @Test
    void shouldLogRequestWithoutBody() throws IOException {
        driver.addExpectation(onRequestTo("/").withMethod(PUT), giveEmptyResponse());

        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello, world!", "text/plain"));

        final HttpPut request = new HttpPut(driver.getBaseUrl());

        client.execute(request);

        final String message = captureRequest();

        assertThat(message)
                .startsWith("Outgoing Request:")
                .contains(format("PUT http://localhost:%d HTTP/1.1", driver.getPort()))
                .doesNotContain("Content-Type")
                .doesNotContain("Hello, world!");
    }

    private String captureRequest() throws IOException {
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(any(Precorrelation.class), captor.capture());
        return captor.getValue();
    }

}
