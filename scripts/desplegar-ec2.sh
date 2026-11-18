#!/usr/bin/env bash
# Despliega una versión de Patent Sweep en el EC2 (ver docs/despliegue/EC2.md y ADR-007).
#
#   sudo bash desplegar-ec2.sh <dueño/repositorio> <sha-del-commit> <imagen@sha256:digest>
#
# Lo ejecuta el workflow cd.yml en el servidor mediante AWS Systems Manager (Run Command), como root.
# También sirve para desplegar a mano desde una sesión de Session Manager.
#
# 1. Descarga del repositorio público, en el commit exacto, solo docker-compose.yml,
#    docker-compose.prod.yml y mocks/ (el .env de producción no se toca).
# 2. Fija la imagen por digest en imagen.env, para que un reinicio use exactamente la misma imagen.
# 3. Levanta los servicios y verifica /actuator/health y un endpoint de la API.

set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Uso: sudo bash $0 <dueño/repositorio> <sha-del-commit> <imagen@sha256:digest>" >&2
  exit 2
fi

REPOSITORIO="$1"
SHA="$2"
IMAGEN="$3"
DIRECTORIO_APP="${DIRECTORIO_APP:-/opt/patent-sweep}"

# Los argumentos se validan antes de usarlos: llegan desde el workflow.
[[ "$REPOSITORIO" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] || { echo "Repositorio inválido: $REPOSITORIO" >&2; exit 2; }
[[ "$SHA" =~ ^[0-9a-f]{40}$ ]] || { echo "SHA inválido: $SHA" >&2; exit 2; }
[[ "$IMAGEN" =~ ^[a-z0-9._/-]+@sha256:[0-9a-f]{64}$ ]] || { echo "Imagen inválida (se espera nombre@sha256:...): $IMAGEN" >&2; exit 2; }

if [ ! -d "$DIRECTORIO_APP" ]; then
  echo "No existe $DIRECTORIO_APP: ejecute primero scripts/preparar-ec2.sh" >&2
  exit 1
fi
cd "$DIRECTORIO_APP"
if [ ! -f .env ]; then
  echo "No existe $DIRECTORIO_APP/.env. Créelo a mano (ver docs/despliegue/EC2.md, sección 4)." >&2
  exit 1
fi

echo "==> Archivos de despliegue del commit $SHA"
temporal="$(mktemp -d)"
trap 'rm -rf "$temporal"' EXIT
curl -fsSL --retry 3 --max-time 60 \
  "https://codeload.github.com/${REPOSITORIO}/tar.gz/${SHA}" -o "$temporal/codigo.tar.gz"
# La raíz del tar es "<repositorio>-<sha>/"; se conserva solo lo que está debajo de patent-sweep/.
tar -xzf "$temporal/codigo.tar.gz" -C "$temporal" --strip-components=2 --wildcards \
  '*/patent-sweep/docker-compose.yml' '*/patent-sweep/docker-compose.prod.yml' '*/patent-sweep/mocks'
install -m 644 "$temporal/docker-compose.yml" "$temporal/docker-compose.prod.yml" .
rm -rf mocks
mv "$temporal/mocks" mocks

printf 'IMAGE_REF=%s\n' "$IMAGEN" > imagen.env

compose() {
  docker compose --env-file .env --env-file imagen.env \
    -f docker-compose.yml -f docker-compose.prod.yml "$@"
}

echo "==> Descargando imágenes y levantando servicios ($IMAGEN)"
if ! compose pull; then
  echo "No se pudo descargar la imagen. Si el paquete de GHCR es privado, hágalo público (ver EC2.md, sección 5)." >&2
  exit 1
fi
compose up -d --no-build --remove-orphans

puerto="$(grep -E '^API_PUERTO=' .env | tail -n 1 | cut -d= -f2- | tr -d "\"' \r")"
puerto="${puerto:-8085}"

fallar() {
  echo "ERROR: $*" >&2
  compose ps >&2 || true
  compose logs --tail=150 api-patent-sweep >&2 || true
  exit 1
}

echo "==> Verificando salud de la API en el puerto $puerto"
sano=false
for intento in $(seq 1 30); do
  if curl -fsS "http://localhost:${puerto}/actuator/health" 2>/dev/null | grep -q '"UP"'; then
    sano=true
    echo "Salud UP (intento $intento)"
    break
  fi
  echo "Esperando a la API (intento $intento/30)..."
  sleep 6
done
[ "$sano" = true ] || fallar "La API no respondió UP en /actuator/health."

# Prueba de humo: una patente que no existe debe dar 404 en ProblemDetail (controlador + base de datos).
codigo="$(curl -s -o "$temporal/humo.json" -w '%{http_code}' "http://localhost:${puerto}/api/v1/patentes/estado/0")"
if [ "$codigo" != "404" ] || ! grep -q '"status":404' "$temporal/humo.json"; then
  fallar "La prueba de humo esperaba 404 en /api/v1/patentes/estado/0 y obtuvo $codigo."
fi
echo "Prueba de humo OK (404 esperado)"

docker image prune -f > /dev/null
echo "Despliegue completado: $IMAGEN (commit $SHA)"
