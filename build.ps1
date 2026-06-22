Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ===================================" -ForegroundColor Yellow
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Cleaning up old build files..." -ForegroundColor Yellow
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " build.ps1 | build.gradle" -ForegroundColor Yellow
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "WARN" -ForegroundColor Yellow -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ===================================" -ForegroundColor Yellow
Remove-Item -Path ".\build" -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Green -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ------< EXECUTING >------" -ForegroundColor Green
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Green -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " Running Gradle Build..." -ForegroundColor Cyan
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Green -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " EXECUTION COMPLETE!" -ForegroundColor Green
gradle build

Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Green -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " =============================================================" -ForegroundColor Green
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Green -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " DONE! | GRADLE HAS ALL INFOs ERRORS ARE NOT DISPLAYED HERE!" -ForegroundColor Green
Write-Host "[" -ForegroundColor DarkGray -NoNewline; Write-Host "INFO" -ForegroundColor Green -NoNewline; Write-Host "]" -ForegroundColor DarkGray -NoNewline; Write-Host " ============================================================" -ForegroundColor Green