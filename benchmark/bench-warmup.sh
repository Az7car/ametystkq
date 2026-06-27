#!/bin/bash
# Cold vs warm startup time comparison
set -euo pipefail

BENCH_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$BENCH_DIR/.." && pwd)"
SRV="$ROOT_DIR/server"
JAR="$ROOT_DIR/source/paper-server/build/libs/AmetystKQ-1.21.11-R0.1-SNAPSHOT.jar"
RES="$BENCH_DIR/results"
TAG=$(date +%Y%m%d_%H%M%S)
mkdir -p "$RES"
RESULTS="$RES/startup_results_$TAG.txt"
echo "mem phase startup_s" > "$RESULTS"

run_test() {
    local mem="$1" phase="$2"
    local log="$RES/startup_${phase}_${mem}_$TAG.log"
    local flags="-Xms${mem} -Xmx${mem} -XX:+UseZGC -XX:+AlwaysPreTouch"
    flags+=" -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=5"

    local t0=$SECONDS
    cd "$SRV"
    java $flags -jar "$JAR" --nogui </dev/null > "$log" 2>&1 || true
    local real_s=$((SECONDS - t0))
    local startup=$(grep -oP 'Done \(\K[\d.]+(?=s\)! For help)' "$log" | head -1 || echo "N/A")
    echo "$mem $phase ${startup}s (elapsed ${real_s}s)"
    echo "$mem $phase $startup" >> "$RESULTS"
}

echo "=== Cold (first run, generates world) ==="
for mem in "1G" "2G" "4G"; do
    rm -rf "$SRV/world" "$SRV/world_nether" "$SRV/world_the_end" "$SRV/config" "$SRV/libraries"
    echo "eula=true" > "$SRV/eula.txt"
    run_test "$mem" "cold"
done

echo ""
echo "=== Warm (world exists) ==="
for mem in "1G" "2G" "4G"; do
    run_test "$mem" "warm"
done

echo ""
cat "$RESULTS"
