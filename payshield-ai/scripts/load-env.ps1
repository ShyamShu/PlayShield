
$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    throw ".env not found at $envFile"
}

Get-Content $envFile | ForEach-Object {
    $line = $_

    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$' -and
        $line -notmatch '^\s*#') {

        $key = $matches[1]
        $value = $matches[2].Trim()

        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
             ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable(
            $key, $value, "Process"
        )
    }
}

Write-Host "Loaded .env variables into this PowerShell process."
Write-Host "Identity password loaded: $([bool]$env:IDENTITY_DB_PASSWORD)"
cls
