@echo off
setlocal

cd /d "%~dp0"

if not defined DATABASE_URL (
    echo DATABASE_URL must be set to your Supabase connection URL.
    echo.
    echo PowerShell example:
    echo   $env:DATABASE_URL='postgres://postgres.PROJECT_REF@aws-0-REGION.pooler.supabase.com:5432/postgres'
    echo   $env:DATABASE_USERNAME='postgres.PROJECT_REF'
    echo   $env:DATABASE_PASSWORD='PASSWORD'
    echo   .\run-web.bat
    exit /b 1
)

echo Starting with the configured online PostgreSQL database.
echo Open http://localhost:8080 after the server starts.
call .\mvnw.cmd spring-boot:run
