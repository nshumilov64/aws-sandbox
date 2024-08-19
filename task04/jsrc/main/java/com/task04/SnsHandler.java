package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.RegionScope;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(lambdaName = "sns_handler",
	roleName = "sns_handler-role",
	isPublishVersion = true,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DependsOn(
		name = "lambda_topic",
		resourceType = ResourceType.SNS_TOPIC
)
@SnsEventSource(
		targetTopic = "lambda_topic",
		regionScope = RegionScope.DEFAULT
)
public class SnsHandler implements RequestHandler<SNSEvent, String> {
	@Override
	public String handleRequest(SNSEvent event, Context context) {
        event.getRecords().forEach(record -> context.getLogger().log(record.getSNS().getMessage()));
		return "OK";
	}
}
