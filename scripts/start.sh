#!/bin/bash
# AmetystKQ Startup Script
# Run this from the AmetystKQ root directory.
# Auto-detects hardware and applies optimal JVM flags.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVER_DIR="$ROOT_DIR/server"

JAR="${1:-$ROOT_DIR/source/paper-server/build/libs/AmetystKQ-1.21.11-R0.1-SNAPSHOT.jar}"
MEMORY="${2:-auto}"
CDS="${3:-auto}"

if [ ! -f "$JAR" ]; then
    echo "JAR not found: $JAR"
    echo "Usage: $0 [jar-path] [memory: auto|1G|2G|4G|8G] [cds: auto|on|off]"
    exit 1
fi

# Auto-detect total system RAM in GB
TOTAL_RAM=$(awk '/MemTotal/ {printf "%d\n", $2/1024/1024}' /proc/meminfo 2>/dev/null || echo 2)
CPU_CORES=$(nproc 2>/dev/null || echo 2)

if [ "$MEMORY" = "auto" ]; then
    if [ "$TOTAL_RAM" -le 2 ]; then
        MEMORY="1G"
        GC_FLAGS="-XX:+UseZGC"
    elif [ "$TOTAL_RAM" -le 4 ]; then
        MEMORY="2G"
        GC_FLAGS="-XX:+UseZGC"
    else
        MEMORY="4G"
        GC_FLAGS="-XX:+UseZGC"
    fi
fi

# Auto-detect Java
if [ -n "${JAVA_HOME:-}" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="java"
fi

cd "$SERVER_DIR"

# CDS archive for faster startup
if [ "$CDS" = "auto" ]; then
    CDS_FILE="ametystkq.jsa"
    if [ -f "$CDS_FILE" ]; then
        CDS_FLAG="-XX:SharedArchiveFile=$CDS_FILE"
    else
        CDS_FLAG="-XX:ArchiveClassesAtExit=$CDS_FILE"
        echo "Generating CDS archive on first run for faster subsequent startups..."
    fi
elif [ "$CDS" = "on" ]; then
    CDS_FILE="ametystkq.jsa"
    if [ -f "$CDS_FILE" ]; then
        CDS_FLAG="-XX:SharedArchiveFile=$CDS_FILE"
    else
        CDS_FLAG=""
        echo "CDS enabled but no archive found. Run once with 'auto' to generate it."
    fi
else
    CDS_FLAG=""
fi

# Calculate thread pool sizes
if [ "$CPU_CORES" -le 2 ]; then
    MAX_CONCURRENT="2"
elif [ "$CPU_CORES" -le 4 ]; then
    MAX_CONCURRENT="4"
else
    MAX_CONCURRENT="8"
fi

echo "=== AmetystKQ ==="
echo "Memory: $MEMORY | CPUs: $CPU_CORES | GC: $GC_FLAGS | I/O: $MAX_CONCURRENT"
echo "Server dir: $SERVER_DIR"
echo "Starting server..."

exec $JAVA -Xms$MEMORY -Xmx$MEMORY \
    $GC_FLAGS \
    $CDS_FLAG \
    -XX:MaxGCPauseMillis=5 \
    -XX:+AlwaysPreTouch \
    -XX:+UseTransparentHugePages \
    -XX:+UnlockExperimentalVMOptions \
    -XX:-OmitStackTraceInFastThrow \
    -XX:PerMethodRecompilationCutoff=5000 \
    -XX:PerBytecodeRecompilationCutoff=5000 \
    -Dpaper.playerconnection.keepalive=60 \
    -Dminecraft.chunk.io.maxConcurrent=$MAX_CONCURRENT \
    -Dio.netty.leakDetectionLevel=disabled \
    -Dio.netty.recycler.maxCapacity.default=65536 \
    -jar "$JAR" --nogui
