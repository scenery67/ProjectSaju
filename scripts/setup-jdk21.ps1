# Downloads Temurin JDK 21 into tools/jdk-21, local to this project only.
# Does NOT touch system PATH / JAVA_HOME / the currently installed JDK 8.
# 이 프로젝트 전용 폴더에만 JDK21을 설치한다. 시스템 PATH/JAVA_HOME(Java 8)은 건드리지 않는다.
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jdkDir = Join-Path $root "tools\jdk-21"

if (Test-Path (Join-Path $jdkDir "*\bin\java.exe")) {
    Write-Host "JDK 21 already present under $jdkDir — skipping download."
    exit 0
}

New-Item -ItemType Directory -Force -Path $jdkDir | Out-Null
$zipPath = Join-Path $root "tools\temurin-21-jdk-windows-x64.zip"

Write-Host "Downloading Temurin JDK 21 (Windows x64)..."
Invoke-WebRequest -Uri "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile $zipPath

Write-Host "Extracting..."
Expand-Archive -Path $zipPath -DestinationPath $jdkDir -Force
Remove-Item $zipPath

Write-Host "Done. JDK 21 installed under $jdkDir (project-local only)."
