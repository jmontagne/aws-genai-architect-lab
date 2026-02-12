package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class FlightTool {

    private static final Logger log = LoggerFactory.getLogger(FlightTool.class);

    private final DynamoDbAsyncClient dynamoDbClient;
    private final AgentProperties properties;

    public FlightTool(DynamoDbAsyncClient dynamoDbClient, AgentProperties properties) {
        this.dynamoDbClient = dynamoDbClient;
        this.properties = properties;
    }

    public CompletableFuture<List<Flight>> searchFlights(String origin, String destination, String date) {
        String route = origin + "#" + destination;
        log.debug("Searching flights: route={}, date={}", route, date);

        QueryRequest request = QueryRequest.builder()
                .tableName(properties.getTableName())
                .indexName("date-index")
                .keyConditionExpression("#route = :route AND #date = :date")
                .expressionAttributeNames(Map.of(
                        "#route", "route",
                        "#date", "date"))
                .expressionAttributeValues(Map.of(
                        ":route", AttributeValue.fromS(route),
                        ":date", AttributeValue.fromS(date)))
                .build();

        return dynamoDbClient.query(request)
                .thenApply(response -> {
                    List<Flight> flights = response.items().stream()
                            .map(this::toFlight)
                            .toList();
                    log.debug("Found {} flights for route={}, date={}", flights.size(), route, date);
                    return flights;
                })
                .exceptionally(ex -> {
                    log.error("DynamoDB query failed for route={}, date={}", route, date, ex);
                    throw new AgentException(AgentException.ErrorCode.DYNAMODB_QUERY_FAILED,
                            "Failed to search flights: " + ex.getMessage(), ex);
                });
    }

    public CompletableFuture<List<Flight>> getFlightDetails(String flightId) {
        log.debug("Getting flight details: flightId={}", flightId);

        ScanRequest request = ScanRequest.builder()
                .tableName(properties.getTableName())
                .filterExpression("flightId = :flightId")
                .expressionAttributeValues(Map.of(
                        ":flightId", AttributeValue.fromS(flightId)))
                .build();

        return dynamoDbClient.scan(request)
                .thenApply(response -> {
                    List<Flight> flights = response.items().stream()
                            .map(this::toFlight)
                            .toList();
                    log.debug("Found {} results for flightId={}", flights.size(), flightId);
                    return flights;
                })
                .exceptionally(ex -> {
                    log.error("DynamoDB scan failed for flightId={}", flightId, ex);
                    throw new AgentException(AgentException.ErrorCode.DYNAMODB_QUERY_FAILED,
                            "Failed to get flight details: " + ex.getMessage(), ex);
                });
    }

    private Flight toFlight(Map<String, AttributeValue> item) {
        return new Flight(
                getStringAttr(item, "route"),
                getStringAttr(item, "flightId"),
                getStringAttr(item, "date"),
                getStringAttr(item, "airline"),
                getStringAttr(item, "departureTime"),
                getStringAttr(item, "arrivalTime"),
                getStringAttr(item, "aircraft"),
                getNumberAttr(item, "price"),
                (int) getNumberAttr(item, "availableSeats"),
                getBoolAttr(item, "baggageIncluded")
        );
    }

    private String getStringAttr(Map<String, AttributeValue> item, String key) {
        AttributeValue val = item.get(key);
        return val != null ? val.s() : "";
    }

    private double getNumberAttr(Map<String, AttributeValue> item, String key) {
        AttributeValue val = item.get(key);
        return val != null && val.n() != null ? Double.parseDouble(val.n()) : 0.0;
    }

    private boolean getBoolAttr(Map<String, AttributeValue> item, String key) {
        AttributeValue val = item.get(key);
        return val != null && val.bool() != null && val.bool();
    }
}
