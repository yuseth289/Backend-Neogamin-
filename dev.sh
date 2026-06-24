#!/usr/bin/env bash
# =============================================================
# NeoGaming — Script de desarrollo local
# Uso: ./dev.sh [start|stop|logs]
# =============================================================

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$PROJECT_DIR/.dev-logs"
PID_FILE="$PROJECT_DIR/.dev-pids"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

log()  { echo -e "${CYAN}[NEO]${NC} $*"; }
ok()   { echo -e "${GREEN}[OK]${NC}  $*"; }
warn() { echo -e "${YELLOW}[!!]${NC}  $*"; }
err()  { echo -e "${RED}[ERR]${NC} $*"; }

# ── Limpieza al salir con Ctrl+C ────────────────────────────
cleanup() {
  echo ""
  warn "Deteniendo todos los procesos..."
  if [[ -f "$PID_FILE" ]]; then
    while IFS= read -r pid; do
      kill "$pid" 2>/dev/null && echo "  killed $pid" || true
    done < "$PID_FILE"
    rm -f "$PID_FILE"
  fi
  log "Deteniendo contenedores Docker..."
  docker compose -f "$PROJECT_DIR/docker-compose.yml" stop postgres redis mailhog 2>/dev/null || true
  ok "Todo detenido."
}
trap cleanup EXIT INT TERM

# ── Comandos auxiliares ──────────────────────────────────────
cmd_logs() {
  echo -e "${BOLD}Logs disponibles en $LOG_DIR:${NC}"
  ls -1 "$LOG_DIR"/*.log 2>/dev/null || warn "No hay logs todavía."
  echo ""
  echo "  tail -f $LOG_DIR/backend.log"
  echo "  tail -f $LOG_DIR/ai-service.log"
  echo "  tail -f $LOG_DIR/frontend.log"
}

cmd_stop() {
  warn "Deteniendo servicios..."
  if [[ -f "$PID_FILE" ]]; then
    while IFS= read -r pid; do
      kill "$pid" 2>/dev/null || true
    done < "$PID_FILE"
    rm -f "$PID_FILE"
  fi
  docker compose -f "$PROJECT_DIR/docker-compose.yml" stop postgres redis mailhog 2>/dev/null || true
  ok "Detenido."
  exit 0
}

# ── Esperar a que un puerto esté disponible ──────────────────
wait_port() {
  local name=$1 port=$2 max=${3:-60}
  local i=0
  log "Esperando $name en :$port..."
  until nc -z localhost "$port" 2>/dev/null; do
    sleep 1
    i=$((i+1))
    if [[ $i -ge $max ]]; then
      err "$name no respondió en $max s — revisa $LOG_DIR/$name.log"
      exit 1
    fi
  done
  ok "$name listo (:$port)"
}

# ── Registrar PID ────────────────────────────────────────────
save_pid() { echo "$1" >> "$PID_FILE"; }

# ════════════════════════════════════════════════════════════
#  INICIO
# ════════════════════════════════════════════════════════════
ACTION="${1:-start}"

case "$ACTION" in
  logs) cmd_logs; exit 0 ;;
  stop) trap - EXIT INT TERM; cmd_stop ;;
esac

mkdir -p "$LOG_DIR"
> "$PID_FILE"

echo ""
echo -e "${BOLD}╔══════════════════════════════════════╗${NC}"
echo -e "${BOLD}║    NeoGaming Dev Environment         ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════╝${NC}"
echo ""

# ── 1. Contenedores de infraestructura ──────────────────────
log "Levantando contenedores Docker (postgres · redis · mailhog)..."
docker compose -f "$PROJECT_DIR/docker-compose.yml" \
  up -d redis mailhog \
  --remove-orphans 2>&1 | grep -E "Started|Running|Created|Error" || true

# PostgreSQL local ya corre en :5432 — no se usa el contenedor
wait_port "redis"     6379 30
wait_port "mailhog"   8025 30

# ── 2. AI Microservice ───────────────────────────────────────
AI_DIR="$PROJECT_DIR/neogaming-ai-service"
if [[ ! -d "$AI_DIR/.venv" ]]; then
  warn "No se encontró .venv en neogaming-ai-service — ejecuta: cd neogaming-ai-service && python -m venv .venv && pip install -e ."
else
  log "Iniciando AI microservice (cargando neogaming-ai-service/.env)..."
  (
    cd "$AI_DIR"
    source .venv/bin/activate
    set -o allexport
    [[ -f .env ]] && source <(grep -v '^\s*#' .env | grep -v '^\s*$')
    set +o allexport
    # Apuntar al backend local, no a Railway
    SPRING_BOOT_BASE_URL="http://localhost:8080"
    uvicorn src.api.main:app --host 0.0.0.0 --port 8001 --reload \
      > "$LOG_DIR/ai-service.log" 2>&1
  ) &
  save_pid $!
  wait_port "ai-service" 8001 60
fi

# ── 3. Backend Spring Boot ───────────────────────────────────
BACKEND_DIR="$PROJECT_DIR/backend"
log "Iniciando backend Spring Boot (cargando backend/.env)..."
(
  cd "$BACKEND_DIR"
  # Cargar variables del .env ignorando comentarios y líneas vacías
  set -o allexport
  [[ -f .env ]] && source <(grep -v '^\s*#' .env | grep -v '^\s*$')
  set +o allexport
  ./mvnw spring-boot:run \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Xms256m -Xmx512m" \
    > "$LOG_DIR/backend.log" 2>&1
) &
save_pid $!
wait_port "backend" 8080 120

# ── 4. Frontend Angular ──────────────────────────────────────
FRONT_DIR="$PROJECT_DIR/front-neo"
log "Iniciando frontend Angular..."
(
  cd "$FRONT_DIR"
  npm start > "$LOG_DIR/frontend.log" 2>&1
) &
save_pid $!
wait_port "frontend" 4200 120

# ── Resumen ──────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${GREEN}✓ NeoGaming levantado correctamente${NC}"
echo ""
echo -e "  ${CYAN}Frontend${NC}      →  http://localhost:4200"
echo -e "  ${CYAN}Backend API${NC}   →  http://localhost:8080"
echo -e "  ${CYAN}Swagger UI${NC}    →  http://localhost:8080/swagger-ui.html"
echo -e "  ${CYAN}AI Service${NC}    →  http://localhost:8001"
echo -e "  ${CYAN}MailHog${NC}       →  http://localhost:8025"
echo ""
echo -e "  Logs: ${YELLOW}./dev.sh logs${NC}   |   Parar: ${YELLOW}Ctrl+C${NC} o ${YELLOW}./dev.sh stop${NC}"
echo ""

# Mantener vivo hasta Ctrl+C
wait
