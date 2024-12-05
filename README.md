# Log Management System

## Overview
The Log Management System is a comprehensive Java-based application designed to collect, process, and visualize logs from various sources. It leverages Elasticsearch for efficient log storage and retrieval, providing a robust web interface for real-time monitoring and analysis of log data. This system is ideal for developers and system administrators who need to track application performance, troubleshoot issues, and gain insights from log data.

## Features
- **Log Collection**: Fetch logs from Elasticsearch based on specified indices, allowing for flexible querying.
- **Real-time Monitoring**: Visualize log data using interactive charts and graphs, enabling quick insights into application behavior.
- **WebSocket Integration**: Stream logs to the client in real-time, ensuring that users always have the latest data.
- **Periodic Fetching**: Automatically fetch and save logs at regular intervals, reducing manual effort and ensuring data freshness.
- **User-Friendly Interface**: A responsive web interface built with JavaFX and HTML/CSS, designed for ease of use and accessibility.
- **Data Visualization**: Utilize ECharts for dynamic and interactive visualizations of log data, including pie charts, bar charts, and line graphs.
- **Error Tracking**: Monitor and categorize logs based on severity levels (INFO, WARN, ERROR) for better error management.

## Technologies Used
- **Java**: The primary programming language for the application, utilizing Java 17 or higher.
- **Elasticsearch**: A powerful search and analytics engine for storing and querying log data.
- **JavaFX**: A framework for building rich desktop applications with a modern user interface.
- **Jetty**: A lightweight servlet container for serving web content and handling WebSocket connections.
- **ECharts**: A powerful charting library for data visualization in the web interface.
- **Jackson**: A library for processing JSON data, used for serializing and deserializing log entries.

## Getting Started

### Prerequisites
Before you begin, ensure you have the following installed:
- **Java 17 or higher**: Download from [Oracle](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) or [OpenJDK](https://openjdk.java.net/install/).
- **Maven**: A build automation tool for Java projects. Install it from [Maven's official website](https://maven.apache.org/download.cgi).
- **Elasticsearch**: Version 8.x is required. Download and install from [Elastic's official website](https://www.elastic.co/downloads/elasticsearch).

### Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/faiz-oussama/Distributed-Microservices-Log-Management-System
   cd Distributed-Microservices-Log-Management-System
   ```

2. **Build the project using Maven**:
   ```bash
   mvn clean install
   ```

3. **Start Elasticsearch**:
   - Ensure Elasticsearch is running on your local machine. You can start it using the command:
     ```bash
     ./bin/elasticsearch
     ```
   - Verify that Elasticsearch is running by navigating to `http://localhost:9200` in your web browser.

4. **Run the application**:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.LogMonitoringApp"
   ```

5. **Access the application**:
   - Open your web browser and navigate to `http://localhost:8080` to access the Log Monitoring Dashboard.

### Configuration
- **ElasticSearchClient Configuration**:
  - Open `ElasticSearchClient.java` and update the `serverUrl` and `apiKey` variables with your Elasticsearch server details.
  
- **Log File Path**:
  - Ensure that the log file path in `LogWebSocketServer.java` points to the correct location of your `logs.json` file.

## Usage
### Web Interface
- **Dashboard**: The main dashboard provides an overview of log metrics, including live counts and log levels.
- **Log Visualization**: Use the various charts to analyze log data:
  - **Log Levels Pie Chart**: Displays the distribution of log levels (INFO, WARN, ERROR).
  - **HTTP Methods vs Status Codes**: A bar chart comparing the success and failure rates of different HTTP methods.
  - **Top Accessed URLs**: A bar chart showing the most frequently accessed URLs.
  - **Error Trends**: A line chart visualizing error occurrences over time.

### Filtering Logs
- Use the date range filter to narrow down logs based on specific time frames. Input the start and end dates and click "Apply" to refresh the data.

### Real-time Log Streaming
- The application streams logs in real-time using WebSocket connections. As new logs are generated, they will automatically appear in the dashboard.

## Project Structure
```
log-management/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           ├── ElasticSearchClient.java
│   │   │           ├── LogEntry.java
│   │   │           ├── LogMonitoringApp.java
│   │   │           ├── LogWebSocketServer.java
│   │   │           ├── LocalHttpServer.java
│   │   │           ├── EcommerceLogGenerator.java
│   │   │           └── ...
│   │   └── resources/
│   │       ├── index.html
│   │       ├── logVis.html
│   │       ├── script.js
│   │       ├── style.css
│   │       └── ...
│   └── test/
│       └── ...
├── pom.xml
└── README.md
```

## Contributing
Contributions are welcome! If you have suggestions for improvements or find bugs, please fork the repository and submit a pull request. Ensure that your code adheres to the project's coding standards and includes appropriate tests.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments
- [Elasticsearch](https://www.elastic.co/) for providing a powerful search and analytics engine.
- [JavaFX](https://openjfx.io/) for enabling rich desktop application development.
- [ECharts](https://echarts.apache.org/) for providing a robust charting library for data visualization.
- [Jetty](https://www.eclipse.org/jetty/) for serving web content and handling WebSocket connections.

## Contact
For any inquiries or support, please contact [your.email@example.com](mailto:faizouss123@gmail.com).
