package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenMeteoClient {
    private static final URI SAMPLE_URI = URI.create("https://api.open-meteo.com/v1/forecast?latitude=52.52&" +
            "longitude=13.41&current=temperature_2m,wind_speed_10m&" +
            "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m");
    private static final HttpClient client = HttpClient.newHttpClient();

    public static HttpResponse<String> getSampleForecast() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(SAMPLE_URI)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}