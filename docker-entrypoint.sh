#!/bin/sh
set -e

if [ "$1" = "catalina.sh" ]; then
    exec gosu tomcat "$@"
fi
exec "$@"
