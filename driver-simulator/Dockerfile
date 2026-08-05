FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common-proto ./common-proto
COPY user-service ./user-service
COPY order-service ./order-service
COPY notification-service ./notification-service
COPY delivery-service ./delivery-service
COPY api-gateway ./api-gateway
COPY driver-simulator ./driver-simulator
RUN mvn -q -pl driver-simulator -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/driver-simulator/target/driver-simulator.jar simulator.jar
ENTRYPOINT ["java", "-jar", "simulator.jar"]
