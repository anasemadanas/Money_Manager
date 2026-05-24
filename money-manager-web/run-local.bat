@echo off
setlocal

cd /d "%~dp0"

if not defined LOCAL_DATABASE_URL set "LOCAL_DATABASE_URL=jdbc:postgresql://localhost:5432/moneymanager"
if not defined LOCAL_DATABASE_USERNAME set "LOCAL_DATABASE_USERNAME=postgres"
if not defined LOCAL_DATABASE_PASSWORD set "LOCAL_DATABASE_PASSWORD=123456"

set "DATABASE_URL=%LOCAL_DATABASE_URL%"
set "DATABASE_USERNAME=%LOCAL_DATABASE_USERNAME%"
set "DATABASE_PASSWORD=%LOCAL_DATABASE_PASSWORD%"

echo Starting with local PostgreSQL at %DATABASE_URL%
echo Open http://localhost:8080 after the server starts.
call .\mvnw.cmd spring-boot:run
