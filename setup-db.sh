#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────
# NeoGaming — Setup base de datos local
# Uso: ./setup-db.sh
#
# Requisitos:
#   - PostgreSQL instalado y corriendo en localhost:5432
#   - Archivo neogaming_seed.sql en la misma carpeta
#   - Usuario de postgres con permisos para crear BD
# ─────────────────────────────────────────────────────────

set -euo pipefail

BOLD='\033[1m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/neogaming_seed.sql"
DB_NAME="neogaming"

echo -e "${BOLD}NeoGaming — Setup BD local${NC}"
echo ""

# ── Verificar que existe el archivo SQL ──────────────────
if [[ ! -f "$SQL_FILE" ]]; then
  echo -e "${RED}[ERR]${NC} No se encontró neogaming_seed.sql en $SCRIPT_DIR"
  echo "      Descárgalo y ponlo en la misma carpeta que este script."
  exit 1
fi

# ── Detectar usuario de postgres ─────────────────────────
PG_USER="${PG_USER:-$(whoami)}"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"

echo -e "${CYAN}[1/3]${NC} Configuración detectada:"
echo "      Host: $PG_HOST:$PG_PORT"
echo "      Usuario: $PG_USER"
echo "      Base de datos: $DB_NAME"
echo ""

# ── Crear BD si no existe ────────────────────────────────
echo -e "${CYAN}[2/3]${NC} Creando base de datos '$DB_NAME'..."
psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres \
  -c "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null || true
psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres \
  -c "CREATE DATABASE $DB_NAME;" 2>/dev/null

# ── Importar datos ───────────────────────────────────────
echo -e "${CYAN}[3/3]${NC} Importando datos (puede tardar unos segundos)..."
psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$DB_NAME" \
  -f "$SQL_FILE" > /dev/null 2>&1

echo ""
echo -e "${GREEN}✓ Base de datos lista${NC}"
echo ""
echo "  Usuarios:   $(psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $DB_NAME -tAc 'SELECT COUNT(*) FROM users;' 2>/dev/null || echo '?')"
echo "  Productos:  $(psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $DB_NAME -tAc 'SELECT COUNT(*) FROM products;' 2>/dev/null || echo '?')"
echo "  Categorías: $(psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $DB_NAME -tAc 'SELECT COUNT(*) FROM categories;' 2>/dev/null || echo '?')"
echo ""
echo "  Configura tu backend con:"
echo "    DB_URL=jdbc:postgresql://localhost:5432/$DB_NAME"
echo "    DB_USERNAME=$PG_USER"
echo "    DB_PASSWORD=<tu password>"
