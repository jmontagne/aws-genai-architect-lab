package com.awslab.agent.model;

public record Flight(
        String route,
        String flightId,
        String date,
        String airline,
        String departureTime,
        String arrivalTime,
        String aircraft,
        double price,
        int availableSeats,
        boolean baggageIncluded
) {}
