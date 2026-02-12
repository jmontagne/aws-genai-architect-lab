package com.awslab.agent.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.*;
import java.util.stream.Collectors;

public class FlightToolHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String tableName = System.getenv("TABLE_NAME");

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Received event: " + event);

        String actionGroup = (String) event.get("actionGroup");
        String apiPath = (String) event.get("apiPath");

        Map<String, String> parameters = extractParameters(event);

        String responseBody;
        int statusCode = 200;

        try {
            responseBody = switch (apiPath) {
                case "/searchFlights" -> searchFlights(parameters);
                case "/getFlightDetails" -> getFlightDetails(parameters);
                default -> {
                    statusCode = 400;
                    yield "{\"error\": \"Unknown action: " + apiPath + "\"}";
                }
            };
        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            statusCode = 500;
            responseBody = "{\"error\": \"" + e.getMessage() + "\"}";
        }

        return Map.of(
                "messageVersion", "1.0",
                "response", Map.of(
                        "actionGroup", actionGroup,
                        "apiPath", apiPath,
                        "httpMethod", "POST",
                        "httpStatusCode", statusCode,
                        "responseBody", Map.of(
                                "application/json", Map.of(
                                        "body", responseBody
                                )
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractParameters(Map<String, Object> event) {
        Map<String, String> params = new HashMap<>();

        try {
            Object requestBody = event.get("requestBody");
            if (requestBody instanceof Map) {
                Map<String, Object> body = (Map<String, Object>) requestBody;
                Object content = body.get("content");
                if (content instanceof Map) {
                    Map<String, Object> contentMap = (Map<String, Object>) content;
                    Object jsonContent = contentMap.get("application/json");
                    if (jsonContent instanceof Map) {
                        Map<String, Object> jsonMap = (Map<String, Object>) jsonContent;
                        Object properties = jsonMap.get("properties");
                        if (properties instanceof List) {
                            List<Map<String, String>> propList = (List<Map<String, String>>) properties;
                            for (Map<String, String> prop : propList) {
                                params.put(prop.get("name"), prop.get("value"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fall through with empty params
        }

        return params;
    }

    private String searchFlights(Map<String, String> parameters) {
        String origin = parameters.getOrDefault("origin", "");
        String destination = parameters.getOrDefault("destination", "");
        String date = parameters.getOrDefault("date", "");
        String route = origin + "#" + destination;

        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName("date-index")
                .keyConditionExpression("#route = :route AND #date = :date")
                .expressionAttributeNames(Map.of(
                        "#route", "route",
                        "#date", "date"))
                .expressionAttributeValues(Map.of(
                        ":route", AttributeValue.fromS(route),
                        ":date", AttributeValue.fromS(date)))
                .build();

        List<Map<String, String>> flights = dynamoDb.query(request).items().stream()
                .map(this::itemToMap)
                .collect(Collectors.toList());

        return toJson(flights);
    }

    private String getFlightDetails(Map<String, String> parameters) {
        String flightId = parameters.getOrDefault("flightId", "");

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("flightId = :flightId")
                .expressionAttributeValues(Map.of(
                        ":flightId", AttributeValue.fromS(flightId)))
                .build();

        List<Map<String, String>> flights = dynamoDb.scan(request).items().stream()
                .map(this::itemToMap)
                .collect(Collectors.toList());

        if (flights.isEmpty()) {
            return "{\"error\": \"Flight not found: " + flightId + "\"}";
        }

        return toJson(flights.get(0));
    }

    private Map<String, String> itemToMap(Map<String, AttributeValue> item) {
        Map<String, String> map = new LinkedHashMap<>();
        item.forEach((key, value) -> {
            if (value.s() != null) {
                map.put(key, value.s());
            } else if (value.n() != null) {
                map.put(key, value.n());
            } else if (value.bool() != null) {
                map.put(key, value.bool().toString());
            }
        });
        return map;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
