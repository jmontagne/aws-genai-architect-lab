#!/usr/bin/env python3
"""Seed DynamoDB flights table with sample data."""

import argparse
import boto3
from decimal import Decimal


FLIGHTS = [
    # WAW -> CDG (Warsaw to Paris)
    {"route": "WAW#CDG", "flightId": "LO335", "date": "2025-03-15", "airline": "LOT Polish Airlines",
     "departureTime": "06:45", "arrivalTime": "09:10", "aircraft": "Boeing 737-800",
     "price": Decimal("450"), "availableSeats": 42, "baggageIncluded": True},
    {"route": "WAW#CDG", "flightId": "AF1145", "date": "2025-03-15", "airline": "Air France",
     "departureTime": "08:30", "arrivalTime": "10:45", "aircraft": "Airbus A320",
     "price": Decimal("380"), "availableSeats": 23, "baggageIncluded": True},
    {"route": "WAW#CDG", "flightId": "LH1234", "date": "2025-03-15", "airline": "Lufthansa",
     "departureTime": "14:20", "arrivalTime": "16:35", "aircraft": "Airbus A319",
     "price": Decimal("520"), "availableSeats": 8, "baggageIncluded": True},
    {"route": "WAW#CDG", "flightId": "LO337", "date": "2025-03-16", "airline": "LOT Polish Airlines",
     "departureTime": "07:00", "arrivalTime": "09:25", "aircraft": "Embraer E195",
     "price": Decimal("410"), "availableSeats": 35, "baggageIncluded": True},
    {"route": "WAW#CDG", "flightId": "AF1147", "date": "2025-03-16", "airline": "Air France",
     "departureTime": "12:15", "arrivalTime": "14:30", "aircraft": "Airbus A320",
     "price": Decimal("395"), "availableSeats": 18, "baggageIncluded": True},
    {"route": "WAW#CDG", "flightId": "LO339", "date": "2025-03-17", "airline": "LOT Polish Airlines",
     "departureTime": "06:45", "arrivalTime": "09:10", "aircraft": "Boeing 737-800",
     "price": Decimal("430"), "availableSeats": 50, "baggageIncluded": True},

    # WAW -> FCO (Warsaw to Rome)
    {"route": "WAW#FCO", "flightId": "LO521", "date": "2025-03-15", "airline": "LOT Polish Airlines",
     "departureTime": "09:00", "arrivalTime": "11:45", "aircraft": "Boeing 737-MAX 8",
     "price": Decimal("490"), "availableSeats": 30, "baggageIncluded": True},
    {"route": "WAW#FCO", "flightId": "AZ601", "date": "2025-03-15", "airline": "ITA Airways",
     "departureTime": "13:30", "arrivalTime": "16:15", "aircraft": "Airbus A320neo",
     "price": Decimal("420"), "availableSeats": 15, "baggageIncluded": True},
    {"route": "WAW#FCO", "flightId": "FR8821", "date": "2025-03-16", "airline": "Ryanair",
     "departureTime": "06:00", "arrivalTime": "08:50", "aircraft": "Boeing 737-800",
     "price": Decimal("120"), "availableSeats": 60, "baggageIncluded": False},
    {"route": "WAW#FCO", "flightId": "LO523", "date": "2025-03-17", "airline": "LOT Polish Airlines",
     "departureTime": "10:15", "arrivalTime": "13:00", "aircraft": "Embraer E195",
     "price": Decimal("470"), "availableSeats": 25, "baggageIncluded": True},

    # JFK -> LHR (New York to London)
    {"route": "JFK#LHR", "flightId": "BA178", "date": "2025-03-15", "airline": "British Airways",
     "departureTime": "19:00", "arrivalTime": "07:15", "aircraft": "Boeing 777-300ER",
     "price": Decimal("890"), "availableSeats": 45, "baggageIncluded": True},
    {"route": "JFK#LHR", "flightId": "VS4", "date": "2025-03-15", "airline": "Virgin Atlantic",
     "departureTime": "21:30", "arrivalTime": "09:45", "aircraft": "Airbus A350-1000",
     "price": Decimal("820"), "availableSeats": 32, "baggageIncluded": True},
    {"route": "JFK#LHR", "flightId": "AA100", "date": "2025-03-16", "airline": "American Airlines",
     "departureTime": "18:00", "arrivalTime": "06:10", "aircraft": "Boeing 777-200ER",
     "price": Decimal("950"), "availableSeats": 12, "baggageIncluded": True},
    {"route": "JFK#LHR", "flightId": "BA180", "date": "2025-03-17", "airline": "British Airways",
     "departureTime": "22:00", "arrivalTime": "10:15", "aircraft": "Airbus A380",
     "price": Decimal("870"), "availableSeats": 55, "baggageIncluded": True},

    # CDG -> JFK (Paris to New York)
    {"route": "CDG#JFK", "flightId": "AF22", "date": "2025-03-15", "airline": "Air France",
     "departureTime": "10:30", "arrivalTime": "13:15", "aircraft": "Boeing 777-300ER",
     "price": Decimal("780"), "availableSeats": 38, "baggageIncluded": True},
    {"route": "CDG#JFK", "flightId": "DL263", "date": "2025-03-15", "airline": "Delta Air Lines",
     "departureTime": "14:00", "arrivalTime": "16:45", "aircraft": "Airbus A330-900neo",
     "price": Decimal("720"), "availableSeats": 22, "baggageIncluded": True},
    {"route": "CDG#JFK", "flightId": "AF24", "date": "2025-03-16", "airline": "Air France",
     "departureTime": "09:00", "arrivalTime": "11:45", "aircraft": "Airbus A350-900",
     "price": Decimal("810"), "availableSeats": 40, "baggageIncluded": True},
    {"route": "CDG#JFK", "flightId": "UA57", "date": "2025-03-17", "airline": "United Airlines",
     "departureTime": "11:30", "arrivalTime": "14:15", "aircraft": "Boeing 767-400ER",
     "price": Decimal("690"), "availableSeats": 28, "baggageIncluded": True},
]


def seed_table(table_name: str, region: str) -> None:
    dynamodb = boto3.resource("dynamodb", region_name=region)
    table = dynamodb.Table(table_name)

    with table.batch_writer() as batch:
        for flight in FLIGHTS:
            batch.put_item(Item=flight)
            print(f"  Added: {flight['flightId']} ({flight['route']}, {flight['date']})")

    print(f"\nSeeded {len(FLIGHTS)} flights into table '{table_name}'")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed DynamoDB flights table")
    parser.add_argument("--table", required=True, help="DynamoDB table name")
    parser.add_argument("--region", default="us-east-1", help="AWS region")
    args = parser.parse_args()

    seed_table(args.table, args.region)
