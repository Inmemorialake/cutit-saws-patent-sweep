#!/usr/bin/env bash
# Prepara un EC2 con Ubuntu 24.04 para desplegar Patent Sweep (ver docs/despliegue/EC2.md).
#
#   sudo bash preparar-ec2.sh [usuario]
#
# - Instala Docker Engine y el plugin de Docker Compose desde el repositorio oficial de Docker.
# - Crea 2 GB de swap en /swapfile (útil en instancias de 1–2 GB de RAM).
# - Crea /opt/patent-sweep, cuyo dueño es el usuario del despliegue, y lo agrega al grupo docker.
#
# Es idempotente: se puede ejecutar varias veces; cada paso revisa si ya está hecho.
# El usuario por defecto es el que invoca sudo (SUDO_USER), o "ubuntu" si no hay ninguno.

set -euo pipefail

DIRECTORIO_APP="/opt/patent-sweep"
ARCHIVO_SWAP="/swapfile"
TAMANO_SWAP="2G"
USUARIO="${1:-${SUDO_USER:-ubuntu}}"

info() { printf '\n==> %s\n' "$*"; }
aviso() { printf 'AVISO: %s\n' "$*" >&2; }

if [ "$(id -u)" -ne 0 ]; then
  echo "Ejecute el script con sudo: sudo bash $0 [usuario]" >&2
  exit 1
fi

# shellcheck source=/dev/null
. /etc/os-release
if [ "${ID:-}" != "ubuntu" ]; then
  echo "Este script es para Ubuntu (24.04). Distribución detectada: ${PRETTY_NAME:-desconocida}." >&2
  exit 1
fi
if [ "${VERSION_ID:-}" != "24.04" ]; then
  aviso "probado en Ubuntu 24.04; detectado ${VERSION_ID:-desconocido}. Se continúa."
fi

if ! id "$USUARIO" > /dev/null 2>&1; then
  echo "El usuario '$USUARIO' no existe." >&2
  exit 1
fi

# systemd no está disponible dentro de contenedores (útil para probar el script).
hay_systemd() { [ -d /run/systemd/system ]; }

# --- 1. Docker Engine + plugin Compose -------------------------------------------------
info "Docker Engine y plugin Compose"
if command -v docker > /dev/null 2>&1 && docker compose version > /dev/null 2>&1; then
  echo "Ya instalados: $(docker --version) / $(docker compose version)"
else
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -q
  apt-get install -y -q ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  if [ ! -s /etc/apt/keyrings/docker.asc ]; then
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
  fi
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${UBUNTU_CODENAME:-$VERSION_CODENAME} stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -q
  apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  echo "Instalados: $(docker --version) / $(docker compose version)"
fi

if hay_systemd; then
  systemctl enable --now docker > /dev/null
  echo "Servicio docker habilitado y en ejecución."
else
  aviso "sin systemd: no se habilitó el servicio docker."
fi

# --- 2. Swap de 2 GB --------------------------------------------------------------------
info "Swap de $TAMANO_SWAP en $ARCHIVO_SWAP"
if [ -f /.dockerenv ]; then
  aviso "dentro de un contenedor no se puede crear swap; se omite."
else
  if [ ! -f "$ARCHIVO_SWAP" ]; then
    fallocate -l "$TAMANO_SWAP" "$ARCHIVO_SWAP" || dd if=/dev/zero of="$ARCHIVO_SWAP" bs=1M count=2048 status=none
    chmod 600 "$ARCHIVO_SWAP"
    mkswap "$ARCHIVO_SWAP" > /dev/null
    echo "Archivo de swap creado."
  else
    echo "El archivo de swap ya existe."
  fi
  if ! swapon --show=NAME --noheadings | grep -qx "$ARCHIVO_SWAP"; then
    swapon "$ARCHIVO_SWAP"
    echo "Swap activado."
  else
    echo "El swap ya está activo."
  fi
  if ! grep -qE "^${ARCHIVO_SWAP}[[:space:]]" /etc/fstab; then
    echo "$ARCHIVO_SWAP none swap sw 0 0" >> /etc/fstab
    echo "Swap agregado a /etc/fstab."
  fi
  # Usar el swap solo como respaldo, no como memoria habitual.
  echo "vm.swappiness=10" > /etc/sysctl.d/99-patent-sweep-swap.conf
  sysctl -q -p /etc/sysctl.d/99-patent-sweep-swap.conf
fi

# --- 3. Carpeta de la aplicación y permisos --------------------------------------------
info "Carpeta $DIRECTORIO_APP y usuario '$USUARIO'"
install -d -m 0750 -o "$USUARIO" -g "$USUARIO" "$DIRECTORIO_APP"
echo "Carpeta lista (dueño: $USUARIO)."

if id -nG "$USUARIO" | tr ' ' '\n' | grep -qx docker; then
  echo "'$USUARIO' ya pertenece al grupo docker."
else
  usermod -aG docker "$USUARIO"
  echo "'$USUARIO' agregado al grupo docker (cierre sesión y vuelva a entrar para que aplique)."
fi

info "Listo"
if [ ! -f "$DIRECTORIO_APP/.env" ]; then
  echo "Falta crear $DIRECTORIO_APP/.env (chmod 600). Ver docs/despliegue/EC2.md, sección del .env de producción."
else
  echo "$DIRECTORIO_APP/.env ya existe; no se modificó."
fi
