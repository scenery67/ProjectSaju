#!/usr/bin/env bash
# Runs the Spring Boot backend using the project-local JDK 21, without
# changing the machine's system JAVA_HOME/PATH (Java 8 stays the system default).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jdk_home="$(dirname "$(dirname "$(compgen -G "$root/tools/jdk-21/*/bin/java" | head -n1)")")"

if [ -z "$jdk_home" ]; then
  echo "JDK 21 not found under tools/jdk-21. Run scripts/setup-jdk21.sh first." >&2
  exit 1
fi

export JAVA_HOME="$jdk_home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using JAVA_HOME=$JAVA_HOME (this process only)"
cd "$root/backend"
./gradlew.bat bootRun
