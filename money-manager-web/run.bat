@echo off
setlocal

cd /d "%~dp0"

:: Use Supabase or any PostgreSQL URL here. If DATABASE_URL includes user/password, DATABASE_USERNAME and DATABASE_PASSWORD are optional.
if not defined DATABASE_URL (
    set "DATABASE_URL=jdbc:postgresql://localhost:5432/moneymanager"
)

if not defined DATABASE_USERNAME (
    set "DATABASE_USERNAME=postgres"
)

if not defined DATABASE_PASSWORD (
    set "DATABASE_PASSWORD=123456"
)

echo Using database: %DATABASE_URL%
echo Using database user: %DATABASE_USERNAME%

echo Open http://localhost:8080 after the server starts.
call .\mvnw.cmd spring-boot:run
pause
