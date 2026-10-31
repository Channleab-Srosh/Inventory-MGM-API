# ==============================================================================
# Reset Inventory Management Demo Data
# ==============================================================================
Write-Host "Resetting Inventory Management Database to pristine demo state..." -ForegroundColor Cyan

$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "inventory_management_db" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }

if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = "pgsql"
}

$scriptPath = Join-Path $PSScriptRoot "..\src\main\resources\db\seed\demo_data.sql"
if (-not (Test-Path $scriptPath)) {
    Write-Error "Seed script not found at $scriptPath"
    exit 1
}

psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $scriptPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nSuccessfully reset demo data for all presentation cases!" -ForegroundColor Green
} else {
    Write-Host "`nFailed to execute demo_data.sql. Please verify PostgreSQL connection." -ForegroundColor Red
}
