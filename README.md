# sevendays-states

7 Days to Die dedicated server logs and save data dashboard.

The application imports server data from `players.xml`, Docker logs, and Telnet `lp` output, then displays a Japanese travel-diary style dashboard with player locations, recent events, POI names, kill counts, and server metrics.

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
- Polls Telnet `lp` output to refresh current online player state.
- Displays player cards, timeline entries, POI status, kill leaders, and server status.
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
├── 7dtd/config/                 # Local 7DTD config files
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
APP_ENVIRONMENT=production
SEVEN_DAYS_LOG_SOURCE=docker
SEVEN_DAYS_ROOT=/home/ec2-user/7dtd
SEVEN_DAYS_DATA_DIRECTORY=/home/ec2-user/7dtd/data
SEVEN_DAYS_GAME_DIRECTORY=/home/ec2-user/7dtd/game
SEVEN_DAYS_CONTAINER_NAME=7dtd
SEVEN_DAYS_DOCKER_LOG_SINCE=5m
SEVEN_DAYS_TELNET_ENABLED=true
SEVEN_DAYS_TELNET_HOST=127.0.0.1
SEVEN_DAYS_TELNET_PORT=8081
SEVEN_DAYS_TELNET_PASSWORD=
SEVEN_DAYS_LOG_SERVER_METRIC_INTERVAL_MINUTES=60
SEVEN_DAYS_CURRENT_STATE_MAX_AGE_SECONDS=120
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

The application must be started from the project root so relative paths such as `SEVENDAYS_ROOT=7dtd` resolve correctly.

## Production Layout

The intended EC2 layout is:

```text
/home/ec2-user/sevendays-states
├── .env
├── 7dtd
├── app
│   └── app.jar
├── scripts
└── src / pom.xml / ...
```

The systemd service should run from:

```text
WorkingDirectory=/home/ec2-user/sevendays-states
EnvironmentFile=/home/ec2-user/sevendays-states/.env
ExecStart=java -jar app/app.jar
```

Typical production service commands:

```bash
sudo systemctl restart sevendays-states
sudo systemctl status sevendays-states --no-pager
sudo journalctl -u sevendays-states -n 100 --no-pager
```

## Notes

- Player uniqueness must not be based on name alone.
- EOS ID is the first stable identity key, Steam ID is the fallback.
- Entity ID may change after reconnects or server restarts and should not be used as the master identity.
- Existing duplicate player/history rows should be handled safely in views or through explicit reviewed SQL, not by risky automatic deletes.
