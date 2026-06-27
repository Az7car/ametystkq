#!/bin/bash
# Quick benchmark - startup time + TPS under load
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
SRV="$ROOT/server"
JAR="$ROOT/source/paper-server/build/libs/AmetystKQ-1.21.11-R0.1-SNAPSHOT.jar"
RCON="$DIR/mcrcon"
RES="$DIR/results"
TAG=$(date +%Y%m%d_%H%M%S)
mkdir -p "$RES"
SUMMARY="$RES/report_$TAG.txt"
echo "" > "$SUMMARY"

log() { echo "[$(date +%H:%M:%S)] $*"; }

# Startup test
test_startup() {
    local mem="$1" name="$2"
    local log="$RES/start_${name}_${TAG}.log"
    log "Startup: $name ($mem)..."
    local t0=$SECONDS
    cd "$SRV"
    java -Xms$mem -Xmx$mem -XX:+UseZGC -XX:+AlwaysPreTouch \
         -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=5 \
         -jar "$JAR" --nogui </dev/null > "$log" 2>&1 || true
    local elapsed=$((SECONDS - t0))
    local s=$(grep -oP 'Done \(\K[\d.]+(?=s\)! For help)' "$log" | head -1 || echo "?")
    echo "$name ${s}s (wall ${elapsed}s)" >> "$SUMMARY"
    log "  -> $s startup"
}

# Metrics test via RCON
test_metrics() {
    local mem="$1" name="$2"
    local log="$RES/metrics_${name}_${TAG}.log"
    local rconlog="$RES/rcon_${name}_${TAG}.txt"
    > "$rconlog"

    log "Metrics: $name ($mem)..."
    rm -rf "$SRV/world" "$SRV/world_nether" "$SRV/world_the_end" "$SRV/config"
    cat > "$SRV/server.properties" <<EOF
enable-rcon=true
rcon.port=25591
rcon.password=ametystkq
broadcast-rcon-to-ops=true
server-port=25592
online-mode=false
level-seed=42
view-distance=8
simulation-distance=8
max-players=50
EOF
    echo "eula=true" > "$SRV/eula.txt"

    local flags="-Xms$mem -Xmx$mem -XX:+UseZGC -XX:+AlwaysPreTouch"
    flags+=" -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=5"

    cd "$SRV"
    java $flags -jar "$JAR" --nogui </dev/null > "$log" 2>&1 &
    local pid=$!
    for ((i=0;i<180;i++)); do grep -q "RCON running" "$log" 2>/dev/null && break; sleep 1; done
    sleep 2

    local s=$(grep -oP 'Done \(\K[\d.]+(?=s\)! For help)' "$log" | head -1)

    rcon_cmd() { timeout 8 "$RCON" -H localhost -P 25591 -p "ametystkq" "$1" >> "$rconlog" 2>/dev/null || true; }

    # Idle TPS (record after settle)
    sleep 5
    rcon_cmd "tps"

    # Summon many entities
    for i in $(seq 1 100); do rcon_cmd "summon minecraft:sheep $((i*2)) 64 0"; done
    sleep 6
    rcon_cmd "tps"

    for i in $(seq 1 100); do rcon_cmd "summon minecraft:zombie $((i*2)) 64 0"; done
    sleep 6
    rcon_cmd "tps"

    for i in $(seq 1 100); do rcon_cmd "summon minecraft:item $((i*3)) 64 0 {Item:{id:\"minecraft:dirt\",count:1}}"; done
    sleep 6
    rcon_cmd "tps"

    # Hoppers
    rcon_cmd "fill -5 64 -5 5 64 5 minecraft:hopper"
    sleep 6
    rcon_cmd "tps"

    # Stop
    rcon_cmd "stop" > /dev/null 2>&1 || true
    sleep 6; kill $pid 2>/dev/null || true; wait $pid 2>/dev/null || true

    # Parse TPS
    local clean=$(tr -d '\r' < "$rconlog" | sed 's/\x1b\[[0-9;]*m//g')
    local tps=($(echo "$clean" | grep "TPS from last" | grep -oP '\d+\.\d+' | head -8))

    echo "  start=${s}s" >> "$SUMMARY"
    echo "  tps_idle=${tps[0]:-N/A}" >> "$SUMMARY"
    echo "  tps_sheep=${tps[1]:-N/A}" >> "$SUMMARY"
    echo "  tps_zombie=${tps[2]:-N/A}" >> "$SUMMARY"
    echo "  tps_items=${tps[3]:-N/A}" >> "$SUMMARY"
    echo "  tps_hopper=${tps[4]:-N/A}" >> "$SUMMARY"

    log "  TPS idle=${tps[0]:-N/A} sheep=${tps[1]:-N/A} zombie=${tps[2]:-N/A} items=${tps[3]:-N/A} hopper=${tps[4]:-N/A}"
    rm -rf "$SRV/config" "$SRV/world" "$SRV/world_nether" "$SRV/world_the_end" 2>/dev/null || true
}

# ---- Cold start (3 profiles) ----
echo "=== Startup: Cold (world gen) ===" >> "$SUMMARY"
for mem in 1G 2G 4G; do
    rm -rf "$SRV/world" "$SRV/world_nether" "$SRV/world_the_end" "$SRV/config" "$SRV/libraries" 2>/dev/null || true
    echo "eula=true" > "$SRV/eula.txt"
    test_startup "$mem" "cold_$mem"
done

# ---- Warm start (world exists) ----
echo "=== Startup: Warm ===" >> "$SUMMARY"
for mem in 1G 2G 4G; do
    test_startup "$mem" "warm_$mem"
done

# ---- Metrics test (only 1G, 2G) ----
echo "=== Metrics ===" >> "$SUMMARY"
for mem in 1G 2G; do
    test_metrics "$mem" "$mem"
done

echo ""
echo "=== FINAL REPORT ==="
cat "$SUMMARY"
