# Stage 1: Build the WAR file
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run with Tomcat
FROM tomcat:10.1.55-jdk17
WORKDIR /usr/local/tomcat/webapps

# Remove default ROOT app
RUN rm -rf ROOT

# Copy WAR from build stage
COPY --from=build /app/target/event-api.war ./ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]