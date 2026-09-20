#!/usr/bin/env bash
# Start, stop and check the app locally.
#
# Use this rather than "mvnw spring-boot:run": that forks a second JVM, so stopping Maven can
# leave the app running and holding the port. A stray old build answering requests looks exactly
# like a working new one, which is a confusing hour to spend.
#
# Needs JAVA_HOME pointing at a JDK 21, and PostgreSQL up (docker compose up -d).
set -euo pipefail

cd "$(dirname "$0")/.."
JAR="target/collabflow-0.0.1-SNAPSHOT.jar"
PORT="${SERVER_PORT:-8081}"

major_version() {
    "$1" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/'
}

# The build needs Java 21. A machine's default JAVA_HOME is often an older JDK, and running the
# jar with it fails with an UnsupportedClassVersionError that says nothing useful, so look for a
# 21 instead of trusting whatever is first on the path.
find_java() {
    local candidate
    for candidate in "${JAVA_HOME:+$JAVA_HOME/bin/java}" java \
            "$HOME"/.jdks/jdk-21*/bin/java "/c/Program Files/Java/jdk-21"*/bin/java; do
        if [ -n "$candidate" ] && command -v "$candidate" > /dev/null 2>&1 \
                && [ "$(major_version "$candidate")" -ge 21 ] 2>/dev/null; then
            echo "$candidate"
            return 0
        fi
    done
    echo "No Java 21 found. Set JAVA_HOME to a JDK 21 (this project needs it)." >&2
    return 1
}

listening_pid() {
    # The process holding the port, whoever started it.
    netstat -ano 2>/dev/null | grep ":$PORT " | grep LISTENING | head -1 | awk '{print $NF}'
}

case "${1:-}" in
start)
    pid="$(listening_pid || true)"
    if [ -n "$pid" ]; then
        echo "Port $PORT is already taken by process $pid. Run '$0 stop' first."
        exit 1
    fi
    java_bin="$(find_java)" || exit 1
    [ -f "$JAR" ] || JAVA_HOME="$(dirname "$(dirname "$java_bin")")" ./mvnw -q -B -DskipTests package
    "$java_bin" -jar "$JAR" > target/app.log 2>&1 &
    echo -n "starting"
    for _ in $(seq 1 60); do
        if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT/actuator/health")" = "200" ]; then
            echo " - up on http://localhost:$PORT (Swagger at /swagger-ui.html), logging to target/app.log"
            exit 0
        fi
        echo -n "."
        sleep 1
    done
    echo " - it did not come up; see target/app.log"
    exit 1
    ;;
stop)
    pid="$(listening_pid || true)"
    if [ -z "$pid" ]; then
        echo "Nothing is listening on port $PORT."
        exit 0
    fi
    powershell -NoProfile -Command "Stop-Process -Id $pid -Force" 2>/dev/null \
        || kill -9 "$pid" 2>/dev/null
    sleep 1
    echo "Stopped process $pid."
    ;;
status)
    pid="$(listening_pid || true)"
    if [ -z "$pid" ]; then
        echo "Not running (nothing on port $PORT)."
    else
        echo "Process $pid is listening on port $PORT: $(curl -s "http://localhost:$PORT/actuator/health")"
    fi
    ;;
*)
    echo "usage: $0 {start|stop|status}"
    exit 1
    ;;
esac
