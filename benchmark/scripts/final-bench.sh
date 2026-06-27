#!/bin/bash
# AmetystKQ Final Comprehensive Benchmark
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
SRV="$ROOT/server"
JAR="$ROOT/source/paper-server/build/libs/AmetystKQ-1.21.11-R0.1-SNAPSHOT.jar"
RCON_BIN="$DIR/mcrcon"
RES="$DIR/results"
TAG=$(date +%Y%m%d_%H%M%S)
mkdir -p "$RES"
SUMMARY="$RES/final_$TAG.txt"

RPORT=25593
RPASS="ametystkq"

log() { echo "[$(date +%H:%M:%S)] $*"; }

rcon() { timeout 8 "$RCON_BIN" -H localhost -P $RPORT -p "$RPASS" "$1" >> "$RPCFILE" 2>/dev/null || true; }

# ---- Single profile benchmark ----
bench_profile() {
    local mem="$1" label="$2"
    local SLOG="$RES/server_${label}_$TAG.log"
    RPCFILE="$RES/rcon_${label}_$TAG.txt"
    > "$RPCFILE"

    log "=== $label ($mem) ==="

    # Clean world but KEEP libraries for cached Paperclip
    rm -rf "$SRV/world" "$SRV/world_nether" "$SRV/world_the_end" "$SRV/config" \
           "$SRV/logs" "$SRV/cache" "$SRV/plugins" \
           "$SRV/ops.json" "$SRV/usercache.json" "$SRV/whitelist.json" \
           "$SRV/server.properties" "$SRV/bukkit.yml" "$SRV/spigot.yml" \
           "$SRV/commands.yml" "$SRV/help.yml" "$SRV/permissions.yml" \
           "$SRV/ametystkq.jsa" "$SRV"/*.log 2>/dev/null || true
    mkdir -p "$SRV/plugins"

    cat > "$SRV/server.properties" <<EOF
enable-rcon=true
rcon.port=$RPORT
rcon.password=$RPASS
broadcast-rcon-to-ops=true
server-port=25594
online-mode=false
level-seed=42
view-distance=8
simulation-distance=8
max-players=50
level-type=default
allow-nether=true
allow-end=true
generate-structures=true
motd=AmetystKQ Benchmark
EOF
    echo "eula=true" > "$SRV/eula.txt"

    local flags="-Xms$mem -Xmx$mem -XX:+UseZGC -XX:+AlwaysPreTouch"
    flags+=" -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=5"
    flags+=" -Dpaper.playerconnection.keepalive=60"
    flags+=" -Dio.netty.leakDetectionLevel=disabled"

    cd "$SRV"
    local T0=$SECONDS
    java $flags -jar "$JAR" --nogui </dev/null > "$SLOG" 2>&1 &
    local pid=$!

    # Wait for RCON
    local ok=0
    for ((i=0;i<180;i++)); do
        if grep -q "RCON running" "$SLOG" 2>/dev/null; then ok=1; break; fi
        sleep 1
    done
    [ "$ok" = "1" ] || { log "FAIL: timeout"; kill $pid 2>/dev/null; return 1; }
    local startup_sec=$((SECONDS - T0))
    local startup_log=$(grep -oP 'Done \(\K[\d.]+(?=s\)! For help)' "$SLOG" | head -1 || echo "$startup_sec")
    log "Startup: ${startup_log}s (wall ${startup_sec}s)"
    sleep 2

    # === TEST 1: Idle TPS ===
    log "[1/5] Idle TPS (20s)..."
    sleep 20
    rcon "tps"
    
    # === TEST 2: Chunk gen (teleport around) ===
    log "[2/5] Chunk gen..."
    rcon "execute in minecraft:overworld run tp 100 64 0"
    sleep 1
    rcon "execute in minecraft:overworld run tp 200 64 0"
    sleep 1
    rcon "execute in minecraft:overworld run tp 0 64 100"
    sleep 1
    rcon "execute in minecraft:overworld run tp 0 64 200"
    sleep 1
    rcon "execute in minecraft:overworld run tp 0 64 -100"
    sleep 1
    rcon "execute in minecraft:overworld run tp -100 64 -100"
    sleep 1
    rcon "execute in minecraft:overworld run tp 200 64 200"
    sleep 1
    rcon "execute in minecraft:overworld run tp 0 64 0"
    sleep 3
    rcon "tps"
    
    # === TEST 3: 1000 entities (500 animals + 500 monsters) ===
    log "[3/5] 1000 entities..."
    # 500 sheep
    rcon "summon minecraft:sheep 5 64 0"
    for i in $(seq 1 50); do rcon "summon minecraft:sheep $((i*5)) 64 0"; done
    # 500 zombies
    for i in $(seq 1 50); do rcon "summon minecraft:zombie $((i*5)) 64 0"; done
    sleep 10
    rcon "tps"
    
    # === TEST 4: 500 items ===
    log "[4/5] 500 items..."
    for i in $(seq 1 50); do
        rcon "summon minecraft:item $((i*5)) 64 0 {Item:{id:\"minecraft:dirt\",count:1}}"
    done
    sleep 8
    rcon "tps"
    
    # === TEST 5: 441 hoppers ===
    log "[5/5] Hopper grid (21x21)..."
    rcon "fill -10 64 -10 10 64 10 minecraft:hopper"
    sleep 1
    rcon "fill -10 63 -10 10 63 10 minecraft:chest"
    sleep 8
    rcon "tps"
    
    # Recovery
    sleep 15
    rcon "tps"
    
    # Stop
    rcon "stop" > /dev/null 2>&1 || true
    sleep 5
    kill $pid 2>/dev/null || true
    wait $pid 2>/dev/null || true
    
    # Parse
    parse_results "$SLOG" "$RPCFILE" "$label" "$mem" "$startup_log" "TOTAL=$((SECONDS - T0))s"
}

parse_results() {
    local slog="$1" rpc="$2" label="$3" mem="$4" startup="$5" extra="$6"
    local clean=$(tr -d '\r' < "$rpc" | sed 's/\x1b\[[0-9;]*m//g')
    local tps=($(echo "$clean" | grep "TPS from last" | grep -oP '\d+\.\d+' | head -10))
    
    local maxmem=$(grep -oP 'Max memory: \K\d+' "$slog" | head -1 || echo "?")
    local wsize=$(du -sh "$SRV/world" 2>/dev/null | awk '{print $1}' || echo "?")
    
    # Count entities used from log
    local ecount=$(grep -oP 'Entities:\s+\K\d+' "$slog" 2>/dev/null | tail -1 || echo "?")
    
    echo ""
    echo "=== $label ($mem) ==="
    echo "  Startup:        ${startup}s (${extra})"
    echo "  Idle TPS:       ${tps[0]:-N/A}"
    echo "  Chunk TPS:      ${tps[1]:-N/A}"
    echo "  Entity TPS:     ${tps[2]:-N/A}"
    echo "  Item TPS:       ${tps[3]:-N/A}"
    echo "  Hopper TPS:     ${tps[4]:-N/A}"
    echo "  Recovery TPS:   ${tps[5]:-N/A}"
    echo "  Max memory:     ${maxmem}MB"
    echo "  Entity count:   ${ecount}"
    echo "  World on disk:  ${wsize}"
    
    # Save raw
    {
        echo "label=$label mem=$mem"
        echo "startup=$startup"
        echo "tps_idle=${tps[0]:-N/A}"
        echo "tps_chunk=${tps[1]:-N/A}"
        echo "tps_entity=${tps[2]:-N/A}"
        echo "tps_item=${tps[3]:-N/A}"
        echo "tps_hopper=${tps[4]:-N/A}"
        echo "tps_recov=${tps[5]:-N/A}"
        echo "mem_max=${maxmem}"
        echo "entities=${ecount}"
        echo "world=${wsize}"
    } > "$RES/raw_${label}_$TAG.txt"
    
    # Summary
    {
        echo ""
        echo "=== $label ($mem) ==="
        echo "  Startup:        ${startup}s"
        echo "  Idle TPS:       ${tps[0]:-N/A}"
        echo "  Chunk TPS:      ${tps[1]:-N/A}"
        echo "  Entity TPS:     ${tps[2]:-N/A}"
        echo "  Item TPS:       ${tps[3]:-N/A}"
        echo "  Hopper TPS:     ${tps[4]:-N/A}"
        echo "  Recovery TPS:   ${tps[5]:-N/A}"
        echo "  Max memory:     ${maxmem}MB"
        echo "  Entity count:   ${ecount}"
        echo "  World on disk:  ${wsize}"
    } >> "$SUMMARY"
}

# ====================================================================
echo "================================================"
echo " AmetystKQ Final Benchmark"
echo "================================================"
echo " Date:  $(date)"
echo " JVM:   $(java -version 2>&1 | head -1)"
echo " CPU:   $(nproc) cores"
echo " OS:    $(free -h | grep Mem | awk '{print $2}') RAM"
echo " JAR:   $JAR"
echo "================================================"

{
    echo "================================================"
    echo " AmetystKQ Final Benchmark - $(date)"
    echo " JVM: $(java -version 2>&1 | head -1)"
    echo " CPU: $(nproc) cores"
    echo " OS RAM: $(free -h | grep Mem | awk '{print $2}')"
    echo "================================================"
    echo " Tests: idle TPS, chunk gen, 1000 entities,"
    echo "         500 items, 441 hoppers, recovery TPS"
} > "$SUMMARY"

for mem in "1G" "2G" "4G"; do
    bench_profile "$mem" "$mem"
done

echo ""
echo "================================================"
echo " COMPLETE"
echo "================================================"
cat "$SUMMARY"
