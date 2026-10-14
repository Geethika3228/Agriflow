#!/usr/bin/env bash
# Compile everything and start AgriFlow.   Usage: ./run.sh [port]      (default 8080)
# Add --test to run the brute-force self-check first:   ./run.sh --test
set -e
cd "$(dirname "$0")"
mkdir -p out testout
javac -Xlint:all -d out $(find src -name '*.java')
if [ "$1" = "--test" ]; then
  javac -cp out -d testout test/Selfcheck.java
  java -cp out:testout Selfcheck
  shift
fi
echo "Starting on http://localhost:${1:-8080}  (Ctrl+C to stop)"
exec java -cp out agriflow.AgriFlowApp "${1:-8080}"
