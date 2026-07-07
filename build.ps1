# =================================================================
# UNIVERSAL BUILD SCRIPT - Works from any directory
# Place in PATH and call as: buildplugin
# =================================================================

# =================================================================
# 1. USER PROMPT (CHOOSE BUILD TOOL)
# =================================================================
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INPUT" -ForegroundColor Cyan -NoNewline; Write-Host "] " -ForegroundColor DarkGray -NoNewline; 
$choice = Read-Host "Type 'm' for Maven, 'g' for Gradle, 'gw' for Gradle Wrapper, 'v' to check versions, 'c' to delete build/ target/ .gradle/ gradle/"

# Validate input
if ($choice -eq "v") {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Checking Java Version..." -ForegroundColor Cyan
    java -version
    
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Checking Gradle Version..." -ForegroundColor Cyan
    gradle -v
    
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Checking Maven Version..." -ForegroundColor Cyan
    mvn -version
    
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Checking Gradle Wrapper Version..." -ForegroundColor Cyan
    
    # Safe check for Gradle Wrapper to handle missing or broken jar files gracefully
    if (Test-Path "gradlew.bat") {
        try {
            # Redirecting standard error so native Java crashes can be caught cleanly
            & .\gradlew.bat -version 2>$null
            if ($LASTEXITCODE -ne 0) { throw }
        } catch {
            Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Gradle Wrapper files are corrupted or missing gradle-wrapper.jar!" -ForegroundColor Red
        }
    } else {
        Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " gradlew.bat not found in current directory." -ForegroundColor Yellow
    }
    Exit 0
}

if ($choice -eq "c") {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Cleaning up folders..." -ForegroundColor Cyan
    Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -Path "gradle" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Cleanup complete!" -ForegroundColor Green
    Exit 0
}

if ($choice -ne "m" -and $choice -ne "g" -and $choice -ne "gw") {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Invalid choice. Exiting script." -ForegroundColor Red
    Exit 1
}

# =================================================================
# 2. CLEANUP (DELETE TARGET & BUILD FOLDERS)
# =================================================================
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ===================================" -ForegroundColor Yellow
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Cleaning up old build files..." -ForegroundColor Yellow
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Removing build and target folders" -ForegroundColor Yellow
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ===================================" -ForegroundColor Yellow

# Force delete both directories silently if they exist (relative to current directory)
Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue

# =================================================================
# 3. EXECUTION
# =================================================================
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " =========================" -ForegroundColor Green
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " - starting executor..."
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " - Done!" -ForegroundColor Green
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " =========================" -ForegroundColor Green

$buildExitCode = 0
$toolName = ""

if ($choice -eq "g") {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Running Gradle Build..." -ForegroundColor Cyan
    $toolName = "GRADLE"
    try {
        Write-Host "Checking Version Please Wait..." -ForegroundColor Yellow
        & gradle -v
        Write-Host "Done Checking Version!" -ForegroundColor Green
        & gradle build
        $buildExitCode = $LASTEXITCODE
    } catch {
        $buildExitCode = 1
    }
} elseif ($choice -eq "gw") {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Running Gradle Wrapper Build..." -ForegroundColor Cyan
    $toolName = "GRADLE WRAPPER"
    try {
        $needsGeneration = $false

        if (Test-Path "gradlew.bat") {
            Write-Host "Verifying existing wrapper..." -ForegroundColor Yellow
            # Check wrapper safely here as well
            try {
                & .\gradlew.bat -v 2>$null
                if ($LASTEXITCODE -ne 0) { $needsGeneration = $true }
            } catch {
                $needsGeneration = $true
            }
        } else {
            $needsGeneration = $true
        }

        if ($needsGeneration) {
            Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Wrapper error or missing. Initializing wrapper version 9.6.0..." -ForegroundColor Yellow
            & gradle wrapper --gradle-version 9.6.0 --no-daemon
        }

        Write-Host "Executing wrapper build..." -ForegroundColor Yellow
        & .\gradlew.bat build --no-daemon
        $buildExitCode = $LASTEXITCODE
    } catch {
        $buildExitCode = 1
    }
} else {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Running Maven Build..." -ForegroundColor Cyan
    $toolName = "MAVEN"
    try {
        Write-Host "Checking Version Please Wait..." -ForegroundColor Yellow
        & mvn -version
        Write-Host "Done Checking Version!" -ForegroundColor Green
        & mvn clean package
        $buildExitCode = $LASTEXITCODE
    } catch {
        $buildExitCode = 1
    }
}

# =================================================================
# 4. POST-BUILD SUMMARY
# =================================================================
if ($buildExitCode -eq 0) {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " EXECUTION COMPLETE!" -ForegroundColor Green
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " =============================================================" -ForegroundColor Green
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " DONE! | $toolName HAS ALL INFOs ERRORS ARE NOT DISPLAYED HERE!" -ForegroundColor Green
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ============================================================" -ForegroundColor Green
} else {
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " BUILD FAILED!" -ForegroundColor Red
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " =============================================================" -ForegroundColor Red
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " FAILED! | $toolName BUILD ENCOUNTERED ERRORS! (Exit Code: $buildExitCode)" -ForegroundColor Red
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Check the output above for details." -ForegroundColor Red
    Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ============================================================" -ForegroundColor Red
    Exit $buildExitCode
}