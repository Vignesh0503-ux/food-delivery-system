# Real-Time Food Delivery System

A microservices reference project built to exercise every major service
communication pattern in one coherent system: REST, unary gRPC, server
streaming, client streaming, bidirectional streaming, Kafka, and Redis.

## Architecture

```
                         Client (web / mobile)
                                |
                            REST/HTTP
                                |
                          API Gateway (8080)
                                |
              -------------------------------------
              |                |                  |
        User Service     Order Service      Delivery Service
        REST 8081        REST 8082          REST 8084
        gRPC 9091        (gRPC client)      gRPC 9094
              ^                |                  ^
              |          unary gRPC                \
              --------------                        \  bidi + client
                            \                          \  streaming
                     server streaming gRPC               \
                            \                              Driver apps
                      Notification Service                (driver-simulator)
                      REST 8083 / gRPC 9093
                            ^
                            |
                          Kafka
                     (order-created, order-delivered)
                            ^
                            |
                       Order Service (producer)
                            |
                          Redis
              (delivery-service caches driver locations)
```

## Communication patterns, and where to find them

| Pattern | Where | File |
|---|---|---|
| REST | Client → API Gateway → services | `api-gateway/.../application.yml`, each `*Controller.java` |
| Unary gRPC | Order Service → User Service (validate customer on order create) | `order-service/.../UserServiceClient.java`, `user-service/.../UserGrpcService.java` |
| Server streaming gRPC | Order Service → Notification Service (stream of per-channel send statuses) | `order-service/.../NotificationStreamClient.java`, `notification-service/.../NotificationGrpcService.java` |
| Client streaming gRPC | Driver → Delivery Service (upload buffered location history after reconnecting) | `driver-simulator/.../DriverSimulator.java` (`runLocationHistoryUpload`), `delivery-service/.../DeliveryGrpcService.java` (`uploadLocationHistory`) |
| Bidirectional streaming gRPC | Driver ↔ Delivery Service (live position in, dispatch instructions out) | `driver-simulator/.../DriverSimulator.java` (`runLiveTracking`), `delivery-service/.../DeliveryGrpcService.java` (`trackDriver`) |
| Kafka | Order Service publishes `order-created` / `order-delivered`; Notification Service consumes | `order-service/.../OrderEventPublisher.java`, `notification-service/.../OrderEventListener.java` |
| Redis | Delivery Service caches last known driver location | `delivery-service/.../DriverLocationCache.java` |

## Project layout

```
food-delivery-system/
  common-proto/         .proto definitions, compiled to shared gRPC stubs
  user-service/          REST + unary gRPC server
  order-service/         REST, unary+server-streaming gRPC clients, Kafka producer
  notification-service/  server-streaming gRPC server, Kafka consumer
  delivery-service/       REST, client+bidi streaming gRPC server, Redis cache
  api-gateway/            Spring Cloud Gateway, single REST entry point
  driver-simulator/       standalone CLI acting as a driver's mobile app,
                          the only piece that actually calls the streaming RPCs
  docker-compose.yml
```

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker + Docker Compose (for the full stack, Kafka, Redis)

> This project was scaffolded in a sandboxed environment without access to
> Maven Central, so the build has **not** been compiled here. Versions were
> chosen carefully (Spring Boot 3.3.2, grpc-java 1.65.1, grpc-spring-boot-starter
> 3.1.0.RELEASE) but run `mvn -q -am -pl <module> package` locally first and
> fix any version drift before assuming everything is green.

## Running everything with Docker Compose

```bash
docker compose up --build
```

This starts Zookeeper, Kafka, Redis, and all five services. Ports:

| Service | REST | gRPC |
|---|---|---|
| api-gateway | 8080 | – |
| user-service | 8081 | 9091 |
| order-service | 8082 | – (client only) |
| notification-service | 8083 | 9093 |
| delivery-service | 8084 | 9094 |

## Try it out

**1. Create an order** (REST → API Gateway → Order Service → unary gRPC to
User Service → Kafka event → server-streaming gRPC to Notification Service):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": "u1", "items": ["Margherita Pizza", "Garlic Bread"]}'
```

Watch the `order-service` and `notification-service` logs — you'll see the
unary call, the Kafka publish, and the streamed PUSH/SMS/EMAIL statuses.

**2. Mark it delivered:**

```bash
curl -X POST http://localhost:8080/api/orders/<orderId>/deliver
```

**3. Simulate a driver going live** (bidirectional streaming):

```bash
docker compose run --rm driver-simulator delivery-service 9094 driver-1 track
```

or, for the client-streaming location-history-upload path:

```bash
docker compose run --rm driver-simulator delivery-service 9094 driver-1 history
```

**4. Check the cached driver location** (Redis, via REST):

```bash
curl http://localhost:8080/api/drivers/driver-1/location
```

## Running locally without Docker

Start Kafka and Redis yourself (or `docker compose up kafka zookeeper redis`),
then from the repo root:

```bash
mvn -q -am -pl common-proto install
mvn -q -pl user-service spring-boot:run &
mvn -q -pl notification-service spring-boot:run &
mvn -q -pl order-service spring-boot:run &
mvn -q -pl delivery-service spring-boot:run &
mvn -q -pl api-gateway spring-boot:run &
```

Then run the simulator directly:

```bash
mvn -q -pl driver-simulator -am package
java -jar driver-simulator/target/driver-simulator.jar localhost 9094 driver-1 track
```

## Known simplifications (intentional, for a learning project)

- In-memory stores (`UserStore`, `OrderStore`) instead of a real database —
  swap for JPA + Postgres if you want persistence.
- No auth/service discovery on the API Gateway — routes are static, no
  Eureka/Consul. Fine for one instance per service; add if you scale out.
- `DeliveryGrpcService`'s dispatch logic ("every 5th update gets a new
  order") is a stand-in for a real matching/dispatch algorithm.
- Bidirectional streaming here assumes one Delivery Service instance. If
  you run more than one, you'll need sticky routing or to move
  connection/dispatch state into Redis so any instance can serve any driver.
