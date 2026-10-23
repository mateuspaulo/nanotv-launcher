param(
    [string]$IP
)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "         OTIMIZADOR TCL ANDROID TV + NANOTV LAUNCHER" -ForegroundColor Cyan
Write-Host "======================================================================`n"

if (-not $IP) {
    $IP = Read-Host "Digite o endereco IP da sua TV TCL (ex: 192.168.1.100)"
}

if (-not $IP) {
    Write-Error "Endereco IP obrigatorio!"
    exit 1
}

$target = "$($IP):5555"
Write-Host "`n[*] Conectando a $target ..." -ForegroundColor Yellow
adb connect $target

Write-Host "`n[*] Instalando NanoTV Launcher..." -ForegroundColor Yellow
$apkCandidates = @(
    (Join-Path $PSScriptRoot "..\releases\NanoTV-Launcher-v1.1.0.apk"),
    (Join-Path $PSScriptRoot "..\releases\NanoTV-Launcher-v1.0.apk")
)
$apkPath = $null
foreach ($cand in $apkCandidates) {
    if (Test-Path $cand) { $apkPath = $cand; break }
}
if (-not $apkPath) {
    $found = Get-ChildItem -Path (Join-Path $PSScriptRoot "..\releases") -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $apkPath = $found.FullName }
}

if ($apkPath -and (Test-Path $apkPath)) {
    Write-Host "[+] Instalando: $apkPath" -ForegroundColor Green
    adb -s $target install -r $apkPath
} else {
    Write-Warning "APK nao encontrado na pasta releases. Certifique-se de baixar o release."
}

Write-Host "`n[*] Definindo NanoTV como launcher padrao..." -ForegroundColor Yellow
adb -s $target shell "cmd package set-home-activity com.nanotv.launcher/.MainActivity"

Write-Host "`n[*] Desativando launcher pesado e recomendacoes do Google..." -ForegroundColor Yellow
adb -s $target shell "pm disable-user --user 0 com.google.android.tvlauncher"
adb -s $target shell "pm disable-user --user 0 com.google.android.tvrecommendations"

Write-Host "`n[*] Desativando bloatware e telemetria da TCL..." -ForegroundColor Yellow
$tclBloat = @(
    "com.tcl.bootadservice",
    "com.tcl.bi",
    "com.tcl.waterfall.overseas",
    "com.tcl.tvweishi",
    "com.tcl.appmarket2",
    "com.tcl.esticker",
    "com.tcl.showmode",
    "com.tcl.usercenter",
    "com.tcl.common.terminalmanagerplus",
    "com.tcl.notereminder",
    "com.tcl.messagebox",
    "com.tcl.copydatatotv",
    "com.tcl.partnercustomizer"
)
foreach ($pkg in $tclBloat) {
    adb -s $target shell "pm disable-user --user 0 $pkg"
}

Write-Host "`n[*] Acelerando animacoes do sistema (0.5x)..." -ForegroundColor Yellow
adb -s $target shell "settings put global window_animation_scale 0.5"
adb -s $target shell "settings put global transition_animation_scale 0.5"
adb -s $target shell "settings put global animator_duration_scale 0.5"

Write-Host "`n[*] Abrindo NanoTV Launcher..." -ForegroundColor Yellow
adb -s $target shell "am start -n com.nanotv.launcher/.MainActivity"
adb -s $target shell "input keyevent KEYCODE_HOME"

Write-Host "`n[SUCESSO] TV otimizada com sucesso! Aproveite a nova fluidez.`n" -ForegroundColor Green

