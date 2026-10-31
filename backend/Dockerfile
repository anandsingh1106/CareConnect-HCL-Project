FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/careconnect-backend.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
