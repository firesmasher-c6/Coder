# Get the directory where this script is located
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetFolder = Join-Path $scriptPath "target"

# Delete the target folder first
if (Test-Path $targetFolder) {
    Write-Host "Deleting target folder..." -ForegroundColor Yellow
    Remove-Item -Path $targetFolder -Recurse -Force
    Write-Host "Target folder deleted." -ForegroundColor Green
} else {
    Write-Host "Target folder not found." -ForegroundColor Yellow
}

# Run Maven clean package
Write-Host "Building project..." -ForegroundColor Cyan
mvn clean package

Write-Host "Build complete." -ForegroundColor Green