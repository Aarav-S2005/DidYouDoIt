#!/usr/bin/env bash
# ==============================================================================
# DidYouDoIt - Linux .deb Package Generation (Debian, Ubuntu, Mint, Pop!_OS)
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${SCRIPT_DIR}/../../app"
OUTPUT_DIR="${SCRIPT_DIR}/output"
APP_VERSION="${1:-1.0.0}"

echo "==> 1. Building DidYouDoIt desktop JAR..."
cd "${APP_DIR}"
./mvnw clean package -DskipTests

JAR_FILE=$(find target -name "DidYouDoIt-*.jar" | head -n 1)
JAR_NAME=$(basename "${JAR_FILE}")

echo "==> 2. Preparing output directory..."
mkdir -p "${OUTPUT_DIR}"

ICON_FILE="${APP_DIR}/src/main/resources/icons/app-icon.png"

echo "==> 3. Running jpackage for Debian/Ubuntu (.deb)..."
jpackage \
  --type deb \
  --input "${APP_DIR}/target" \
  --main-jar "${JAR_NAME}" \
  --main-class "com.aarav.didyoudoit.DidYouDoItApp" \
  --name "didyoudoit" \
  --app-version "${APP_VERSION}" \
  --vendor "Aarav" \
  --description "Personal Accountability & Nagging Desktop App" \
  --icon "${ICON_FILE}" \
  --linux-shortcut \
  --linux-menu-group "Utility;Office;" \
  --dest "${OUTPUT_DIR}"

echo "==> SUCCESS: .deb package created in: ${OUTPUT_DIR}"
