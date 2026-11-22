# Distributed LinkedIn

A set of Spring Boot services behind one API gateway, plus a blank React app. Databases, Kafka, and object storage all run locally. Nothing in this repo calls a hosted database.

## Run everything

```bash
docker compose up --build
```

The first build downloads Maven and Node images and compiles every service. Later starts are faster.

| What | URL |
|---|---|
| API the frontend should call | http://localhost:8080 |
| React app | http://localhost:5173 |
| Eureka | http://localhost:8761 |
| Kafbat UI | http://localhost:8085 |
| MinIO console (`minio` / `minio12345`) | http://localhost:9001 |
| Neo4j browser (`neo4j` / `linkedin123`) | http://localhost:7474 |

Postgres is on `localhost:5432` (`linkedin` / `linkedin`). Kafka is on `localhost:9092`.

Stop the stack with `docker compose down`. Add `-v` if you also want to delete the database volumes.

## Layout

```
apps/web                  React, TypeScript, Tailwind, shadcn
services/api-gateway      the only public HTTP entry
services/discovery-server Eureka
services/user-service     signup, login, profile image
services/post-service     posts, likes, Kafka producers
services/connection-service  Neo4j graph and connection requests
services/notification-service Kafka consumers
services/uploader-service image upload
deploy/postgres           creates the local databases
docs/GUIDE.md             what changed and how each flow works
```

Read [docs/GUIDE.md](docs/GUIDE.md) for the Kafka topics, the upload path, Feign calls, and the Docker setup.

## Build one image with Jib

Compose builds images from each service `Dockerfile`. Jib is also configured if you want an image without that Dockerfile:

```bash
mvn -pl services/post-service -am jib:dockerBuild
```

The image name is `linkedin/<service>:local`.
