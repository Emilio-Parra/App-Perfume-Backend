# Inicio local: solicita secretos en cada ejecucion sin guardarlos en GitHub.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path '.\mvnw.cmd')) {
    throw 'Este archivo debe estar en la carpeta del backend, junto a mvnw.cmd.'
}

$claveDb = Read-Host 'Clave MySQL de tienda_app' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $claveDb).Password
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw 'Es necesaria la clave del usuario tienda_app.'
}

$env:ADMIN_EMAIL = 'admin@duoc.cl'
$claveAdmin = Read-Host 'Clave inicial del administrador (Enter si ya existe)' -AsSecureString
$textoAdmin = [System.Net.NetworkCredential]::new('', $claveAdmin).Password
if ([string]::IsNullOrWhiteSpace($textoAdmin)) {
    Remove-Item Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
} else {
    $env:ADMIN_PASSWORD = $textoAdmin
}




# Conservar la clave JWT entre reinicios sin publicarla en GitHub.
$rutaJwt = Join-Path $PSScriptRoot '.env.jwt.local'

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    if (Test-Path $rutaJwt) {
        $env:JWT_SECRET = (Get-Content $rutaJwt -Raw).Trim()
    }
    else {
        $bytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $rng.GetBytes($bytes)
        }
        finally {
            $rng.Dispose()
        }

        $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
        [System.IO.File]::WriteAllText($rutaJwt, $env:JWT_SECRET)
    }
}



Write-Host 'Iniciando Spring Boot. Si la tabla productos esta vacia, se cargara el catalogo inicial.'
& .\mvnw.cmd spring-boot:run
