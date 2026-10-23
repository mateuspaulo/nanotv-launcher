@echo off
chcp 65001 >nul
title Restaurador TCL Android TV (Voltar ao Padrao de Fabrica)
cls
echo ======================================================================
echo       RESTAURADOR TCL ANDROID TV (REVERTER TODAS AS ALTERACOES)
echo ======================================================================
echo.
echo Este script ira:
echo  1. Reabilitar o launcher oficial do Google TV
echo  2. Reabilitar todos os servicos de fabrica da TCL
echo  3. Restaurar as escalas de animacao para 1.0x (padrao)
echo.

set /p TV_IP="Digite o endereco IP da sua TV (ex: 192.168.1.100): "
if "%TV_IP%"=="" (
    echo [ERRO] IP invalido!
    pause
    exit /b 1
)

echo.
echo [*] Conectando a %TV_IP%:5555 ...
adb connect %TV_IP%:5555

echo.
echo [*] Reabilitando launcher e recomendacoes do Google...
adb -s %TV_IP%:5555 shell "pm enable com.google.android.tvlauncher"
adb -s %TV_IP%:5555 shell "pm enable com.google.android.tvrecommendations"
adb -s %TV_IP%:5555 shell "cmd package set-home-activity com.google.android.tvlauncher/.MainActivity"

echo.
echo [*] Reabilitando servicos da TCL...
adb -s %TV_IP%:5555 shell "pm enable com.tcl.bootadservice"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.bi"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.waterfall.overseas"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.tvweishi"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.appmarket2"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.esticker"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.showmode"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.usercenter"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.common.terminalmanagerplus"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.notereminder"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.messagebox"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.copydatatotv"
adb -s %TV_IP%:5555 shell "pm enable com.tcl.partnercustomizer"

echo.
echo [*] Restaurando escalas de animacao para 1.0x...
adb -s %TV_IP%:5555 shell "settings put global window_animation_scale 1.0"
adb -s %TV_IP%:5555 shell "settings put global transition_animation_scale 1.0"
adb -s %TV_IP%:5555 shell "settings put global animator_duration_scale 1.0"

echo.
echo [*] Abrindo o launcher oficial do Google...
adb -s %TV_IP%:5555 shell "am start -n com.google.android.tvlauncher/.MainActivity"

echo.
echo ======================================================================
echo [SUCESSO] Sistema restaurado para as configuracoes de fabrica!
echo ======================================================================
pause

