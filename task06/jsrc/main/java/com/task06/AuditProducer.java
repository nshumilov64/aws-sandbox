package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@LambdaHandler(lambdaName = "audit_producer", roleName = "audit_producer-role")
@DependsOn(name = "Configuration", resourceType = ResourceType.DYNAMODB_TABLE)
@DependsOn(name = "Audit", resourceType = ResourceType.DYNAMODB_TABLE)
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 10)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "table", value = "${target_table}")
})
public class AuditProducer implements RequestHandler<DynamodbEvent, String> {
    public static final String INSERT = "INSERT";
    public static final String MODIFY = "MODIFY";

    public String handleRequest(DynamodbEvent event, Context context) {
        List<Item> items = event.getRecords().stream()
                .map(this::createItem)
                .collect(Collectors.toList());
        saveItems(items);
        return "OK";
    }

    private Item createItem(DynamodbEvent.DynamodbStreamRecord record) {
        String key = record.getDynamodb().getNewImage().get("key").getS();
        Integer newValue = Integer.valueOf(record.getDynamodb().getNewImage().get("value").getN());
        String modificationTime = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        switch (record.getEventName()) {
            case INSERT:
                return new Item()
                        .with("id", UUID.randomUUID().toString())
                        .with("itemKey", key)
                        .with("modificationTime", modificationTime)
                        .with("newValue", Map.of("key", key, "value", newValue));
            case MODIFY:
                Integer oldValue = Integer.valueOf(record.getDynamodb().getOldImage().get("value").getN());
                return new Item().with("id", UUID.randomUUID().toString())
                        .with("itemKey", key)
                        .with("modificationTime", modificationTime)
                        .with("updatedAttribute", "value")
                        .with("oldValue", oldValue)
                        .with("newValue", newValue);
            default:
                throw new IllegalArgumentException("Unknown event type: " + record.getEventName());
        }
    }

    private void saveItems(List<Item> items) {
        AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(System.getenv("region"))
                .build();
        List<WriteRequest> requests = items.stream()
                .map(e -> new WriteRequest().withPutRequest(new PutRequest(ItemUtils.toAttributeValues(e))))
                .collect(Collectors.toList());
        dynamoDB.batchWriteItem(Map.of(System.getenv("table"), requests));
    }
}
