@echo off
REM Windows-compatible frontend startup script with proper escaping

setlocal EnableDelayedExpansion=Disabled

REM Get the directory where the script is located
for %%i in ("%~dp0") do set scriptPath=%%~dp0
if not defined scriptPath set scriptPath=%~dp0
cd /d "%scriptPath%"

echo ========================================
echo Working Directory: %scriptPath%
echo ========================================

REM Check if node_modules exists
if exist "node_modules" (
    echo [1mFound node_modules directory - skipping npm install[0m
) else (
    echo [1mnode_modules not found - running npm install...[0m
    call npm install
)

REM Check if npm is available
if "%npm_available%"=="" (
    echo Checking for npm...
    where npm.cmd > nul.txt
    set /p npm_available=Error
    for /f "tokens=*" delims=," %%a in ('npm.cmd') do (
        set npm_available=Found
    )
)

if "%npm_available%"=="Found" (
    echo [32mFound npm - starting frontend...[0m

    REM Run npm in background
    start /b npm.cmd run dev > ..\..\frontend.log 2>&1

    echo [32mFrontend should be starting on port 5173[0m
    echo [36mPlease access: http://localhost:5173[0m
    echo [32mPress Ctrl+C to stop[0m
) else (
    echo [31mERROR: npm.cmd not found![0m
    echo [31mPlease install Node.js and npm on Windows first[0m
    echo [31mDownload from: https://nodejs.org/[0m
)

echo ========================================
echo.
