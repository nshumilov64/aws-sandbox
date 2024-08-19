package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;

@LambdaHandler(lambdaName = "sqs_handler", roleName = "sqs_handler-role")
@DependsOn(
        name = "async_queue",
        resourceType = ResourceType.SQS_QUEUE
)
@SqsTriggerEventSource(
        targetQueue = "async_queue",
        batchSize = 10    // Default?
)
public class SqsHandler implements RequestHandler<SQSEvent, String> {
    public String handleRequest(SQSEvent event, Context context) {
        event.getRecords().forEach(message -> context.getLogger().log(message.getBody()));
        return "OK";
    }
}
