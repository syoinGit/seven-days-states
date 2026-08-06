# Running and deployment

## Environment

Copy `.env.example` to `.env` and set real values.

```bash
cp .env.example .env
```

`.env.example` is the tracked EC2 template. `.env` contains host-specific values
and secrets and must never be committed.

For local development, change these values in `.env`:

```dotenv
APP_ENVIRONMENT=local
SEVEN_DAYS_LOG_SOURCE=file
SEVEN_DAYS_ROOT=7dtd
SEVEN_DAYS_DOCKER_LOG_ENABLED=false
SEVEN_DAYS_TELNET_ENABLED=false
```

For EC2, keep the production paths from `.env.example`. The repository directory
and the 7DTD server directory are intentionally separate:

```text
/home/ec2-user/seven-days-stats  # application Git repository
/home/ec2-user/7dtd              # live 7DTD server data
```

Copy the Compose template only for a new environment. Do not replace an existing
EC2 `compose.yml` until its PostgreSQL volume mapping has been compared.

```bash
cp compose.example.yml compose.yml
```

The Spring Boot app does not automatically load `.env`, so export it before starting the app:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

For EC2/systemd, set the same values through an `EnvironmentFile` or service environment.

### Bedrock observations

The integration is disabled by default. On an EC2 instance with an IAM role that can call
`bedrock:InvokeModel`, enable it without adding static AWS credentials:

```dotenv
WATCHPOINT_AI_BEDROCK_ENABLED=true
WATCHPOINT_AI_AWS_REGION=ap-northeast-1
WATCHPOINT_AI_BEDROCK_MODEL_ID=jp.anthropic.claude-haiku-4-5-20251001-v1:0
WATCHPOINT_AI_SCHEDULE_MINUTES=30
```

The AWS SDK uses `DefaultCredentialsProvider`, which obtains temporary credentials from the
attached EC2 IAM role. Do not add access keys, secret keys, Anthropic keys, or Bedrock API keys
to `.env`. An administrator can test one generation with `POST /maintenance/ai-analysis/publish`.

## EC2 layout

The production directory is expected to be:

```text
/home/ec2-user/seven-days-stats
├── .env
├── app
│   └── app.jar
├── compose.yml
└── scripts
```

Live 7DTD files remain under `/home/ec2-user/7dtd` and are not part of this
repository.

Build and place the jar under `app/`:

```bash
./scripts/build-app.sh
```

Run the jar from the project root so relative paths such as `SEVEN_DAYS_ROOT=7dtd` resolve correctly:

```bash
./scripts/run-app.sh
```

Deploy the latest `main` branch, build the jar, and restart systemd:

```bash
cd /home/ec2-user/seven-days-stats
./scripts/deploy-app.sh
```

The deployment script detects both `seven-days-stats.service` and the legacy
`sevendays-states.service`. To select a unit explicitly, run:

```bash
SERVICE_NAME=sevendays-states.service ./scripts/deploy-app.sh
```

Inspect production logs with:

```bash
sudo journalctl -u seven-days-stats.service -n 100 --no-pager
```
