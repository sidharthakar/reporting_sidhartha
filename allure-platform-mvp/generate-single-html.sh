#!/bin/bash
# Usage: ./generate-single-html.sh storage/<appId>/<release>/<runId>
RUN_DIR="$1"
if [ -z "$RUN_DIR" ]; then
  echo "Usage: $0 <run_dir>"
  exit 1
fi
if ! command -v allure >/dev/null 2>&1; then
  echo "Allure CLI not found. Please install Allure CLI and ensure 'allure' is on PATH."
  exit 2
fi
TMP_OUT="$RUN_DIR/.report_out"
mkdir -p "$TMP_OUT"
allure generate "$RUN_DIR/allure-results" -o "$TMP_OUT" --clean
if [ -f "$TMP_OUT/index.html" ]; then
  mv "$TMP_OUT/index.html" "$RUN_DIR/index.html"
  echo "index.html generated at $RUN_DIR/index.html"
else
  echo "Failed to generate index.html"
fi
