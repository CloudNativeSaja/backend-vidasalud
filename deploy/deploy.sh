#!/bin/bash
# Redespliega el backend en la EC2:  ./deploy/deploy.sh
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || { echo "Falta .env (cp .env.example .env y editar)"; exit 1; }

git pull --ff-only
docker compose up -d --build
docker image prune -f >/dev/null

echo "Esperando que el BFF quede sano..."
for i in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    echo "OK: BFF responde"; docker compose ps; exit 0
  fi
  sleep 5
done
echo "El BFF no respondio a tiempo. Revisar: docker compose logs --tail=80 bff"; exit 1
