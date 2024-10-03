package org.example;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpResponse;

public class ClientTest {
    @Test
    public void shouldFetchSampleForecast() throws IOException, InterruptedException {
        HttpResponse<String> response = OpenMeteoClient.getSampleForecast();
        Assert.assertEquals(200, response.statusCode());
        Assert.assertFalse(response.body().isEmpty());
    }
}
