#!/bin/bash
# AmetystKQ Comprehensive Benchmark Suite v4
set -euo pipefail

BENCH_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$BENCH_DIR/.." && pwd)"
SERVER_DIR="$ROOT_DIR/server"
RES_DIR="$BENCH_DIR/results"
JAR="$ROOT_DIR/source/paper-server/build/libs/AmetystKQ-1.21.11-R0.1-SNAPSHOT.jar"
RCON_PORT=25589
RCON_PASS="ametystkq"
RCON="$BENCH_DIR/mcrcon"

mkdir -p "$RES_DIR"
TAG=$(date +%Y%m%d_%H%M%S)
SUMMARY="$RES_DIR/summary_$TAG.txt"
RPC="$RES_DIR/rcon_out.txt"

log() { echo "[$(date +%H:%M:%S)] $*"; }
summary() { echo "$*" | tee -a "$SUMMARY"; }

rcon() { timeout 10 "$RCON" -H localhost -P $RCON_PORT -p "$RCON_PASS" "$1" >> "$RPC" 2>/dev/null || true; }

clean_all() {
    rm -rf "$SERVER_DIR/world" "$SERVER_DIR/world_nether" "$SERVER_DIR/world_the_end" \
           "$SERVER_DIR/config" "$SERVER_DIR/plugins" "$SERVER_DIR/logs" "$SERVER_DIR/cache" \
           "$SERVER_DIR/libraries" "$SERVER_DIR/ops.json" "$SERVER_DIR/usercache.json" \
           "$SERVER_DIR/whitelist.json" "$SERVER_DIR/server.properties" \
           "$SERVER_DIR/bukkit.yml" "$SERVER_DIR/spigot.yml" "$SERVER_DIR/commands.yml" \
           "$SERVER_DIR/help.yml" "$SERVER_DIR/permissions.yml" "$SERVER_DIR/ametystkq.jsa" \
           "$SERVER_DIR"/*.log "$SERVER_DIR"/*.dat 2>/dev/null || true
    mkdir -p "$SERVER_DIR/plugins"
    echo "eula=true" > "$SERVER_DIR/eula.txt"
}

gen_props() {
    cat > "$SERVER_DIR/server.properties" <<EOF
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
broadcast-rcon-to-ops=true
server-port=25590
online-mode=false
level-seed=42
view-distance=8
simulation-distance=8
max-players=50
level-type=default
allow-nether=true
allow-end=true
spawn-animals=true
spawn-monsters=true
generate-structures=true
motd=AmetystKQ Benchmark
EOF
}

bench() {
    local mem="$1" label="$2"
    local slog="$RES_DIR/server_${label}.log"  # server log

    log "=== Profile $mem ==="
    clean_all; gen_props
    > "$RPC"

    local flags="-Xms${mem} -Xmx${mem} -XX:+UseZGC -XX:+AlwaysPreTouch"
    flags+=" -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=5"
    flags+=" -Dpaper.playerconnection.keepalive=60"
    flags+=" -Dio.netty.leakDetectionLevel=disabled"

    cd "$SERVER_DIR"
    java $flags -jar "$JAR" --nogui </dev/null > "$slog" 2>&1 &
    local pid=$!

    for ((i=0; i<180; i++)); do
        grep -q "RCON running" "$slog" 2>/dev/null && break
        sleep 1
    done
    sleep 2

    local startup=$(grep -oP 'Done \(\K[\d.]+(?=s\).*For help)' "$slog" | head -1)
    log "Startup: ${startup}s"

    # ====== TEST 1: Idle TPS (30s) ======
    log "[1/6] Idle TPS (30s)..."
    sleep 5
    rcon "tps"
    sleep 25
    rcon "tps"
    sleep 1

    # ====== TEST 2: Chunk generation via teleport ======
    log "[2/6] Chunk gen stress..."
    # Teleport to distant coords to force chunk generation
    for dist in 50 100 150 200 300 400 500; do
        rcon "execute in minecraft:overworld run tp $dist 64 $dist"
        sleep 1
    done
    # Now forceload chunks to keep them loaded
    for x in 0 2 4; do
        for z in 0 2 4; do
            rcon "forceload add $x $z"
        done
    done
    sleep 3
    rcon "tps"
    rcon "gc"
    sleep 2

    # ====== TEST 3: Animal stress (500 sheep) ======
    log "[3/6] Animals (500 sheep)..."
    rcon "summon minecraft:sheep 0 64 0"
    for i in $(seq 1 50); do
        rcon "summon minecraft:sheep $((i*2)) 64 0"
    done
    sleep 8
    rcon "tps"
    sleep 1

    # ====== TEST 4: Monster stress (500 zombies) ======
    log "[4/6] Monsters (500 zombies)..."
    for i in $(seq 1 50); do
        rcon "summon minecraft:zombie $((i*2)) 64 0"
    done
    sleep 8
    rcon "tps"
    sleep 1

    # ====== TEST 5: Item stress (400 items) ======
    log "[5/6] Items (400 items)..."
    for i in $(seq 1 40); do
        rcon "summon minecraft:item $((i*3)) 64 0 {Item:{id:\"minecraft:dirt\",count:1}}"
    done
    sleep 8
    rcon "tps"
    sleep 1

    # ====== TEST 6: Hopper lag machine ======
    log "[6/6] Hoppers + fill..."
    rcon "fill -10 64 -10 10 64 10 minecraft:hopper"
    sleep 1
    rcon "fill -10 63 -10 10 63 10 minecraft:chest"
    sleep 8
    rcon "tps"
    sleep 2

    # Recovery
    sleep 20
    rcon "tps"

    # Stop
    rcon "stop" > /dev/null 2>&1 || true
    sleep 6
    kill $pid 2>/dev/null || true; wait $pid 2>/dev/null || true

    # Merge RCON output into server log (appended at end)
    cat "$RPC" >> "$slog"
    report "$slog" "$label" "$mem" "$startup"
}

report() {
    local log="$1" label="$2" mem="$3" startup="$4"

    # Strip ANSI, extract all TPS values (1m avg)
    local clean=$(tr -d '\r' < "$RPC" | sed 's/\x1b\[[0-9;]*m//g' 2>/dev/null)
    local tps_vals=($(echo "$clean" | grep "TPS from last" | grep -oP '\d+\.\d+' | head -10))

    local tps_idle="${tps_vals[0]:-N/A}"
    local tps_chunk="${tps_vals[2]:-N/A}"   # after all chunks loaded
    local tps_animal="${tps_vals[3]:-N/A}"
    local tps_monster="${tps_vals[4]:-N/A}"
    local tps_item="${tps_vals[5]:-N/A}"
    local tps_hopper="${tps_vals[6]:-N/A}"
    local tps_recov="${tps_vals[7]:-N/A}"

    # Memory from bootstrap
    local max_mem=$(grep -oP 'Max memory: \K\d+' "$log" 2>/dev/null || echo "N/A")

    # World size
    local wsize=$(du -sh "$SERVER_DIR/world" 2>/dev/null | awk '{print $1}' || echo "N/A")

    # Count entities (forceload/chunk info)
    local entity_line=$(echo "$clean" | grep "Entities:" | tail -1 || echo "")
    local entities=$(echo "$entity_line" | grep -oP '\d+' || echo "N/A")

    echo ""
    summary "=== $label ($mem) ==="
    summary "  Startup:      ${startup}s"
    summary "  Idle TPS:     ${tps_idle}"
    summary "  Chunk TPS:    ${tps_chunk}"
    summary "  Animal TPS:   ${tps_animal}"
    summary "  Monster TPS:  ${tps_monster}"
    summary "  Item TPS:     ${tps_item}"
    summary "  Hopper TPS:   ${tps_hopper}"
    summary "  Recovery TPS: ${tps_recov}"
    summary "  Max memory:   ${max_mem}MB"
    summary "  Entities:     ${entities}"
    summary "  World on disk:${wsize}"
    echo ""

    {
        echo "label=$label mem=$mem"
        echo "startup=$startup"
        echo "tps_idle=$tps_idle tps_chunk=$tps_chunk"
        echo "tps_animal=$tps_animal tps_monster=$tps_monster"
        echo "tps_item=$tps_item tps_hopper=$tps_hopper"
        echo "tps_recov=$tps_recov"
        echo "mem_max=${max_mem}MB"
        echo "entities=$entities"
        echo "world=${wsize}"
    } > "$RES_DIR/raw_${label}.txt"
}

# ============================================================
echo "=============================================="
echo " AmetystKQ Comprehensive Benchmark"
echo "=============================================="
echo " Date:  $(date)"
echo " JVM:   $(java -version 2>&1 | head -1)"
echo " CPU:   $(nproc) cores"
echo " RAM:   $(free -h | grep Mem | awk '{print $2}') total"
echo " JAR:   $JAR"
echo "=============================================="

summary "=============================================="
summary " AmetystKQ Benchmark - $(date)"
summary " JVM: $(java -version 2>&1 | head -1)"
summary " CPU: $(nproc) cores"
summary " OS RAM: $(free -h | grep Mem | awk '{print $2}') total"
summary "=============================================="

for mem in "1G" "2G" "4G"; do
    bench "$mem" "$mem"
done

echo ""
echo "=============================================="
echo " Complete!"
echo "=============================================="
cat "$SUMMARY"
