package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = true,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "table", value = "${target_table}")})
public class ApiHandler implements RequestHandler<ApiHandler.Request, APIGatewayV2HTTPResponse> {
    static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayV2HTTPResponse handleRequest(Request request, Context context) {
        context.getLogger().log("region: " + System.getenv("region"));
        context.getLogger().log("target_table: " + System.getenv("table"));
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        context.getLogger().log("timestamp: " + now);
        try {
            context.getLogger().log("request: " + objectMapper.writeValueAsString(request));
            // Creating and storing the Event
            Event event = new Event(UUID.randomUUID().toString(), request.getPrincipalId(), now, request.getContent());
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(System.getenv("region"))
                    .build();
            DynamoDBMapper mapper = new DynamoDBMapper(client);
            DynamoDBMapperConfig mapperConfig = new DynamoDBMapperConfig.Builder()
                    .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride
                            .withTableNameReplacement(System.getenv("table")))
                    .build();
            mapper.save(event, mapperConfig);
            // Formulating the response
            Response response = new Response(201, event);
            return APIGatewayV2HTTPResponse.builder().withStatusCode(201)
                    .withBody(objectMapper.writeValueAsString(response)).build();
        } catch (Exception e) {
            context.getLogger().log(exceptionToString(e));
            return APIGatewayV2HTTPResponse.builder().withStatusCode(500).withBody(e.getMessage()).build();
        }
    }

    private String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(e.getMessage());
        e.printStackTrace(pw);
        return sw.toString();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private int principalId;
        private Map<String, String> content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private int statusCode;
        private Event event;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @DynamoDBTable(tableName = "Events")
    public static class Event {
        @DynamoDBHashKey
        private String id;
        @DynamoDBAttribute
        private int principalId;
        @DynamoDBAttribute
        private String createdAt;
        @DynamoDBAttribute
        private Map<String, String> body;
    }
}
