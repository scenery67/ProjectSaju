# Runs the Spring Boot backend using the project-local JDK 21, without
# changing the machine's system JAVA_HOME/PATH (Java 8 stays the system default).
# JAVA_HOME is set only for this process, so it never leaks to other terminals/apps.
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jdkHome = Get-ChildItem -Path (Join-Path $root "tools\jdk-21") -Directory -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $jdkHome) {
    Write-Error "JDK 21 not found under tools\jdk-21. Run scripts\setup-jdk21.ps1 first."
    exit 1
}

$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

Write-Host "Using JAVA_HOME=$jdkHome (this process only)"
Set-Location (Join-Path $root "backend")
& .\gradlew.bat bootRun
