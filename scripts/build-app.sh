#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

./mvnw clean package
mkdir -p app
cp target/sevendays-states-0.0.1-SNAPSHOT.jar app/sevendays-states.jar

echo "Built app/sevendays-states.jar"
