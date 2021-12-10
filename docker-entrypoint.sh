#!/bin/sh
set -e
export JAVA_OPTS="-Dlog4j2.formatMsgNoLookups=true"

if [ "$1" = "catalina.sh" ]; then
    gosu tomcat mkdir -p /var/log/chatnoir2/query_logs
    exec gosu tomcat "$@"
fi
exec "$@"
