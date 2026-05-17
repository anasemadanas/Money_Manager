@echo off
setlocal

cd /d "%~dp0"

call .\mvnw.cmd clean package -DskipTests
if errorlevel 1 exit /b 1

java -jar target\moneytracker-0.0.1-SNAPSHOT.jar
