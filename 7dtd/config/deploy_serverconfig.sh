#!/usr/bin/env bash

set -Eeuo pipefail

SOURCE_FILE="/home/ec2-user/config/serverconfig.xml"
DEST_FILE="/home/ec2-user/7dtd/data/serverconfig.xml"
COMPOSE_DIR="/home/ec2-user/7dtd"
BACKUP_DIR="/home/ec2-user/7dtd/config_backup"
TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"

echo "=== 7DTD serverconfig deploy start ==="

# デプロイ元ファイル確認
if [[ ! -f "$SOURCE_FILE" ]]; then
    echo "ERROR: Source file not found: $SOURCE_FILE" >&2
    exit 1
fi

# XMLの簡易チェック
if command -v xmllint >/dev/null 2>&1; then
    if ! xmllint --noout "$SOURCE_FILE"; then
        echo "ERROR: XML validation failed." >&2
        exit 1
    fi
else
    echo "INFO: xmllint is not installed. Skipping XML validation."
fi

# バックアップディレクトリ作成
mkdir -p "$BACKUP_DIR"

# 現在の設定をバックアップ
if [[ -f "$DEST_FILE" ]]; then
    BACKUP_FILE="${BACKUP_DIR}/serverconfig_${TIMESTAMP}.xml"
    cp -p "$DEST_FILE" "$BACKUP_FILE"
    echo "Backup created: $BACKUP_FILE"
fi

# 新しい設定を配置
install -m 644 "$SOURCE_FILE" "$DEST_FILE"
echo "Deployed: $SOURCE_FILE -> $DEST_FILE"

# Docker Compose設定確認
if [[ ! -f "${COMPOSE_DIR}/compose.yaml" ]] &&
   [[ ! -f "${COMPOSE_DIR}/docker-compose.yml" ]]; then
    echo "ERROR: Compose file not found in $COMPOSE_DIR" >&2
    exit 1
fi

# サーバー再起動
cd "$COMPOSE_DIR"

echo "Restarting 7DTD container..."
docker compose restart 7dtd

echo "Waiting for container..."
sleep 5

# 起動状態確認
if docker compose ps --status running | grep -q "7dtd"; then
    echo "7DTD container is running."
else
    echo "ERROR: 7DTD container may not be running." >&2
    docker compose ps
    exit 1
fi

echo
echo "Recent logs:"
docker compose logs --tail=30 7dtd

echo
echo "=== Deploy completed ==="
