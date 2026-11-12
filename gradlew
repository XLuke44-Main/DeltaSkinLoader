#!/bin/sh

JAVA_HOME="/home/xluke44/BIN/JAVA/jdk-17.0.19+10"
JAVACMD="$JAVA_HOME/bin/java"
export JAVA_HOME

APP_PATH=$0

while [ -h "$APP_PATH" ]; do
    APP_DIR=$(cd -P "$(dirname "$APP_PATH")" >/dev/null 2>&1 && pwd) || exit 1
    LINK=$(readlink "$APP_PATH")

    case "$LINK" in
        /*) APP_PATH=$LINK ;;
        *)  APP_PATH=$APP_DIR/$LINK ;;
    esac
done

APP_HOME=$(cd -P "$(dirname "$APP_PATH")" >/dev/null 2>&1 && pwd) || exit 1
APP_BASE_NAME=${0##*/}

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

warn() {
    echo "$*" >&2
}

die() {
    echo >&2
    echo "$*" >&2
    echo >&2
    exit 1
}

if [ ! -x "$JAVACMD" ]; then
    die "ERROR: Java executable not found or not executable:
  $JAVACMD

Update JAVA_HOME near the top of gradlew to your installed JDK 17 path."
fi

MAX_FD=maximum

case "$MAX_FD" in
    max*)
        MAX_FD=$(ulimit -H -n) ||
            warn "Could not query maximum file descriptor limit"
        ;;
esac

case "$MAX_FD" in
    '' | soft)
        ;;
    *)
        ulimit -n "$MAX_FD" ||
            warn "Could not set maximum file descriptor limit to $MAX_FD"
        ;;
esac

set -- \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"

if ! command -v xargs >/dev/null 2>&1; then
    die "xargs is not available"
fi

eval "set -- $(
    printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed 's~[^-[:alnum:]+,./:=@_]~\\&~g;' |
        tr '\n' ' '
)" '"$@"'

exec "$JAVACMD" "$@"

