#!/bin/bash
# Clona o actualiza los microservicios en ./servicios (cada uno es su propio repositorio).
#
#   ./deploy/servicios.sh                  # rama main de todos
#   SERVICIOS_REF=mi-rama ./deploy/servicios.sh
#
# El compose construye cada imagen desde ./servicios/<servicio>.
set -euo pipefail
cd "$(dirname "$0")/.."

ORG="${GIT_ORG_URL:-https://github.com/CloudNativeSaja}"
REF="${SERVICIOS_REF:-main}"
SERVICIOS=(
  ms-vidasalud-bff
  ms-vidasalud-appointments
  ms-vidasalud-catalog
  ms-vidasalud-notify
  ms-vidasalud-mq-admin
  ms-vidasalud-report
  ms-vidasalud-audit
  ms-vidasalud-kafka-admin
)

mkdir -p servicios

for s in "${SERVICIOS[@]}"; do
  if [ -d "servicios/$s/.git" ]; then
    echo "== Actualizando $s ($REF)"
    git -C "servicios/$s" fetch --quiet origin
    git -C "servicios/$s" checkout --quiet "$REF"
    git -C "servicios/$s" pull --quiet --ff-only origin "$REF"
  else
    echo "== Clonando $s ($REF)"
    git clone --quiet --branch "$REF" "$ORG/$s.git" "servicios/$s"
  fi
done

echo "Servicios listos en ./servicios (rama $REF)"
