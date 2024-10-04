package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import lombok.Data;
import lombok.Value;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID;

@LambdaHandler(lambdaName = "api_handler", roleName = "api_handler-role")
@DependsOn(name = "${booking_userpool}", resourceType = ResourceType.COGNITO_USER_POOL)
@DependsOn(name = "${tables_table}", resourceType = ResourceType.DYNAMODB_TABLE)
@DependsOn(name = "${reservations_table}", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "user_pool_id", value = "${booking_userpool}",
                valueTransformer = USER_POOL_NAME_TO_USER_POOL_ID),
        @EnvironmentVariable(key = "client_id", value = "${booking_userpool}",
                valueTransformer = USER_POOL_NAME_TO_CLIENT_ID)
})
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Gson gson = new Gson();
    private static final CognitoIdentityProviderClient cognito = CognitoIdentityProviderClient.create();
    // Dynamo API, don't know those are thread-safe and if it makes sense to reuse them
    private static final DynamoDbClient dynamo = DynamoDbClient.create();
    private static final DynamoDbEnhancedClient dynamoEnhanced = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamo).build();
    private static final DynamoDbTable<Table> tablesTable = dynamoEnhanced
            .table(System.getenv("tables_table"), TableSchema.fromBean(Table.class));
    private static final DynamoDbTable<Reservation> reservationsTable = dynamoEnhanced
            .table(System.getenv("reservations_table"), TableSchema.fromBean(Reservation.class));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        context.getLogger().log("System Environment: " + gson.toJson(System.getenv()));
        context.getLogger().log("Request Event: " + gson.toJson(requestEvent));
        try {
            APIGatewayProxyResponseEvent responseEvent = routeRequest(requestEvent);
            context.getLogger().log("Response Event: " + gson.toJson(responseEvent));
            return responseEvent;
        } catch (JsonParseException e) {
            context.getLogger().log("Error parsing request: " + e.getMessage());
            return badRequest(String.format("Unable to parse the body: %s", e.getMessage()));
        } catch (Exception e) {
            context.getLogger().log("Error handling request: " + e.getMessage());
            return genericResponse(500, String.format("Error: %s", e.getMessage()));
        }
    }

    private APIGatewayProxyResponseEvent routeRequest(APIGatewayProxyRequestEvent event) {
        String[] pathElements = event.getPath().substring(1).split("/");
        String method = event.getHttpMethod();
        switch (pathElements[0]) {
            case "signup":
                if (pathElements.length > 1) {
                    return mappingNotFound(event.getPath());
                }
                return method.equals("POST") ? processSignUp(gson.fromJson(event.getBody(), SignUp.class)) :
                        unsupportedMethod(event.getHttpMethod(), event.getPath());
            case "signin":
                if (pathElements.length > 1) {
                    return mappingNotFound(event.getPath());
                }
                return method.equals("POST") ? processSignIn(gson.fromJson(event.getBody(), SignIn.class)) :
                        unsupportedMethod(event.getHttpMethod(), event.getPath());
            case "tables":
                switch (pathElements.length) {
                    case 1:
                        switch (method) {
                            case "GET":
                                return processGetTables();
                            case "POST":
                                return processPostTable(gson.fromJson(event.getBody(), Table.class));
                            default:
                                unsupportedMethod(event.getHttpMethod(), event.getPath());
                        }
                        break;
                    case 2:
                        return method.equals("GET") ? processGetTable(pathElements[1]) :
                                unsupportedMethod(event.getHttpMethod(), event.getPath());
                    default:
                        return mappingNotFound(event.getPath());
                }
            case "reservations":
                if (pathElements.length > 1) {
                    return mappingNotFound(event.getPath());
                }
                switch (method) {
                    case "GET":
                        return processGetReservations();
                    case "POST":
                        return processPostReservation(gson.fromJson(event.getBody(), Reservation.class));
                    default:
                        return unsupportedMethod(event.getHttpMethod(), event.getPath());
                }
            default:
                return mappingNotFound(event.getPath());
        }
    }

    private APIGatewayProxyResponseEvent genericResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(statusCode).withBody(body);
    }

    private APIGatewayProxyResponseEvent mappingNotFound(String path) {
        return genericResponse(404, String.format("No mapping found for %s", path));
    }

    private APIGatewayProxyResponseEvent unsupportedMethod(String method, String path) {
        return genericResponse(405, String.format("Unsupported method %s for path %s", method, path));
    }

    private APIGatewayProxyResponseEvent badRequest(String body) {
        return genericResponse(400, body);
    }

    private APIGatewayProxyResponseEvent ok(String body) {
        return genericResponse(200, body);
    }

    private APIGatewayProxyResponseEvent processSignUp(SignUp signUp) {
        if (Validator.invalidEmail(signUp.getEmail()) || Validator.invalidPassword(signUp.getPassword())) {
            return badRequest("Invalid email or password");
        }
        String userPoolId = System.getenv("user_pool_id");
        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(signUp.getEmail())
                .messageAction(MessageActionType.SUPPRESS)
                .build();
        cognito.adminCreateUser(createUserRequest);
        AdminSetUserPasswordRequest setUserPasswordRequest = AdminSetUserPasswordRequest.builder()
                .password(signUp.getEmail())
                .userPoolId(userPoolId)
                .username(signUp.getEmail())
                .permanent(true)
                .build();
        cognito.adminSetUserPassword(setUserPasswordRequest);
        return ok(null);
    }

    private APIGatewayProxyResponseEvent processSignIn(SignIn signIn) {
        if (Validator.invalidEmail(signIn.getEmail()) || Validator.invalidPassword(signIn.getPassword())) {
            return badRequest("Invalid email or password");
        }
        String userPoolId = System.getenv("user_pool_id");
        String clientId = System.getenv("client_id");
        // Recommended auth flows are very odd
        AdminInitiateAuthRequest request = AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authParameters(Map.of("USERNAME", signIn.getEmail(), "PASSWORD", signIn.getPassword()))
                .build();
        AdminInitiateAuthResponse response = cognito.adminInitiateAuth(request);
        // Need to provide an id token instead of advertised access token smh
        return ok(gson.toJson(new Tokens(response.authenticationResult().idToken())));
    }

    private APIGatewayProxyResponseEvent processGetTables() {
        List<Table> tables = tablesTable.scan().items().stream().collect(Collectors.toList());
        return ok(gson.toJson(new Tables(tables)));
    }

    private APIGatewayProxyResponseEvent processPostTable(Table table) {
        tablesTable.putItem(table);
        return ok((gson.toJson(new IdWrapper<>(table.getId()))));
    }

    private APIGatewayProxyResponseEvent processGetTable(String tableId) {
        try {
            Key tableKey = Key.builder().partitionValue(Integer.valueOf(tableId)).build();
            Table table = tablesTable.getItem(tableKey);
            if (table == null) {
                return genericResponse(404, String.format("Table with id %s not found", tableId));
            }
            return ok(gson.toJson(table));
        } catch (NumberFormatException e) {
            return badRequest("Invalid table id");
        }
    }

    private APIGatewayProxyResponseEvent processGetReservations() {
        List<Reservation> reservations = reservationsTable.scan().items().stream().collect(Collectors.toList());
        return ok(gson.toJson(new Reservations(reservations)));
    }

    private APIGatewayProxyResponseEvent processPostReservation(Reservation reservation) {
        if (Validator.invalidDate(reservation.getDate())) {
            return badRequest("Invalid date");
        }
        if (Validator.invalidTime(reservation.getSlotTimeStart()) ||
                Validator.invalidTime(reservation.getSlotTimeEnd()) ||
                reservation.getSlotTimeStart().compareTo(reservation.getSlotTimeEnd()) >= 0) {
            return badRequest("Invalid (slotStartTime, slotEndTime)");
        }
        // The following 2 checks should really be migrated to a DB-level query, but I can't be asked
        boolean nonExistingTable = tablesTable.scan().items().stream()
                .noneMatch(existingTable -> existingTable.getNumber() == reservation.getTableNumber());
        if (nonExistingTable) {
            return badRequest(String.format("Table with number %d not found", reservation.getTableNumber()));
        }
        boolean hasOverlapsWithExistingReservations = reservationsTable.scan().items().stream()
                .filter(e -> e.getTableNumber() == reservation.getTableNumber())
                .anyMatch(existing -> Validator.overlappingRanges(
                        existing.getSlotTimeStart(), existing.getSlotTimeEnd(),
                        reservation.getSlotTimeStart(), reservation.getSlotTimeEnd(),
                        // Comparing string values should suffice
                        Comparator.comparing(Function.identity())));
        if (hasOverlapsWithExistingReservations) {
            return badRequest("Overlaps with existing reservation");
        }
        reservation.setId(UUID.randomUUID().toString());
        reservationsTable.putItem(reservation);
        return ok(gson.toJson(new IdWrapper<>(reservation.getId())));
    }

    @Value
    public static class SignUp {
        String firstName;
        String lastName;
        String email;
        String password;
    }

    @Value
    public static class SignIn {
        String firstName;
        String lastName;
        String email;
        String password;
    }

    @Value
    public static class Tokens {
        String accessToken;
    }

    @Data
    @DynamoDbBean
    public static class Table {
        private int id;
        private int number;
        private int places;
        private boolean isVip;
        private int minOrder;

        @DynamoDbPartitionKey
        public int getId() {
            return id;
        }
    }

    @Value
    public static class Tables {
        List<Table> tables;
    }

    @Data
    @DynamoDbBean
    public static class Reservation {
        transient private String id;
        private int tableNumber;
        private String clientName;
        private String phoneNumber;
        private String date;
        private String slotTimeStart;
        private String slotTimeEnd;

        @DynamoDbPartitionKey
        public String getId() {
            return id;
        }
    }

    @Value
    public static class Reservations {
        List<Reservation> reservations;
    }

    @Value
    public static class IdWrapper<T> {
        T id;
    }
}
