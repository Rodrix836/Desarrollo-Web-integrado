param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('auth', 'residuos', 'consultas')]
    [string]$Servicio
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location $raiz
if (Test-Path 'C:/Program Files/Eclipse Adoptium/jdk-21.0.3.9-hotspot/bin/javac.exe') {
    $jdk = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.3.9-hotspot'
    if (Test-Path "$jdk/bin/javac.exe") { $env:JAVA_HOME = $jdk }
}
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME/bin;$env:Path" }
$local = Join-Path $raiz '.local'
New-Item -ItemType Directory -Path $local -Force | Out-Null
$archivoClave = Join-Path $local 'jwt-secret.txt'
if (-not (Test-Path $archivoClave)) {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    try {
        $stream = [IO.File]::Open($archivoClave, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        try {
            $contenido = [Text.Encoding]::UTF8.GetBytes([Convert]::ToBase64String($bytes))
            $stream.Write($contenido, 0, $contenido.Length)
        } finally { $stream.Dispose() }
    } catch [IO.IOException] {
        if (-not (Test-Path $archivoClave)) { throw }
    }
}
for ($intento = 0; $intento -lt 20; $intento++) {
    try {
        $env:RECICLO_JWT_SECRET = [IO.File]::ReadAllText($archivoClave).Trim()
        if ($env:RECICLO_JWT_SECRET.Length -ge 32) { break }
    } catch [IO.IOException] { }
    Start-Sleep -Milliseconds 100
}
$env:SERVER_PORT = @{ auth = '8081'; residuos = '8082'; consultas = '8083' }[$Servicio]
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SPRING_PROFILES_ACTIVE = ''
if ($env:RECICLO_JWT_SECRET.Length -lt 32) { throw 'La clave local está vacía o incompleta.' }
# Cada servicio recibe un directorio de datos propio.
$carpetaDatos = (Join-Path $raiz "$Servicio-api/data").Replace('\', '/')
if ($Servicio -eq 'auth') {
    $env:SPRING_DATASOURCE_URL = "jdbc:h2:file:$carpetaDatos/authdb"
} elseif ($Servicio -eq 'residuos') {
    $env:SPRING_DATASOURCE_URL = "jdbc:h2:file:$carpetaDatos/residuosdb"
    $env:SPRING_PROFILES_ACTIVE = 'demo'
}
& mvn.cmd -f "$Servicio-api/pom.xml" spring-boot:run
exit $LASTEXITCODE
