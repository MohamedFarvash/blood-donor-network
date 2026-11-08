# Blood Donor Network

A Java web app to register blood donors, search them by blood group and city,
and manage blood requests. Built with Java, JDBC, MySQL and Java's built-in HttpServer.

## Features
- Register donors and search by blood group and city
- 90-day eligibility rule before a donor can donate again
- Raise and fulfil blood requests
- About page with blood group compatibility and FAQ

## Run it
1. Install Java and MySQL, then run schema.sql in MySQL
2. Download the MySQL Connector/J jar into this folder
3. Set the password: $env:BLOODDONOR_DB_PASSWORD="your_password"
4. javac -cp ".;mysql-connector-j-26.10.0.jar" *.java
5. java -cp ".;mysql-connector-j-26.10.0.jar" WebServer
6. Open http://localhost:8080/
