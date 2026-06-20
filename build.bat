@echo off
REM Get the directory where this script is located
setlocal enabledelayedexpansion
set "scriptPath=%~dp0"
set "targetFolder=%scriptPath%target"

REM Delete the target folder first
if exist "%targetFolder%" (
    echo Deleting target folder...
    rmdir /s /q "%targetFolder%"
    echo Target folder deleted.
) else (
    echo Target folder not found.
)

REM Run Maven clean package
echo Building project...
call mvn clean package

echo Build complete.
pause