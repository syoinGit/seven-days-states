# Running

## Environment

Copy `.env.example` to `.env` and set real values.

```bash
cp .env.example .env
```

`docker compose` reads `.env` automatically.

The Spring Boot app does not automatically load `.env`, so export it before starting the app:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

For EC2/systemd, set the same values through an `EnvironmentFile` or service environment.

## EC2 layout

The production directory is expected to be:

```text
/home/ec2-user/sevendays-states
├── .env
├── 7dtd
├── app
│   └── sevendays-states.jar
├── compose.yml
├── nginx
├── postgres
└── scripts
```

Build and place the jar under `app/`:

```bash
./scripts/build-app.sh
```

Run the jar from the project root so relative paths such as `SEVENDAYS_ROOT=7dtd` resolve correctly:

```bash
./scripts/run-app.sh
```

Production `.env` example:

```bash
POSTGRES_PASSWORD=...
SEVENDAYS_ENVIRONMENT=production
SEVENDAYS_MODE=docker
SEVENDAYS_ROOT=7dtd
SEVENDAYS_DOCKER_CONTAINER_NAME=7dtd
SEVENDAYS_DOCKER_LOG_SINCE=5m
SEVENDAYS_LOG_SCHEDULED_ENABLED=false
SEVENDAYS_TELNET_SCHEDULED_ENABLED=true
SEVENDAYS_TELNET_HOST=localhost
SEVENDAYS_TELNET_PORT=8081
SEVEN_DAYS_TO_DIE_TELNET_PASSWORD=...
```
