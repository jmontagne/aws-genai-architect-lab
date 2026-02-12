package com.awslab.agent.model;

import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

import java.util.List;

public final class ToolDefinitions {

    private ToolDefinitions() {
    }

    public static Tool searchFlightsTool() {
        return Tool.builder()
                .toolSpec(ToolSpecification.builder()
                        .name("searchFlights")
                        .description("Search for available flights between two airports on a given date. "
                                + "Returns a list of matching flights with basic information including "
                                + "flight ID, airline, departure/arrival times, and price.")
                        .inputSchema(ToolInputSchema.builder()
                                .json(Document.mapBuilder()
                                        .putString("type", "object")
                                        .putDocument("properties", Document.mapBuilder()
                                                .putDocument("origin", Document.mapBuilder()
                                                        .putString("type", "string")
                                                        .putString("description",
                                                                "IATA airport code for departure (e.g., WAW, CDG, JFK)")
                                                        .build())
                                                .putDocument("destination", Document.mapBuilder()
                                                        .putString("type", "string")
                                                        .putString("description",
                                                                "IATA airport code for arrival")
                                                        .build())
                                                .putDocument("date", Document.mapBuilder()
                                                        .putString("type", "string")
                                                        .putString("description",
                                                                "Travel date in YYYY-MM-DD format")
                                                        .build())
                                                .build())
                                        .putList("required", List.of(
                                                Document.fromString("origin"),
                                                Document.fromString("destination"),
                                                Document.fromString("date")))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public static Tool getFlightDetailsTool() {
        return Tool.builder()
                .toolSpec(ToolSpecification.builder()
                        .name("getFlightDetails")
                        .description("Get detailed information about a specific flight by its ID. "
                                + "Returns full details including aircraft type, available seats, "
                                + "baggage policy, and pricing.")
                        .inputSchema(ToolInputSchema.builder()
                                .json(Document.mapBuilder()
                                        .putString("type", "object")
                                        .putDocument("properties", Document.mapBuilder()
                                                .putDocument("flightId", Document.mapBuilder()
                                                        .putString("type", "string")
                                                        .putString("description",
                                                                "Unique flight identifier (e.g., LO335)")
                                                        .build())
                                                .build())
                                        .putList("required", List.of(
                                                Document.fromString("flightId")))
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public static List<Tool> allTools() {
        return List.of(searchFlightsTool(), getFlightDetailsTool());
    }
}
