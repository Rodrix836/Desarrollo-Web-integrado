param(
    [ValidateSet('ADMIN', 'USER')]
    [string]$Rol = 'ADMIN'
)

$secreto = $env:RECICLO_JWT_SECRET
if ([string]::IsNullOrWhiteSpace($secreto) -or [Text.Encoding]::UTF8.GetByteCount($secreto) -lt 32) {
    throw 'Configura RECICLO_JWT_SECRET con la misma clave efímera de la API local (mínimo 32 bytes).'
}

function Convertir-Base64Url([byte[]]$Bytes) {
    [Convert]::ToBase64String($Bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$encabezado = Convertir-Base64Url ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
$contenido = @{
    sub = 'cuenta-demo-local'
    rol = $Rol
    exp = [DateTimeOffset]::UtcNow.AddMinutes(15).ToUnixTimeSeconds()
} | ConvertTo-Json -Compress

$datos = "$encabezado." + (Convertir-Base64Url ([Text.Encoding]::UTF8.GetBytes($contenido)))
$hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secreto))
try {
    $firma = Convertir-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($datos)))
    "$datos.$firma"
} finally {
    $hmac.Dispose()
}
