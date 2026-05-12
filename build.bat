@echo off
setlocal

cd /d "%~dp0"

echo.
echo [Discocraft] Cleaning and building plugin...
echo.

call gradlew.bat clean build
if errorlevel 1 (
    echo.
    echo [Discocraft] Build failed.
    exit /b 1
)

echo.
echo [Discocraft] Build complete.
echo [Discocraft] Output: build\libs\Discocraft-1.0-SNAPSHOT.jar
echo.

endlocal
