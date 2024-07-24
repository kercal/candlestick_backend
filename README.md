<<<<<<< HEAD
# Candlestick Aggregator

## Project Overview

This system is designed to help you view price histories in the form of minute candlesticks. The project receives updates from a partner service, transforms these updates, and provides the aggregated data through an easy-to-use API endpoint.

## Setup Instructions

### Prerequisites

Before you begin, make sure you have the following installed:

- [Java 8+](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html)
- [Gradle](https://gradle.org/install/)

### Steps to Set Up

1. **Build the Project**

   Next, build the project using Gradle:

   ```bash
   ./gradlew build

   ```

2. **Run the Partner Service**
   The project includes a partner service that provides the necessary data streams. Start this service by running:

   ```bash
   java -jar partner-service-1.0.1-all.jar --port 8032

   ```

3. **Run the Application**
   Run the Application:
   With the partner service running, you can now start the main application:

./gradlew run

## Running Tests and Usage

To make sure everything is working correctly, run the tests using:

./gradlew test

Once the application is running, you can retrieve candlestick data for any instrument by making a GET request to the following endpoint: http://localhost:9000/candlesticks?isin={ISIN}
=======
# candlestick_backend
>>>>>>> origin/main
