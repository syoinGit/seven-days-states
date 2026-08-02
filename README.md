# WATCHPOINT

7 Days to Die dedicated server logs and save data observation dashboard.

WATCHPOINT turns collected server logs into a mechanical, wasteland-themed activity feed designed to make the game more fun. It imports `players.xml`, Docker logs, and Telnet `lp` output, then connects player activity, combat, vehicles, locations, blood moon alerts, and server telemetry.

## Stack

- Java 21
- Spring Boot 4
- Thymeleaf
- PostgreSQL
- Flyway
- Maven Wrapper

## Main Features

- Imports 7 Days to Die save data such as `players.xml`, world POIs, game entities, and Japanese localization data.
- Streams or imports Docker logs for JOIN, LEAVE, KILL, SLEEPER, XP, and server metric events.
- Polls Telnet `lp` output once per minute to refresh the authoritative online player state.
- Links vehicles to players from logged owner IDs or an unambiguous nearby fresh player position.
- Tracks travel distance for each vehicle and player, including vehicle distance attributed to its owner.
- Condenses the main activity feed to at most one player event per minute and moves blood moon alerts to the sidebar.
- Provides dedicated player, server telemetry, combat, and vehicle pages.
- Builds adventure rankings from kills, travel distance, vehicle distance, and completed login sessions.
- Aggregates seven days of activity for charts and future AI-generated daily adventure journals.
- Identifies players by stable external IDs, preferring EOS ID, then Steam ID.
- Displays event timestamps in JST (`Asia/Tokyo`) as `yyyy-MM-dd HH:mm:ss`.

## Repository Layout

```text
.
├── app/                         # Built production jar location
├── scripts/
│   ├── build-app.sh             # Runs tests/build and writes app/app.jar
│   └── run-app.sh               # Local jar runner
├── src/main/java/               # Spring Boot application
├── src/main/resources/
│   ├── db/migration/            # Flyway migrations
│   ├── static/                  # CSS/images
│   └── templates/               # Thymeleaf templates
├── src/test/java/               # Tests
├── 7dtd/                        # Local-only 7DTD test data (Git-ignored)
├── compose.example.yml          # PostgreSQL Compose template
├── .env.example
└── pom.xml
```

Runtime data under `7dtd/data`, `7dtd/game`, `7dtd/log`, `.env`, and built jars are intentionally ignored by Git.

## Configuration

Copy the example environment file and fill in local or production values.

```bash
cp .env.example .env
```

Important variables:

```bash
POSTGRES_PASSWORD=
APP_ENVIRONMENT=local
SEVEN_DAYS_LOG_SOURCE=file
SEVEN_DAYS_ROOT=7dtd
SEVEN_DAYS_DOCKER_LOG_ENABLED=false
SEVEN_DAYS_TELNET_ENABLED=false
```

Spring Boot does not automatically load `.env` when started directly, so export it before local execution.

```bash
set -a
source .env
set +a
```

## Database

The app expects PostgreSQL and uses Flyway migrations from:

```text
src/main/resources/db/migration
```

Do not edit already-applied Flyway migrations. Add a new versioned migration instead.

## Test

```bash
./mvnw test
```

## Build

Use the project build script:

```bash
scripts/build-app.sh
```

This runs the Maven package lifecycle and places the production jar at:

```text
app/app.jar
```

## Local Run

From the project root:

```bash
set -a
source .env
set +a
java -jar app/app.jar
```

The application must be started from the project root so relative paths such as `SEVEN_DAYS_ROOT=7dtd` resolve correctly.

## Production

EC2 keeps the Git repository at `/home/ec2-user/seven-days-stats` and reads the
7DTD server data from the separate `/home/ec2-user/7dtd` tree. See `RUNNING.md`
for initial setup, deployment, and systemd commands.

## Notes

- Player uniqueness must not be based on name alone.
- EOS ID is the first stable identity key, Steam ID is the fallback.
- Entity ID may change after reconnects or server restarts and should not be used as the master identity.
- Existing duplicate player/history rows should be handled safely in views or through explicit reviewed SQL, not by risky automatic deletes.
