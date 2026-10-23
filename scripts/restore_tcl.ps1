param(
    [string]$IP
)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "      RESTAURADOR TCL ANDROID TV (REVERTER CONFIGURACOES)" -ForegroundColor Cyan
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

Write-Host "`n[*] Reabilitando launcher do Google..." -ForegroundColor Yellow
adb -s $target shell "pm enable com.google.android.tvlauncher"
adb -s $target shell "pm enable com.google.android.tvrecommendations"

Write-Host "`n[*] Reabilitando servicos da TCL..." -ForegroundColor Yellow
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
    adb -s $target shell "pm enable $pkg"
}

Write-Host "`n[*] Restaurando escalas de animacao para 1.0x..." -ForegroundColor Yellow
adb -s $target shell "settings put global window_animation_scale 1.0"
adb -s $target shell "settings put global transition_animation_scale 1.0"
adb -s $target shell "settings put global animator_duration_scale 1.0"

Write-Host "`n[*] Abrindo launcher do Google..." -ForegroundColor Yellow
adb -s $target shell "am start -n com.google.android.tvlauncher/.MainActivity"

Write-Host "`n[SUCESSO] TV restaurada para as configuracoes de fabrica com sucesso.`n" -ForegroundColor Green

