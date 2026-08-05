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
- Tracks travel distance for each vehicle and player, including verified vehicle distance attributed to its driver.
- Attributes vehicle movement only when a fresh, verified driver position matches the vehicle; ambiguous movement is excluded from player totals.
- Classifies player movement as on-foot, verified vehicle, or unknown instead of guessing from vehicle ownership alone.
- Condenses the main activity feed to at most one player event per five-minute window and moves blood moon alerts to the sidebar.
- Provides dedicated player, server telemetry, combat, and vehicle pages.
- Builds adventure rankings from kills, travel distance, vehicle distance, and completed login sessions.
- Aggregates seven days of activity for charts and future AI-generated daily adventure journals.
- Shows explored and unexplored world POIs inferred from player positions within 80 metres.
- Ranks defeated enemy types, charts daily kills, and summarizes character XP growth.
- Aggregates verified vehicle distance by driver and vehicle type while excluding unlinked vehicle noise.
- Identifies players by stable external IDs, preferring EOS ID, then Steam ID.
- Shows online players' activity statuses (活動中, ごはん中, AFK, 外出, 就寝中, ソロ探索中) from the web or in-game commands such as `!飯`, `!afk`, and `!ソロ`.
- Sends status changes back to the game through the optional Telnet command client; offline players remain read-only and show their last known location.
- Mixes player posts into the adventure timeline: authenticated players can post and like alongside game events.
- Uses a public landing page, then requires authentication for the dashboard and all data pages.
- Supports a read-only `VIEWER` guest login alongside `PLAYER` and `ADMIN` accounts; guest responses anonymize player names and external platform IDs, remove player-dossier links, and never expose mutation controls.
- Lets administrators issue login accounts and link each account to one game player; passwords are stored as BCrypt hashes.
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

Javaパッケージの責務と依存方向は [ARCHITECTURE.md](ARCHITECTURE.md) にまとめています。

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
AI_COMMENT_EDITOR_KEY=replace-with-a-long-random-secret
WATCHPOINT_BOOTSTRAP_LOGIN=admin
WATCHPOINT_BOOTSTRAP_PASSWORD=replace-with-a-long-random-password
```

`AI_COMMENT_EDITOR_KEY` protects diary publishing under `/maintenance/diaries`.
If it is empty, daily generation data remains visible but the publishing form is disabled.

`WATCHPOINT_BOOTSTRAP_LOGIN` and `WATCHPOINT_BOOTSTRAP_PASSWORD` create the first
administrator account on startup when both are set. The password is immediately
stored as a BCrypt hash; the plaintext value is only read from the environment.
After logging in, use `/maintenance/accounts` to issue player accounts.

`SESSION_COOKIE_SECURE=true` is required when the site is served over HTTPS (the production
value in `.env.example`). Keep `.env`, PostgreSQL, the 7DTD log/save directories, and the
reverse-proxy access logs outside public storage. The application cannot protect data after a
server, database, or proxy administrator has been compromised.

The public landing page is `/`. `/dashboard`, `/server`, `/kills`, `/vehicles`, `/exploration`,
and `/diaries` require either a player/admin login or the read-only guest login. The guest view
is useful for a portfolio/demo, but only application responses are anonymized; database ports
and the underlying database must remain private. Remove any old Nginx `auth_basic` rule only
after HTTPS and the application login have been tested.

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
