# What changed, and how to read it

This file is the map of the current system. It covers the folder move, the local infrastructure, Kafka, connection requests, image upload, Feign, and Docker.

## 1. Folder structure

Services used to sit in the repository root (`userService`, `postService`, `APIGateway`, and so on). They now live under `services/` with kebab-case names. The React app lives under `apps/web`. Shared deployment files live under `deploy/`.

```
docker-compose.yml
pom.xml                      Maven reactor, lists every service
README.md
docs/GUIDE.md
deploy/postgres/init.sql     creates postsdb and notificationdb
apps/web                     Vite + React + TypeScript + Tailwind + shadcn
services/api-gateway
services/discovery-server
services/user-service
services/post-service
services/connection-service
services/notification-service
services/uploader-service
```

Java package names were left as they were (`me.prathmeshsadake.linkedin.postService` and the others). Only the directories moved, so the code you already know is in the same packages.

The parent `pom.xml` is a reactor. It does not build a jar. `mvn -DskipTests compile` from the root compiles every service.

## 2. One HTTP entry

The browser and the React app talk only to the API gateway:

`http://localhost:8080`

Gateway routes:

| Public path | Service | Auth |
|---|---|---|
| `/api/v1/users/auth/**` | user-service | open (signup and login) |
| `/api/v1/users/**` | user-service | JWT |
| `/api/v1/posts/**` | post-service | JWT |
| `/api/v1/connections/**` | connection-service | JWT |
| `/api/v1/notifications/**` | notification-service | JWT |
| `/api/v1/uploads/**` | uploader-service | JWT |

`StripPrefix=2` removes `/api/v1`. The service then applies its own context path (`/users`, `/posts`, `/connections`, `/notifications`, `/uploads`).

`AuthenticationFilter` checks `Authorization: Bearer <jwt>`, reads the user id from the token subject, and adds `X-User-Id`. Each service copies that header into a thread-local (`AuthContextHolder`) for the request. Signup and login do not go through that filter.

A bad or missing token on a protected route returns 401 from the gateway.

## 3. Local infrastructure instead of hosted services

The previous `application.properties` files pointed at a hosted Postgres (Neon) and expected Neo4j and Kafka on the laptop with hand-written credentials. Those URLs are gone.

`docker compose up` starts:

| Container | Role | Credentials |
|---|---|---|
| `postgres` | one Postgres server, three databases: `userdb`, `postsdb`, `notificationdb` | `linkedin` / `linkedin` |
| `neo4j` | graph for people and relationships | `neo4j` / `linkedin123` |
| `kafka` | single broker, KRaft (no ZooKeeper), default 3 partitions | no auth |
| `kafbat-ui` | topic browser on port 8085 | none |
| `minio` | S3-compatible object store (`pgsty/silo`, a MinIO-compatible server). Official MinIO images are no longer anonymously pullable. | `minio` / `minio12345` |
| `minio-init` | creates the `linkedin` bucket and makes objects readable | runs once, then exits |

`deploy/postgres/init.sql` runs only the first time the Postgres volume is empty. It creates `postsdb` and `notificationdb`. `userdb` is created by `POSTGRES_DB`.

Each Spring service reads the location from environment variables and falls back to `localhost` if you run that one service from the IDE:

- `SPRING_DATASOURCE_URL`
- `SPRING_NEO4J_URI`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
- `AWS_S3_ENDPOINT` and `AWS_S3_PUBLIC_URL`

Inside Compose, those point at the container names (`postgres`, `neo4j`, `kafka`, `discovery-server`, `minio`).

## 4. Docker: Compose, Dockerfiles, and Jib

`docker compose up --build` is the command that starts the whole system.

What that command does, in order:

1. Starts Postgres, Neo4j, Kafka, and MinIO, and waits until their health checks pass.
2. `minio-init` creates the bucket and exits.
3. Builds `discovery-server` from `services/discovery-server/Dockerfile` and waits until port 8761 accepts a connection.
4. Builds and starts user, post, connection, notification, and uploader. They register with Eureka.
5. Builds and starts the gateway on port 8080.
6. Builds and starts the React dev server on port 5173.

Each Java `Dockerfile` is a two-stage build:

- Stage 1 uses `maven:3.9.9-eclipse-temurin-21`, copies that service, and runs `mvn -B -DskipTests package`.
- Stage 2 uses `eclipse-temurin:21-jre` and runs the Spring Boot jar.

The images are separate containers. Compose is what ties them together. There is not one container that runs every JVM.

Jib is a second way to build a Java image, configured in each service `pom.xml` (`jib-maven-plugin` 3.4.5). It does not use the Dockerfile. From the repo root:

```bash
mvn -pl services/post-service -am jib:dockerBuild
```

That produces `linkedin/post-service:local` (and the same pattern for the other services). Compose does not call Jib. Use Jib when you want a smaller image build from Maven. Use Compose when you want the whole system.

Useful commands:

```bash
docker compose up --build          # build and start
docker compose ps                  # see what is healthy
docker compose logs -f post-service
docker compose down                # stop
docker compose down -v             # stop and delete volumes
```

If a service fails on the first boot, it is usually still waiting for Kafka or Eureka. `docker compose logs <service>` shows the reason. `docker compose up` again is safe.

## 5. Signup creates a graph node

Files:

- `services/user-service/.../service/AuthService.java`
- `services/user-service/.../events/UserCreatedEvent.java`
- `services/user-service/.../config/KafkaConfig.java`
- `services/connection-service/.../listener/UserCreatedListener.java`

`POST /api/v1/users/auth/signup` with:

```json
{ "name": "Ada Lovelace", "email": "ada@example.com", "password": "pass123" }
```

The user service saves the row in `userdb`, then publishes `UserCreatedEvent` to the Kafka topic `user-created` (3 partitions, replication factor 1). The connection service listens with group `connection-service` and creates a `Person` node in Neo4j (`userId`, `name`).

Login is unchanged: `POST /api/v1/users/auth/login` returns a JWT. The subject of the token is the user id. Send it as `Authorization: Bearer <token>` on later calls.

The graph node is created asynchronously. If you send a connection request in the same second as signup, the person node might not exist yet. Wait a moment and retry.

## 6. Connection requests

Files:

- `services/connection-service/.../controller/ConnectionsController.java`
- `services/connection-service/.../service/ConnectionsService.java`
- `services/connection-service/.../repository/PersonRepository.java`
- `services/connection-service/.../events/ConnectionRequestedEvent.java`

The current user always comes from `X-User-Id` (the gateway put it there from the JWT). The other user id comes from the path.

| Call | What it does |
|---|---|
| `POST /api/v1/connections/core/requests/{targetUserId}` | current user asks to connect |
| `DELETE /api/v1/connections/core/requests/{targetUserId}` | current user cancels that pending request |
| `POST /api/v1/connections/core/requests/{requesterUserId}/accept` | current user accepts. `{requesterUserId}` is the person who sent it |
| `POST /api/v1/connections/core/requests/{requesterUserId}/decline` | current user declines and the request is deleted |
| `GET /api/v1/connections/core/{userId}/first-degree` | people already connected |

Rules in `ConnectionsService.sendRequest`:

- You cannot request yourself.
- If a `CONNECTED_TO` relationship already exists, the call fails with: people are already connected and the request cannot be sent again.
- If a `CONNECTION_REQUEST` from you to them already exists, the call fails and tells you to cancel it. It does not create a second request.
- Otherwise it writes `(sender)-[:CONNECTION_REQUEST]->(target)` and publishes `connection-requested`.

Accept deletes that request and writes `(sender)-[:CONNECTED_TO]-(target)`. That undirected relationship is what first-degree queries use.

The old endpoint that connected two people immediately, skipping the request, was removed.

## 7. Kafka from the post service

Files:

- `services/post-service/.../config/KafkaConfig.java`
- `services/post-service/.../events/PostCreatedEvent.java`
- `services/post-service/.../events/PostLikedEvent.java`
- `services/post-service/.../service/PostService.java`
- `services/post-service/.../service/PostLikeService.java`
- `services/post-service/.../client/ConnectionsServiceClient.java`

`KafkaConfig` declares two `NewTopic` beans. Spring's `KafkaAdmin` creates them on the broker when post-service starts:

| Topic | Partitions | Replicas | Key |
|---|---|---|---|
| `post-created` | 3 | 1 | recipient user id |
| `post-liked` | 3 | 1 | post owner user id |

`user-created` is declared by the user service. `connection-requested` is declared by the connection service. Same shape: 3 partitions, 1 replica.

Producers use `StringSerializer` for the key and Spring's `JsonSerializer` for the value. Type headers are turned off (`spring.json.add.type.headers=false`) so the consumer does not need the producer's Java class name. Each consumer declares its own copy of the event fields and a `JsonDeserializer` aimed at that class.

`postCreatedKafkaTemplate` is the `KafkaTemplate<String, PostCreatedEvent>` bean. `postLikedKafkaTemplate` is the matching bean for likes.

### When a post is created

`POST /api/v1/posts/core`

```json
{ "content": "hello", "mediaUrl": "http://localhost:9000/linkedin/post/1/....jpg" }
```

`mediaUrl` is optional. The post is saved with `userId` taken from the JWT, not a hardcoded id.

Then `ConnectionsServiceClient` (Feign) calls the connection service:

`GET /connections/core/{authorId}/first-degree`

Feign finds `connection-service` through Eureka. `FeignClientInterceptor` copies `X-User-Id` onto that call.

For every person returned, except the author, post-service sends one `PostCreatedEvent` (`postId`, `authorId`, `recipientUserId`, `content`, `mediaUrl`).

### When a post is liked

`POST /api/v1/posts/likes/{postId}`

The like is stored for the JWT user. If that user already liked it, the call returns 400. Then post-service sends one `PostLikedEvent` (`postId`, `likedByUserId`, `postOwnerId`) so the owner can be notified.

`DELETE /api/v1/posts/likes/{postId}` removes the like and does not publish an event.

## 8. Notification service

Files:

- `services/notification-service/.../listener/NotificationListeners.java`
- `services/notification-service/.../repository/NotificationRepository.java`
- `services/notification-service/.../entity/Notification.java`
- `services/notification-service/.../controller/NotificationController.java`

Three listeners, all in group `notification-service`, concurrency 3 (one consumer thread per partition):

| Topic | Saved for | Message |
|---|---|---|
| `post-created` | `recipientUserId` | user `{authorId}` published a post |
| `post-liked` | `postOwnerId` | user `{likedByUserId}` liked your post |
| `connection-requested` | `toUserId` | `{fromUserName}` sent you a connection request |

Each row is a `Notification`: `userId`, `type`, `message`, `referenceId`, `createdAt`. The repository is `NotificationRepository extends JpaRepository<Notification, Long>` with `findByUserIdOrderByCreatedAtDesc`.

Read them with the recipient's token:

`GET /api/v1/notifications/me`

## 9. Image upload

Files:

- `services/uploader-service/.../service/ImageStorageService.java`
- `services/uploader-service/.../config/S3Config.java`
- `services/uploader-service/.../client/UserServiceClient.java`
- `services/uploader-service/.../controller/UploadController.java`

The uploader speaks the AWS S3 API. In this project the endpoint is MinIO, not a cloud account. `S3Config` builds an `S3Client` with `endpointOverride`, path-style access, and the MinIO access key. The same client code would point at AWS if `AWS_S3_ENDPOINT` were an AWS URL. It is not, in Compose.

`POST /api/v1/uploads/images` is `multipart/form-data`:

- header `Authorization: Bearer <token>`
- form field `usage`: `profile` or `post`
- form field `file`: the image

Allowed types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Anything else is rejected.

The upload itself is the S3 multipart API even for a small file:

1. `createMultipartUpload` opens an upload id.
2. The bytes are split into 5 MB parts. A normal photo is one part.
3. `uploadPart` sends each part.
4. `completeMultipartUpload` commits them.
5. If any part fails, `abortMultipartUpload` drops the incomplete upload.

The response is:

```json
{ "url": "http://localhost:9000/linkedin/profile/<userId>/<uuid>.jpg" }
```

`AWS_S3_PUBLIC_URL` is `http://localhost:9000`, so the URL works from your browser. Inside the Docker network the SDK talks to `http://minio:9000`.

If `usage=profile`, the uploader then calls the user service with Feign (`UserServiceClient`):

`PUT /users/internal/{userId}/profile-image`

That call carries `X-Internal-Token` (`local-dev-token` in Compose). The user service checks the token and stores `profileImageUrl`. This path is not meant to be called from the browser. The browser path for the same update is `PUT /api/v1/users/profile/image` with `{ "url": "..." }` and the user's JWT.

If `usage=post`, the uploader only returns the URL. Put that URL in `mediaUrl` when you create the post. The post row stores it. The uploader does not write to the post database.

`minio-init` sets the bucket to anonymous download, so the returned URL can be opened without a signature.

## 10. Feign, not a stream processor

Service-to-service HTTP in this repo is OpenFeign plus Eureka:

- Post service `ConnectionsServiceClient` loads first-degree connections before it publishes `post-created`.
- Uploader service `UserServiceClient` writes the profile image URL after a profile upload.
- `FeignClientInterceptor` in the post service forwards `X-User-Id`.

There is no Apache Flink job in this repository. Nothing here submits a Flink pipeline. The async work is Kafka consumers in the connection service and the notification service.

## 11. The React app

`apps/web` is a Vite React TypeScript app with Tailwind v4 and shadcn (Nova preset, Radix). `src/App.tsx` is a blank screen with one shadcn button. Pages are not built yet.

`vite.config.ts` proxies `/api` to `VITE_PROXY_TARGET` or `http://localhost:8080`. Inside Compose that target is `http://api-gateway:8080`, so the dev server can call the gateway without a browser CORS problem if you use relative `/api/...` URLs.

`VITE_API_URL` defaults to `http://localhost:8080`. That is the base URL to use from the browser.

Add the next screen in `src/`. Add more shadcn components with:

```bash
cd apps/web
npx shadcn@latest add card input
```

## 12. A full walk-through

Start the stack, then:

1. `POST http://localhost:8080/api/v1/users/auth/signup` for user A and user B.
2. `POST /api/v1/users/auth/login` for each. Keep both tokens.
3. Wait until Eureka shows `CONNECTION-SERVICE` and the user-created events have been consumed. `GET http://localhost:8761` lists the apps. Kafbat at http://localhost:8085 shows `user-created`.
4. As A, `POST /api/v1/connections/core/requests/{bId}` with A's token.
5. As B, `GET /api/v1/notifications/me` shows the request notification.
6. As B, `POST /api/v1/connections/core/requests/{aId}/accept`.
7. `GET /api/v1/connections/core/{aId}/first-degree` returns B.
8. As A, `POST /api/v1/uploads/images` with `usage=post` and an image file. Copy the URL.
9. As A, `POST /api/v1/posts/core` with `content` and that `mediaUrl`.
10. As B, `GET /api/v1/notifications/me` shows the new post.
11. As B, `POST /api/v1/posts/likes/{postId}`.
12. As A, `GET /api/v1/notifications/me` shows the like.

Sending the request again after they are connected returns 400. Sending it twice while it is still pending returns 400. `DELETE /api/v1/connections/core/requests/{targetUserId}` cancels a pending request so it can be sent again.

## 13. Where to look when something breaks

| Symptom | Look at |
|---|---|
| 401 | token missing, expired, or the route is protected |
| 404 from the gateway | path is not one of the routes in `services/api-gateway/src/main/resources/application.yml` |
| empty first-degree list | accept never ran, or the `user-created` consumer has not created the `Person` yet |
| no notification row | Kafbat: is the topic receiving messages? `docker compose logs notification-service` |
| upload 500 | `docker compose logs uploader-service` and `docker compose logs minio` |
| service not in Eureka | `docker compose logs discovery-server` and the service log. The service must reach `http://discovery-server:8761/eureka` |

Kafka topic settings and the producer templates are in each service's `config/KafkaConfig.java`. Event field names have to stay the same on the producer and the consumer, because the JSON has no Java type header.
