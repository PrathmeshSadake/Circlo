# Circlo

Circlo is a small professional network you can run on one machine. People sign up, publish posts with images, like them, send connection requests, and read notifications. The browser talks to a single API gateway. Every database and broker is a local container.

## Run

```bash
docker compose up --build
```

The first start compiles each service, so it takes a while. After that:

| Surface | Address |
|---|---|
| Web app | http://localhost:5173 |
| API | http://localhost:8080 |
| Eureka | http://localhost:8761 |
| Kafka UI | http://localhost:8085 |
| Object storage console | http://localhost:9001 |
| Neo4j browser | http://localhost:7474 |

Object storage login is `minio` / `minio12345`. Neo4j login is `neo4j` / `linkedin123`. Postgres login is `linkedin` / `linkedin`.

```bash
docker compose down
```

Add `-v` to delete the data volumes as well.

To work on the web app alone, with the API already running:

```bash
cd apps/web
npm install
npm run dev
```

The dev server proxies `/api` to `http://localhost:8080`.

## What you can do in the app

Before sign-in, the screen is two columns. The left side is the Circlo mark. The right side is sign in, create account, or forgot password. Forgot password sets a new password immediately, because this stack does not send email.

After sign-in:

- Feed: write a post, attach an image, like a post.
- Network: see other accounts, send a connection request, cancel a pending request, and see who is already connected.
- Profile: upload a profile picture and review your posts.
- Notices: read post, like, and connection events. A connection request can be accepted or declined there.

## Services

| Service | Job |
|---|---|
| `api-gateway` | Public HTTP entry on port 8080. Checks the JWT on protected routes. |
| `discovery-server` | Eureka registry. |
| `user-service` | Accounts, passwords, profile image URL. |
| `post-service` | Posts, likes, and Kafka events for new posts and likes. |
| `connection-service` | Neo4j people graph and connection requests. |
| `notification-service` | Stores the notices produced from Kafka. |
| `uploader-service` | Multipart image upload to local S3-compatible storage. |

Postgres holds users, posts, and notices. Neo4j holds people and relationships. Kafka carries `user-created`, `post-created`, `post-liked`, and `connection-requested`. Each of those topics has 3 partitions.

A longer walk through the code and the containers is in [docs/GUIDE.md](docs/GUIDE.md).

## Build one service image with Jib

Compose uses the Dockerfile in each service. Jib is also set up if you want Maven to build the image:

```bash
mvn -pl services/post-service -am jib:dockerBuild
```

The image is tagged `linkedin/<artifactId>:local`.

## GitHub

Suggested description:

```
Circlo is a local professional network: accounts, posts, likes, connection requests, notices, and image upload behind one API gateway.
```

Suggested topics:

```
spring-boot
spring-cloud
kafka
neo4j
postgresql
react
typescript
docker-compose
eureka
s3
```
