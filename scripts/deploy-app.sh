#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE_NAME="${SERVICE_NAME:-}"

cd "$ROOT_DIR"

if [[ ! -f .env ]]; then
  echo "Missing .env: $ROOT_DIR/.env" >&2
  exit 1
fi

if [[ -z "$SERVICE_NAME" ]]; then
  for candidate in seven-days-stats.service sevendays-states.service; do
    if systemctl cat "$candidate" >/dev/null 2>&1; then
      SERVICE_NAME="$candidate"
      break
    fi
  done
fi

if [[ -z "$SERVICE_NAME" ]]; then
  echo "No seven-days-stats systemd service found." >&2
  echo "Set SERVICE_NAME explicitly and run this script again." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  echo "Tracked files contain local changes; deployment aborted." >&2
  exit 1
fi

git pull --ff-only
./scripts/build-app.sh
sudo systemctl restart "$SERVICE_NAME"

if ! sudo systemctl is-active --quiet "$SERVICE_NAME"; then
  sudo systemctl status "$SERVICE_NAME" --no-pager >&2
  exit 1
fi

sudo systemctl status "$SERVICE_NAME" --no-pager
