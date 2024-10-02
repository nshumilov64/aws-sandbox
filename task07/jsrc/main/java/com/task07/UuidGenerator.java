package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@LambdaHandler(lambdaName = "uuid_generator", roleName = "uuid_generator-role")
@DependsOn(name = "uuid-storage", resourceType = ResourceType.S3_BUCKET)
@DependsOn(name = "uuid_trigger", resourceType = ResourceType.CLOUDWATCH_RULE)
@RuleEventSource(targetRule = "uuid_trigger")
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "bucket", value = "${target_bucket}")})
public class UuidGenerator implements RequestHandler<Object, String> {
    private static final Gson gson = new Gson();

    public String handleRequest(Object request, Context context) {
        context.getLogger().log("Region: " + System.getenv("region"));
        context.getLogger().log("Bucket: " + System.getenv("target_bucket"));
        context.getLogger().log("Request: " + request);
        Map<String, Object> output = Map.of("ids", IntStream.range(0, 10)
                .mapToObj(e -> UUID.randomUUID().toString())
                .collect(Collectors.toList()));
        try {
            AmazonS3Client.builder()
                    .withRegion(System.getenv("region"))
                    .build()
                    .putObject(System.getenv("bucket"),
                            ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
                            gson.toJson(output));
        } catch (Exception e) {
            context.getLogger().log(exceptionToString(e));
        }
        return "OK";
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
