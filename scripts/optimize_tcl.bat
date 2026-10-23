@echo off
chcp 65001 >nul
title Otimizador TCL Android TV + NanoTV Launcher
cls
echo ======================================================================
echo          OTIMIZADOR TCL ANDROID TV + NANOTV LAUNCHER
echo ======================================================================
echo.
echo Este script ira:
echo  1. Conectar a sua TV via rede (ADB)
echo  2. Instalar o NanoTV Launcher (0%% CPU, ultraleve)
echo  3. Definir o NanoTV como tela inicial padrao
echo  4. Desativar telemetria pesada e propagandas de fabrica da TCL
echo  5. Acelerar as animacoes do sistema para resposta imediata no controle
echo.
echo [!] Certifique-se de que a TV esta ligada na mesma rede que o computador
echo [!] Ative a 'DepuraÃ§Ã£o USB' nas Opcoes de Desenvolvedor da TV
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
echo [!] Caso apareca uma mensagem na tela da TV pedindo 'Permitir depuracao', marque 'Sempre' e clique em OK.
timeout /t 3 >nul

echo.
echo [*] Instalando o NanoTV Launcher...
if exist "..\releases\NanoTV-Launcher-v1.1.0.apk" (
    adb -s %TV_IP%:5555 install -r "..\releases\NanoTV-Launcher-v1.1.0.apk"
) else if exist "releases\NanoTV-Launcher-v1.1.0.apk" (
    adb -s %TV_IP%:5555 install -r "releases\NanoTV-Launcher-v1.1.0.apk"
) else if exist "..\releases\NanoTV-Launcher-v1.0.apk" (
    adb -s %TV_IP%:5555 install -r "..\releases\NanoTV-Launcher-v1.0.apk"
) else if exist "releases\NanoTV-Launcher-v1.0.apk" (
    adb -s %TV_IP%:5555 install -r "releases\NanoTV-Launcher-v1.0.apk"
) else (
    for %%f in ("..\releases\*.apk" "releases\*.apk" "*.apk") do (
        echo [*] Instalando %%f ...
        adb -s %TV_IP%:5555 install -r "%%f"
        goto :apk_installed
    )
    echo [AVISO] APK nao encontrado na pasta releases. Baixe o APK das Releases do GitHub.
)
:apk_installed

echo.
echo [*] Configurando NanoTV como launcher padrao...
adb -s %TV_IP%:5555 shell "cmd package set-home-activity com.nanotv.launcher/.MainActivity"

echo.
echo [*] Desativando launcher pesado e propagandas do Google TV...
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.google.android.tvlauncher"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.google.android.tvrecommendations"

echo.
echo [*] Desativando telemetria e bloatware de fabrica da TCL...
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.bootadservice"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.bi"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.waterfall.overseas"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.tvweishi"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.appmarket2"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.esticker"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.showmode"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.usercenter"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.common.terminalmanagerplus"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.notereminder"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.messagebox"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.copydatatotv"
adb -s %TV_IP%:5555 shell "pm disable-user --user 0 com.tcl.partnercustomizer"

echo.
echo [*] Acelerando velocidade de animacao das janelas (0.5x)...
adb -s %TV_IP%:5555 shell "settings put global window_animation_scale 0.5"
adb -s %TV_IP%:5555 shell "settings put global transition_animation_scale 0.5"
adb -s %TV_IP%:5555 shell "settings put global animator_duration_scale 0.5"

echo.
echo [*] Abrindo o NanoTV Launcher na TV...
adb -s %TV_IP%:5555 shell "am start -n com.nanotv.launcher/.MainActivity"
adb -s %TV_IP%:5555 shell "input keyevent KEYCODE_HOME"

echo.
echo ======================================================================
echo [SUCESSO] TV otimizada com sucesso!
echo Aperte o botao Home no controle da TV para conferir a nova fluidez.
echo ======================================================================
pause

