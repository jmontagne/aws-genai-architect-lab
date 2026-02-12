package com.awslab.agent.service;

import com.awslab.agent.config.AgentProperties;
import com.awslab.agent.exception.AgentException;
import com.awslab.agent.model.Flight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightToolTest {

    @Mock
    private DynamoDbAsyncClient dynamoDbClient;

    @Mock
    private AgentProperties properties;

    @InjectMocks
    private FlightTool flightTool;

    @Test
    void searchFlights_happyPath_returnsFlights() {
        when(properties.getTableName()).thenReturn("test-flights");

        Map<String, AttributeValue> item = Map.of(
                "route", AttributeValue.fromS("WAW#CDG"),
                "flightId", AttributeValue.fromS("LO335"),
                "date", AttributeValue.fromS("2025-03-15"),
                "airline", AttributeValue.fromS("LOT Polish Airlines"),
                "departureTime", AttributeValue.fromS("06:45"),
                "arrivalTime", AttributeValue.fromS("09:10"),
                "aircraft", AttributeValue.fromS("Boeing 737-800"),
                "price", AttributeValue.fromN("450"),
                "availableSeats", AttributeValue.fromN("42"),
                "baggageIncluded", AttributeValue.fromBool(true)
        );

        QueryResponse response = QueryResponse.builder().items(List.of(item)).build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        List<Flight> flights = flightTool.searchFlights("WAW", "CDG", "2025-03-15").join();

        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).flightId()).isEqualTo("LO335");
        assertThat(flights.get(0).airline()).isEqualTo("LOT Polish Airlines");
        assertThat(flights.get(0).price()).isEqualTo(450.0);
        assertThat(flights.get(0).baggageIncluded()).isTrue();
    }

    @Test
    void searchFlights_emptyResults_returnsEmptyList() {
        when(properties.getTableName()).thenReturn("test-flights");

        QueryResponse response = QueryResponse.builder().items(List.of()).build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        List<Flight> flights = flightTool.searchFlights("WAW", "XYZ", "2025-03-15").join();

        assertThat(flights).isEmpty();
    }

    @Test
    void searchFlights_verifyKeyCondition() {
        when(properties.getTableName()).thenReturn("test-flights");

        QueryResponse response = QueryResponse.builder().items(List.of()).build();
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        flightTool.searchFlights("WAW", "CDG", "2025-03-15").join();

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(dynamoDbClient).query(captor.capture());

        QueryRequest captured = captor.getValue();
        assertThat(captured.tableName()).isEqualTo("test-flights");
        assertThat(captured.indexName()).isEqualTo("date-index");
        assertThat(captured.keyConditionExpression()).contains("#route = :route");
        assertThat(captured.expressionAttributeValues().get(":route").s()).isEqualTo("WAW#CDG");
        assertThat(captured.expressionAttributeValues().get(":date").s()).isEqualTo("2025-03-15");
    }

    @Test
    void searchFlights_sdkException_throwsAgentException() {
        when(properties.getTableName()).thenReturn("test-flights");

        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DynamoDB error")));

        assertThatThrownBy(() -> flightTool.searchFlights("WAW", "CDG", "2025-03-15").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AgentException.class);
    }

    @Test
    void getFlightDetails_happyPath_returnsFlight() {
        when(properties.getTableName()).thenReturn("test-flights");

        Map<String, AttributeValue> item = Map.of(
                "route", AttributeValue.fromS("WAW#CDG"),
                "flightId", AttributeValue.fromS("AF1145"),
                "date", AttributeValue.fromS("2025-03-15"),
                "airline", AttributeValue.fromS("Air France"),
                "departureTime", AttributeValue.fromS("08:30"),
                "arrivalTime", AttributeValue.fromS("10:45"),
                "aircraft", AttributeValue.fromS("Airbus A320"),
                "price", AttributeValue.fromN("380"),
                "availableSeats", AttributeValue.fromN("23"),
                "baggageIncluded", AttributeValue.fromBool(true)
        );

        ScanResponse response = ScanResponse.builder().items(List.of(item)).build();
        when(dynamoDbClient.scan(any(ScanRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        List<Flight> flights = flightTool.getFlightDetails("AF1145").join();

        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).flightId()).isEqualTo("AF1145");
        assertThat(flights.get(0).aircraft()).isEqualTo("Airbus A320");
        assertThat(flights.get(0).availableSeats()).isEqualTo(23);
    }

    @Test
    void getFlightDetails_notFound_returnsEmptyList() {
        when(properties.getTableName()).thenReturn("test-flights");

        ScanResponse response = ScanResponse.builder().items(List.of()).build();
        when(dynamoDbClient.scan(any(ScanRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        List<Flight> flights = flightTool.getFlightDetails("INVALID").join();

        assertThat(flights).isEmpty();
    }

    @Test
    void getFlightDetails_sdkException_throwsAgentException() {
        when(properties.getTableName()).thenReturn("test-flights");

        when(dynamoDbClient.scan(any(ScanRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("DynamoDB error")));

        assertThatThrownBy(() -> flightTool.getFlightDetails("AF1145").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AgentException.class);
    }
}
