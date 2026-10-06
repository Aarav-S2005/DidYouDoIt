#!/usr/bin/env bash
# ==============================================================================
# DidYouDoIt - Universal Portable Linux Tarball / App-Image Image
# Runs on any Linux distribution (Arch, Manjaro, Alpine, NixOS, etc.)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${SCRIPT_DIR}/../../app"
OUTPUT_DIR="${SCRIPT_DIR}/output"
APP_VERSION="${1:-1.0.0}"

echo "==> 1. Building DidYouDoIt desktop JAR..."
cd "${APP_DIR}"
./mvnw clean package -DskipTests

JAR_FILE="${APP_DIR}/target/DidYouDoIt-1.0-SNAPSHOT-all.jar"
if [ ! -f "${JAR_FILE}" ]; then
  JAR_FILE=$(find target -name "DidYouDoIt-*.jar" | head -n 1)
fi
JAR_NAME=$(basename "${JAR_FILE}")

mkdir -p "${OUTPUT_DIR}"
rm -rf "${OUTPUT_DIR}/didyoudoit"
ICON_FILE="${APP_DIR}/src/main/resources/icons/app-icon.png"

echo "==> 2. Generating standalone portable app image directory..."
jpackage \
  --type app-image \
  --input "${APP_DIR}/target" \
  --main-jar "${JAR_NAME}" \
  --main-class "com.aarav.didyoudoit.Main" \
  --name "didyoudoit" \
  --app-version "${APP_VERSION}" \
  --icon "${ICON_FILE}" \
  --dest "${OUTPUT_DIR}"

echo "==> 3. Creating universal .tar.gz archive..."
cd "${OUTPUT_DIR}"
tar -czvf "didyoudoit-${APP_VERSION}-linux-x64.tar.gz" didyoudoit

echo "==> SUCCESS: Universal portable archive created: ${OUTPUT_DIR}/didyoudoit-${APP_VERSION}-linux-x64.tar.gz"
