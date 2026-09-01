#!/usr/bin/env bash
# Downloads Temurin JDK 21 into tools/jdk-21, local to this project only.
# Does NOT touch system PATH / JAVA_HOME / the currently installed JDK 8.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jdk_dir="$root/tools/jdk-21"

if compgen -G "$jdk_dir/*/bin/java" > /dev/null 2>&1; then
  echo "JDK 21 already present under $jdk_dir — skipping download."
  exit 0
fi

mkdir -p "$jdk_dir"
zip_path="$root/tools/temurin-21-jdk-windows-x64.zip"

echo "Downloading Temurin JDK 21 (Windows x64)..."
curl -L -o "$zip_path" "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"

echo "Extracting..."
unzip -q "$zip_path" -d "$jdk_dir"
rm "$zip_path"

echo "Done. JDK 21 installed under $jdk_dir (project-local only)."
