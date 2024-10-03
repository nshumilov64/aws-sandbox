package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import org.example.OpenMeteoClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@LambdaHandler(lambdaName = "api_handler", roleName = "api_handler-role", layers = {"sdk_layer"})
@LambdaLayer(layerName = "sdk_layer", libraries = {"layer/open-meteo-client-0.1.jar"})
@LambdaUrlConfig
public class ApiHandler implements RequestHandler<Object, String> {
    public String handleRequest(Object request, Context context) {
        try {
            return OpenMeteoClient.getSampleForecast().body();
        } catch (IOException | InterruptedException e) {
            context.getLogger().log(exceptionToString(e));
            return e.getMessage();
        }
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
