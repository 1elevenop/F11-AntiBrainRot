# Creates the F11 release signing key and writes keystore.properties.
# The password is typed here, by you, and only stored in the git-ignored keystore.properties.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$keyDir = Join-Path (Split-Path -Parent $root) 'F11-keys'
$jks = Join-Path $keyDir 'f11-release.jks'
$keytool = 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'

function Plain([securestring] $s) {
    $p = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($p) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p) }
}

Write-Host "`nF11 release signing key" -ForegroundColor Magenta
Write-Host "Key file: $jks`n"
if (Test-Path $jks) {
    Write-Host 'A key already exists there. Nothing was changed.' -ForegroundColor Yellow
    Read-Host 'Press Enter to close'
    exit 0
}
New-Item -ItemType Directory -Force $keyDir | Out-Null

while ($true) {
    $a = Plain (Read-Host 'Choose a password (at least 8 characters)' -AsSecureString)
    $b = Plain (Read-Host 'Type it again' -AsSecureString)
    if ($a -ne $b) { Write-Host "They don't match. Try again.`n" -ForegroundColor Red; continue }
    if ($a.Length -lt 8) { Write-Host "Too short. Try again.`n" -ForegroundColor Red; continue }
    break
}

$env:F11_KS_PASS = $a
try {
    & $keytool -genkeypair -keystore $jks -alias f11 -keyalg RSA -keysize 4096 -validity 10000 `
        -dname 'CN=Jaydeep G., O=1elevenop' -storepass:env F11_KS_PASS -keypass:env F11_KS_PASS
    if ($LASTEXITCODE -ne 0) { throw "keytool failed ($LASTEXITCODE)" }
} finally {
    Remove-Item Env:F11_KS_PASS -ErrorAction SilentlyContinue
}

# Java .properties: escape backslashes.
$escaped = $a.Replace('\', '\\')
@(
    '# Release signing (never commit this file).'
    'storeFile=../F11-keys/f11-release.jks'
    'keyAlias=f11'
    "storePassword=$escaped"
    "keyPassword=$escaped"
) | Set-Content -Encoding ascii (Join-Path $root 'keystore.properties')

Write-Host "`nDone. Key created and keystore.properties filled in." -ForegroundColor Green
Write-Host "BACK UP $jks and your password somewhere safe." -ForegroundColor Yellow
Read-Host 'Press Enter to close'
