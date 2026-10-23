# NanoTV Launcher 📺⚡
> **Launcher ultraleve, moderno e sem anúncios para Smart TVs Android (com foco especial na linha TCL / 50P8M e dispositivos de hardware modesto).**

![NanoTV Launcher Preview](assets/preview.png)

---

## 💡 Por que este projeto existe?

Muitas Smart TVs Android (como a **TCL 50P8M**, modelos Philips, Philco e diversas TV Boxes) possuem hardware modesto: processadores quad-core de baixo consumo e **apenas 2 GB de memória RAM**. 

O launcher padrão oficial do Google TV e as customizações de fábrica dessas TVs pesam excessivamente no sistema:
* **Google TV Launcher:** consome até **90% de CPU** em picos e mais de **250 MB de RAM**, carregando trailers de vídeo automáticos, propagandas e sincronização contínua de canais patrocinados em segundo plano.
* **Telemetria de Fábrica:** serviços em segundo plano (como *TCL BI*, *Waterfall Ads*, *TV Guard*) competem pela CPU e causam engasgos severos, congelamentos no controle remoto e lentidão ao abrir aplicativos.

O **NanoTV Launcher** foi desenvolvido em **Java/Views nativas puras (XML + RecyclerView)**, sem bibliotecas pesadas de runtime, trazendo a TV de volta à fluidez que ela deveria ter.

---

## ✨ Recursos Principais

* ⚡ **0% de Uso de CPU em Repouso:** A interface é estática e instantânea, liberando os núcleos do processador para os seus filmes e vídeos.
* 🧠 **Uso Mínimo de RAM (< 20 MB):** Redução de mais de **90%** comparado ao launcher oficial do Google.
* 📊 **Métricas de Hardware em Tempo Real:** Exibe no topo o consumo de **RAM**, **Espaço em Disco Livre** e **Uso de CPU**. *(Pode ser ocultado a qualquer momento no menu de Configurações)*.
* 🔌 **Indicador de Conexão com a Internet:** Identifica automaticamente se a TV está conectada via **Cabo (Ethernet)** ou **Wi-Fi** (com nome da rede). Ao clicar nele com o controle, abre diretamente a tela de conexões da TV.
* 🎛️ **Seletor Real de Entradas Físicas:** Menu limpo com as portas reais de hardware (**HDMI 1**, **HDMI 2**, **HDMI 3**, **AV** e **TV Aberta/Antena**).
* 🔀 **Organização e Posição Salva Permanentemente:** Mova os aplicativos para onde quiser (esquerda, direita ou início). A ordem fica gravada no armazenamento interno e nunca se perde ao desligar a TV.
* ⭐ **Linha de Favoritos:** Fixe os seus aplicativos mais usados no topo.
* 🛠️ **Menu Completo de Gerenciamento:** Segure a tecla **[OK]** em qualquer app para favoritar, mover de posição, **desinstalar** ou **ocultar** da tela.
* 📜 **Barra de Rolagem Lateral Visível:** Indica claramente que há mais aplicativos abaixo.
* 📱 **Suporte a Apps Sideload:** Exibe todos os aplicativos instalados, inclusive aqueles que o Android TV padrão esconde por não terem banner de TV.
* 🚫 **Zero Propagandas e Zero Telemetria:** Nenhuma conexão externa desnecessária.

---

## 🏠 Como Definir o NanoTV como Launcher Padrão (Sem Ficar Perguntando)

Por padrão, o Android TV tenta forçar o retorno ao launcher original da Google toda vez que a tecla **HOME** é pressionada, exibindo uma caixa de seleção *"Concluir a ação usando..."* repetidamente. 

Para tornar o **NanoTV Launcher a tela inicial definitiva e permanente**, utilize um dos métodos abaixo:

### Método 1: Automático em 1 Clique (Recomendado)
Se você utilizar o nosso script de otimização, ele já realiza todo esse processo automaticamente sem você precisar digitar comandos:
1. Conecte o computador e a TV na mesma rede.
2. Na pasta `scripts/`, execute **`optimize_tcl.bat`** (no Windows) ou **`optimize_tcl.ps1`** (PowerShell).
3. Informe o IP da TV. Pronto! O NanoTV será configurado como padrão e o launcher pesado do Google será desativado.

---

### Método 2: Manual via Comandos ADB

Se preferir fazer diretamente pelo terminal:

```bash
# 1. Conectar na TV via ADB
adb connect SEU_IP_AQUI:5555

# 2. Definir o NanoTV como o Launcher padrão oficial do sistema
adb shell cmd package set-home-activity com.nanotv.launcher/.MainActivity

# 3. Desativar o launcher original da Google (Essencial para não pedir confirmação)
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm disable-user --user 0 com.google.android.tvrecommendations

# 4. Enviar sinal do botão Home para abrir o NanoTV imediatamente
adb shell input keyevent KEYCODE_HOME
```

> **Por que é necessário desativar o pacote da Google?**  
> No Android TV, o launcher de fábrica possui uma prioridade rígida no sistema. Ao executar `pm disable-user`, o Android TV reconhece o NanoTV Launcher como a **única tela inicial existente**, eliminando a caixa de seleção e impedindo que os processos pesados do Google continuem consumindo memória e CPU em segundo plano.  
> *(Esse procedimento é 100% reversível e seguro; nada é deletado permanentemente).*

---

### Método 3: Pelo Controle Remoto (Sem Computador)

1. Instale o APK do NanoTV na TV (via pen drive ou app tipo *Send Files to TV*).
2. Pressione a tecla **HOME** (ícone da casinha) no controle remoto.
3. Na janela que aparecer (*"Concluir a ação usando..."*):
   * Selecione **NanoTV Launcher**.
   * Escolha a opção **SEMPRE** (Always).

*(Nota: Alguns modelos de TV com Android 9/10/11 desabilitam o botão "Sempre" por imposição da fabricante; se isso acontecer na sua TV, utilize o Método 1 ou 2 via ADB para fixar definitivamente).*

---

## 🚀 Guia de Instalação Completa

### Pré-requisitos:
1. Sua TV e seu computador devem estar conectados na **mesma rede local** (via Wi-Fi ou Cabo de rede).
2. Ter o [ADB (Android Debug Bridge)](https://developer.android.com/tools/releases/platform-tools) no computador (ou use os comandos do SDK).

### Passo 1: Ativar a Depuração USB na TV
1. Na sua TV, vá em **Configurações** ➔ **Preferências do dispositivo** ➔ **Sobre**.
2. Desça até a opção **Número da Versão** (ou *Build*) e pressione a tecla **[OK] 7 vezes seguidas** até aparecer a mensagem *"Você agora é um desenvolvedor"*.
3. Volte um menu, entre em **Opções do desenvolvedor** e ative a chave **Depuração USB** (ou *Network Debugging*).

### Passo 2: Descobrir o IP da sua TV
* Na TV, vá em **Configurações** ➔ **Rede e Internet** ➔ selecione a sua conexão atual para ver o endereço IP (exemplo: `192.168.1.100`).

### Passo 3: Executar a Otimização
1. Baixe a versão mais recente em [Releases](../../releases) deste repositório e descompacte os arquivos.
2. No Windows, entre na pasta `scripts` e dê dois cliques em **`optimize_tcl.bat`**.
3. Digite o IP da sua TV e dê Enter.
4. **Olhe para a tela da TV:** aparecerá uma caixa de diálogo perguntando *"Permitir depuração USB?"*. Marque a caixa **"Sempre permitir a partir deste computador"** e selecione **OK** com o controle.
5. O script irá instalar o APK, defini-lo como padrão, desativar telemetrias e ajustar a velocidade das animações para `0.5x`.

---

## 🔄 Como Reverter para o Padrão de Fábrica (Se desejar)

Se você quiser desfazer as alterações e voltar para o launcher oficial do Google:

### Opção 1: Pelo Script Automático
1. Na pasta `scripts`, execute **`restore_tcl.bat`** (ou `restore_tcl.ps1`).
2. Digite o IP da TV e pressione Enter. Tudo será restaurado ao estado original de fábrica.

### Opção 2: Manualmente via ADB
```bash
# 1. Reabilitar o launcher e recomendações do Google
adb shell pm enable com.google.android.tvlauncher
adb shell pm enable com.google.android.tvrecommendations

# 2. Definir o Google TV novamente como padrão
adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity

# 3. Reativar telemetria e serviços de fábrica da TCL
adb shell pm enable com.tcl.bootadservice
adb shell pm enable com.tcl.bi
adb shell pm enable com.tcl.waterfall.overseas
adb shell pm enable com.tcl.tvweishi
adb shell pm enable com.tcl.appmarket2

# 4. Restaurar escalas de animação para 1.0x
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0

# 5. Iniciar a tela inicial original
adb shell am start -n com.google.android.tvlauncher/.MainActivity
```

---

## 🛠️ Comandos de Otimização Manual (Referência)

Caso queira entender ou executar os comandos individualmente:

```bash
# 1. Conectar na TV
adb connect SEU_IP_AQUI:5555

# 2. Instalar o NanoTV Launcher
adb install -r releases/NanoTV-Launcher-v1.0.apk

# 3. Definir como tela inicial padrão e desativar o launcher pesado
adb shell cmd package set-home-activity com.nanotv.launcher/.MainActivity
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm disable-user --user 0 com.google.android.tvrecommendations

# 4. Desativar bloatwares e telemetria pesada da TCL
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

# 5. Acelerar animações de janela (resposta instantânea no controle remoto)
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5

# 6. Abrir o NanoTV
adb shell am start -n com.nanotv.launcher/.MainActivity
```

---

## 💻 Compilação pelo Código-Fonte

Este projeto foi construído usando Android Studio com Gradle padrão:
1. Clone este repositório:
   ```bash
   git clone https://github.com/mateuspaulo/nanotv-launcher.git
   ```
2. Abra a pasta do projeto no **Android Studio**.
3. Compile o APK de depuração via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
4. O APK gerado estará em: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 Licença

Distribuído sob a licença **MIT**. Veja `LICENSE` para mais informações.
Sinta-se livre para contribuir, sugerir melhorias e criar forks para outros modelos de televisores!
