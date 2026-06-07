# Сборка приложения в portable .exe (со встроенной JRE).
# Запуск из корня проекта:  powershell -ExecutionPolicy Bypass -File build-exe.ps1
# Результат: target\dist\volonterHoursAPP\volonterHoursAPP.exe

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# --- Найти JDK 17+ с jpackage ---
function Find-Jdk {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $candidates += Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
                   Sort-Object Name -Descending | ForEach-Object { $_.FullName }
    $candidates += Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
                   Sort-Object Name -Descending | ForEach-Object { $_.FullName }
    foreach ($c in $candidates) {
        if (Test-Path (Join-Path $c "bin\jpackage.exe")) { return $c }
    }
    throw "Не найден JDK с jpackage (нужен JDK 17+). Установите его и/или задайте JAVA_HOME."
}

$jdk = Find-Jdk
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"
Write-Host "JDK: $jdk"

$appJar = "volonterHoursAPP-1.0-SNAPSHOT.jar"

# --- 1. Сборка jar + копирование зависимостей ---
& .\mvnw.cmd -q package -DskipTests
& .\mvnw.cmd -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies `
    "-DoutputDirectory=target\app-input" "-DincludeScope=runtime"
Copy-Item "target\$appJar" "target\app-input\" -Force

# --- 2. Своя JRE через jlink ---
if (Test-Path "target\runtime") { Remove-Item "target\runtime" -Recurse -Force }
& "$jdk\bin\jlink.exe" --module-path "$jdk\jmods" `
    --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.zipfs,jdk.localedata `
    --output "target\runtime" --strip-debug --no-header-files --no-man-pages

# --- 3. Упаковка в .exe (app-image) ---
if (Test-Path "target\dist") { Remove-Item "target\dist" -Recurse -Force }
& "$jdk\bin\jpackage.exe" --type app-image --name volonterHoursAPP `
    --input "target\app-input" --main-jar $appJar `
    --main-class org.example.volonterhoursapp.Launcher `
    --runtime-image "target\runtime" --dest "target\dist" `
    --java-options "-Dfile.encoding=UTF-8"

# --- 4. Положить рядом настройки подключения к БД ---
if (Test-Path "db-config.txt") {
    Copy-Item "db-config.txt" "target\dist\volonterHoursAPP\db-config.txt" -Force
}

