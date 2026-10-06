#!/usr/bin/env bash
# ==============================================================================
# DidYouDoIt - Linux .rpm Package Generation (Fedora, RHEL, openSUSE)
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

mkdir -p "${OUTPUT_DIR}"
ICON_FILE="${APP_DIR}/src/main/resources/icons/app-icon.png"

echo "==> 2. Running jpackage for Fedora/RHEL (.rpm)..."
jpackage \
  --type rpm \
  --input "${APP_DIR}/target" \
  --main-jar "${JAR_NAME}" \
  --main-class "com.aarav.didyoudoit.DidYouDoItApp" \
  --name "didyoudoit" \
  --app-version "${APP_VERSION}" \
  --vendor "Aarav" \
  --description "Personal Accountability & Nagging Desktop App" \
  --icon "${ICON_FILE}" \
  --linux-shortcut \
  --dest "${OUTPUT_DIR}"

echo "==> SUCCESS: .rpm package created in: ${OUTPUT_DIR}"
