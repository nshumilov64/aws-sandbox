package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.TracingMode;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@LambdaHandler(lambdaName = "processor", roleName = "processor-role", tracingMode = TracingMode.Active)
@DependsOn(name = "Weather", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "table", value = "${target_table}")})
@LambdaUrlConfig
@SuppressWarnings("unchecked")
public class Processor implements RequestHandler<Object, String> {
    private static final URI SAMPLE_URI = URI.create("https://api.open-meteo.com/v1/forecast?latitude=52.52&" +
            "longitude=13.41&current=temperature_2m,wind_speed_10m&" +
            "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m");
    private static final Set<String> FORECAST_FIELDS_TO_KEEP = Set.of("elevation", "generationtime_ms", "latitude",
            "longitude", "timezone", "timezone_abbreviation", "utc_offset_seconds");
    private static final Set<String> HOURLY_FIELDS_TO_KEEP = Set.of("temperature_2m", "time");
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    public String handleRequest(Object request, Context context) {
        context.getLogger().log("Region: " + System.getenv("region"));
        context.getLogger().log("Bucket: " + System.getenv("target_bucket"));
        try {
            Map<String, Object> forecast = fetchForecast();
            Map<String, Object> dbEntry = createDbEntry(forecast);
            context.getLogger().log("DB Entry: " + gson.toJson(dbEntry));
            persist(dbEntry);
        } catch (Exception e) {
            context.getLogger().log(exceptionToString(e));
            return "ERROR";
        }
        return "OK";
    }

    private Map<String, Object> createDbEntry(Map<String, Object> forecast) {
        Map<String, Object> dbEntry = new HashMap<>();
        dbEntry.put("id", UUID.randomUUID().toString());
        Map<String, Object> adjustedForecast = forecast.entrySet().stream()
                .filter(e -> FORECAST_FIELDS_TO_KEEP.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> adjustedHourly = ((Map<String, Object>) forecast.get("hourly")).entrySet().stream()
                .filter(e -> HOURLY_FIELDS_TO_KEEP.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        adjustedForecast.put("hourly", adjustedHourly);
        Map<String, Object> adjustedHourlyUnits = ((Map<String, Object>) forecast.get("hourly_units")).entrySet().stream()
                .filter(e -> HOURLY_FIELDS_TO_KEEP.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        adjustedForecast.put("hourly_units", adjustedHourlyUnits);
        dbEntry.put("forecast", adjustedForecast);
        return dbEntry;
    }

    private void persist(Map<String, Object> dbEntry) {
        AmazonDynamoDBClientBuilder.standard()
                .withRegion(System.getenv("region"))
                .build()
                .putItem(System.getenv("table"), ItemUtils.fromSimpleMap(dbEntry));
    }

    private Map<String, Object> fetchForecast() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(SAMPLE_URI)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(response.body(), HashMap.class);
    }

    private String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Exception occurred: " + e.getMessage());
        pw.println("Stack trace:");
        e.printStackTrace(pw);
        return sw.toString();
    }
}
