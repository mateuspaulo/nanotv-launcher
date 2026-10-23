# NanoTV Launcher ðŸ“ºâš¡
> **Launcher ultraleve, moderno e sem anÃºncios para Smart TVs Android (com foco especial na linha TCL / 50P8M e dispositivos de hardware modesto).**

![NanoTV Launcher Preview](assets/preview.png)

---

## ðŸ’¡ Por que este projeto existe?

Muitas Smart TVs Android (como a **TCL 50P8M**, modelos Philips, Philco e diversas TV Boxes) possuem hardware modesto: processadores quad-core de baixo consumo e **apenas 2 GB de memÃ³ria RAM**. 

O launcher padrÃ£o oficial do Google TV e as customizaÃ§Ãµes de fÃ¡brica dessas TVs pesam excessivamente no sistema:
* **Google TV Launcher:** consome atÃ© **90% de CPU** em picos e mais de **250 MB de RAM**, carregando trailers de vÃ­deo automÃ¡ticos, propagandas e sincronizaÃ§Ã£o contÃ­nua de canais patrocinados em segundo plano.
* **Telemetria de FÃ¡brica:** serviÃ§os em segundo plano (como *TCL BI*, *Waterfall Ads*, *TV Guard*) competem pela CPU e causam engasgos severos, congelamentos no controle remoto e lentidÃ£o ao abrir aplicativos.

O **NanoTV Launcher** foi desenvolvido em **Java/Views nativas puras (XML + RecyclerView)**, sem bibliotecas pesadas de runtime, trazendo a TV de volta Ã  fluidez que ela deveria ter.

---

## âœ¨ Recursos Principais

* âš¡ **0% de Uso de CPU em Repouso:** A interface Ã© estÃ¡tica e instantÃ¢nea, liberando os nÃºcleos do processador para os seus filmes e vÃ­deos.
* ðŸ§  **Uso MÃ­nimo de RAM (< 20 MB):** ReduÃ§Ã£o de mais de **90%** comparado ao launcher oficial do Google.
* ðŸ“Š **MÃ©tricas de Hardware em Tempo Real:** Exibe no topo o consumo de **RAM**, **EspaÃ§o em Disco Livre** e **Uso de CPU**. *(Pode ser ocultado a qualquer momento no menu de ConfiguraÃ§Ãµes)*.
* ðŸ”Œ **Indicador de ConexÃ£o com a Internet:** Identifica automaticamente se a TV estÃ¡ conectada via **Cabo (Ethernet)** ou **Wi-Fi** (com nome da rede). Ao clicar nele com o controle, abre diretamente a tela de conexÃµes da TV.
* ðŸŽ›ï¸ **Seletor Real de Entradas FÃ­sicas:** Menu limpo com as portas reais de hardware (**HDMI 1**, **HDMI 2**, **HDMI 3**, **AV** e **TV Aberta/Antena**).
* ðŸ”€ **OrganizaÃ§Ã£o e PosiÃ§Ã£o Salva Permanentemente:** Mova os aplicativos para onde quiser (esquerda, direita ou inÃ­cio). A ordem fica gravada no armazenamento interno e nunca se perde ao desligar a TV.
* â­ **Linha de Favoritos:** Fixe os seus aplicativos mais usados no topo.
* ðŸ› ï¸ **Menu Completo de Gerenciamento:** Segure a tecla **[OK]** em qualquer app para favoritar, mover de posiÃ§Ã£o, **desinstalar** ou **ocultar** da tela.
* ðŸ“œ **Barra de Rolagem Lateral VisÃ­vel:** Indica claramente que hÃ¡ mais aplicativos abaixo.
* ðŸ“± **Suporte a Apps Sideload:** Exibe todos os aplicativos instalados, inclusive aqueles que o Android TV padrÃ£o esconde por nÃ£o terem banner de TV.
* ðŸš« **Zero Propagandas e Zero Telemetria:** Nenhuma conexÃ£o externa desnecessÃ¡ria.

---

## ðŸš€ Como Instalar (Passo a Passo RÃ¡pido)

### PrÃ©-requisitos:
1. Sua TV e seu computador devem estar conectados na **mesma rede local** (via Wi-Fi ou Cabo de rede).
2. Ter o [ADB (Android Debug Bridge)](https://developer.android.com/tools/releases/platform-tools) instalado no computador.

### Passo 1: Ativar a DepuraÃ§Ã£o USB na TV
1. Na sua TV TCL, vÃ¡ em **ConfiguraÃ§Ãµes** âž” **PreferÃªncias do dispositivo** âž” **Sobre**.
2. DesÃ§a atÃ© a opÃ§Ã£o **NÃºmero da VersÃ£o** (ou *Build*) e pressione a tecla **[OK] 7 vezes seguidas** atÃ© aparecer a mensagem *"VocÃª agora Ã© um desenvolvedor"*.
3. Volte um menu, entre em **OpÃ§Ãµes do desenvolvedor** e ative a chave **DepuraÃ§Ã£o USB** (ou *Network Debugging*).

### Passo 2: Descobrir o IP da sua TV
* Na TV, vÃ¡ em **ConfiguraÃ§Ãµes** âž” **Rede e Internet** âž” selecione a sua conexÃ£o atual para ver o endereÃ§o IP (exemplo: `192.168.1.100`).

### Passo 3: Executar a OtimizaÃ§Ã£o em 1 Clique (AutomÃ¡tico)

1. Baixe a versÃ£o mais recente em [Releases](../../releases) deste repositÃ³rio e descompacte os arquivos.
2. No Windows, entre na pasta `scripts` e dÃª dois cliques em:
   * **`optimize_tcl.bat`** (ou use o `optimize_tcl.ps1` no PowerShell).
3. O script solicitarÃ¡ o IP da sua TV. Digite-o e pressione Enter.
4. **Olhe para a tela da TV:** aparecerÃ¡ uma caixa de diÃ¡logo perguntando *"Permitir depuraÃ§Ã£o USB?"*. Marque a caixa **"Sempre permitir a partir deste computador"** e selecione **OK** com o controle.
5. O script irÃ¡:
   * Instalar o `NanoTV-Launcher-v1.0.apk`.
   * TornÃ¡-lo a tela inicial padrÃ£o da TV.
   * Desativar com seguranÃ§a a telemetria pesada e propagandas de fÃ¡brica da TCL.
   * Ajustar as animaÃ§Ãµes do sistema para `0.5x` para resposta instantÃ¢nea no controle remoto.

---

## ðŸ”„ Como Reverter para o PadrÃ£o de FÃ¡brica (Se desejar)

Se por qualquer motivo vocÃª quiser voltar para o launcher oficial do Google e reativar os serviÃ§os de fÃ¡brica da TCL:
1. Na pasta `scripts`, execute **`restore_tcl.bat`** (ou `restore_tcl.ps1`).
2. Digite o IP da TV.
3. Todas as alteraÃ§Ãµes serÃ£o desfeitas imediatamente, restaurando o sistema 100% ao estado original.

---

## ðŸ› ï¸ InstalaÃ§Ã£o Manual via Comandos ADB

Se preferir rodar os comandos manualmente pelo terminal:

```bash
# 1. Conectar na TV
adb connect SEU_IP_AQUI:5555

# 2. Instalar o NanoTV Launcher
adb install -r releases/NanoTV-Launcher-v1.0.apk

# 3. Definir como tela inicial padrÃ£o
adb shell cmd package set-home-activity com.nanotv.launcher/.MainActivity

# 4. Desativar o launcher pesado e anÃºncios do Google TV
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm disable-user --user 0 com.google.android.tvrecommendations

# 5. Desativar bloatwares e telemetria da TCL
adb shell pm disable-user --user 0 com.tcl.bootadservice
adb shell pm disable-user --user 0 com.tcl.bi
adb shell pm disable-user --user 0 com.tcl.waterfall.overseas
adb shell pm disable-user --user 0 com.tcl.tvweishi
adb shell pm disable-user --user 0 com.tcl.appmarket2
adb shell pm disable-user --user 0 com.tcl.esticker
adb shell pm disable-user --user 0 com.tcl.showmode
adb shell pm disable-user --user 0 com.tcl.usercenter
adb shell pm disable-user --user 0 com.tcl.common.terminalmanagerplus
adb shell pm disable-user --user 0 com.tcl.notereminder
adb shell pm disable-user --user 0 com.tcl.messagebox
adb shell pm disable-user --user 0 com.tcl.copydatatotv
adb shell pm disable-user --user 0 com.tcl.partnercustomizer

# 6. Acelerar animaÃ§Ãµes de janela
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5

# 7. Iniciar o NanoTV
adb shell am start -n com.nanotv.launcher/.MainActivity
```

---

## ðŸ’» CompilaÃ§Ã£o pelo CÃ³digo-Fonte

Este projeto pode ser aberto e compilado diretamente no **Android Studio**:
1. Clone este repositÃ³rio:
   ```bash
   git clone https://github.com/mateuspaulo/nanotv-launcher.git
   ```
2. Abra a pasta do projeto no Android Studio.
3. Compile o APK de depuraÃ§Ã£o via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
4. O APK gerado estarÃ¡ em: `app/build/outputs/apk/debug/app-debug.apk`.

---

## ðŸ“„ LicenÃ§a

DistribuÃ­do sob a licenÃ§a **MIT**. Veja `LICENSE` para mais informaÃ§Ãµes.
Sinta-se livre para contribuir, sugerir melhorias e criar forks para outros modelos de televisores!

