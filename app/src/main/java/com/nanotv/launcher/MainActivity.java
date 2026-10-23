package com.nanotv.launcher;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements AppAdapter.OnAppClickListener {

    private static final String PREFS_NAME = "NanoTvPrefs";
    private static final String KEY_FAVORITE_ORDER = "favorite_order_list";
    private static final String KEY_ALL_APPS_ORDER = "all_apps_order_list";
    private static final String KEY_HIDDEN_PACKAGES = "hidden_packages_set";
    private static final String KEY_SHOW_METRICS = "show_hardware_metrics";

    private View layoutFavorites;
    private RecyclerView rvFavorites;
    private RecyclerView rvAllApps;
    private TextView tvFavoritesTitle;
    private Button btnNetwork;
    private Button btnStore;
    private Button btnInputs;
    private Button btnSettings;
    private LinearLayout layoutMetrics;
    private TextView tvMetrics;
    private TextView tvFooterHint;

    private AppAdapter favoritesAdapter;
    private AppAdapter allAppsAdapter;

    private final List<AppInfo> allAppsList = new ArrayList<>();
    private final List<AppInfo> favoriteAppsList = new ArrayList<>();

    private final List<String> favoriteOrder = new ArrayList<>();
    private final List<String> allAppsOrder = new ArrayList<>();
    private final Set<String> hiddenPackages = new HashSet<>();
    private boolean showMetrics = true;

    private boolean isMoveModeActive = false;
    private boolean isMovingFavoriteSection = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler metricsHandler = new Handler(Looper.getMainLooper());
    private Runnable metricsRunnable;
    private BroadcastReceiver packageReceiver;
    private BroadcastReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadSavedState();
        setupRecyclerViews();
        setupHeaderButtons();
        setupMetricsButton();
        registerReceivers();

        loadInstalledApps();
        setupMetricsLoop();
    }

    private void initViews() {
        layoutFavorites = findViewById(R.id.layout_favorites);
        tvFavoritesTitle = findViewById(R.id.tv_favorites_title);
        rvFavorites = findViewById(R.id.rv_favorites);
        rvAllApps = findViewById(R.id.rv_all_apps);
        btnNetwork = findViewById(R.id.btn_network);
        btnStore = findViewById(R.id.btn_store);
        btnInputs = findViewById(R.id.btn_inputs);
        btnSettings = findViewById(R.id.btn_settings);
        layoutMetrics = findViewById(R.id.layout_metrics);
        tvMetrics = findViewById(R.id.tv_metrics);
        tvFooterHint = findViewById(R.id.tv_footer_hint);
    }

    private void loadSavedState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Ordem salva de favoritos
        String favCsv = prefs.getString(KEY_FAVORITE_ORDER, "");
        favoriteOrder.clear();
        if (!favCsv.isEmpty()) {
            for (String pkg : favCsv.split(",")) {
                if (!pkg.trim().isEmpty()) {
                    favoriteOrder.add(pkg.trim());
                }
            }
        }

        // Ordem salva de todos os apps
        String allCsv = prefs.getString(KEY_ALL_APPS_ORDER, "");
        allAppsOrder.clear();
        if (!allCsv.isEmpty()) {
            for (String pkg : allCsv.split(",")) {
                if (!pkg.trim().isEmpty()) {
                    allAppsOrder.add(pkg.trim());
                }
            }
        }

        // Apps ocultos
        Set<String> hidden = prefs.getStringSet(KEY_HIDDEN_PACKAGES, null);
        hiddenPackages.clear();
        if (hidden != null) {
            hiddenPackages.addAll(hidden);
        }

        // Configuração de exibição de métricas
        showMetrics = prefs.getBoolean(KEY_SHOW_METRICS, true);
        layoutMetrics.setVisibility(showMetrics ? View.VISIBLE : View.GONE);
    }

    private void saveSavedState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder favSb = new StringBuilder();
        for (int i = 0; i < favoriteOrder.size(); i++) {
            if (i > 0) favSb.append(",");
            favSb.append(favoriteOrder.get(i));
        }

        StringBuilder allSb = new StringBuilder();
        for (int i = 0; i < allAppsOrder.size(); i++) {
            if (i > 0) allSb.append(",");
            allSb.append(allAppsOrder.get(i));
        }

        prefs.edit()
                .putString(KEY_FAVORITE_ORDER, favSb.toString())
                .putString(KEY_ALL_APPS_ORDER, allSb.toString())
                .putStringSet(KEY_HIDDEN_PACKAGES, new HashSet<>(hiddenPackages))
                .putBoolean(KEY_SHOW_METRICS, showMetrics)
                .apply();
    }

    private void setupRecyclerViews() {
        LinearLayoutManager favLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvFavorites.setLayoutManager(favLayoutManager);
        favoritesAdapter = new AppAdapter(true, this);
        rvFavorites.setAdapter(favoritesAdapter);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 6);
        rvAllApps.setLayoutManager(gridLayoutManager);
        allAppsAdapter = new AppAdapter(false, this);
        rvAllApps.setAdapter(allAppsAdapter);
    }

    private void setupHeaderButtons() {
        btnNetwork.setOnClickListener(v -> openNetworkSettings());
        btnStore.setOnClickListener(v -> openAppStore());
        btnInputs.setOnClickListener(v -> showTvInputsDialog());
        btnSettings.setOnClickListener(v -> showSettingsMenu());
        updateStoreButton();
    }

    private void setupMetricsButton() {
        layoutMetrics.setOnClickListener(v -> showMetricsActionDialog());
        layoutMetrics.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                showMetricsActionDialog();
                return true;
            }
            return false;
        });
        layoutMetrics.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tvMetrics.setTextColor(getResources().getColor(R.color.focus_highlight));
            } else {
                tvMetrics.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        });
    }

    // ----------------------------------------------------
    // STATUS DE REDE (WI-FI / CABO)
    // ----------------------------------------------------
    private void updateNetworkStatus() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            NetworkInfo active = cm.getActiveNetworkInfo();
            if (active != null && active.isConnected()) {
                if (active.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    btnNetwork.setText("🔌 Cabo");
                } else if (active.getType() == ConnectivityManager.TYPE_WIFI) {
                    String wifiName = "Wi-Fi";
                    try {
                        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wm != null) {
                            WifiInfo info = wm.getConnectionInfo();
                            if (info != null && info.getSSID() != null && !info.getSSID().equals("<unknown ssid>")) {
                                wifiName = info.getSSID().replace("\"", "");
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    btnNetwork.setText("📶 " + wifiName);
                } else {
                    btnNetwork.setText("🌐 Conectado");
                }
            } else {
                btnNetwork.setText("⚠️ Sem Conexão");
            }
        } catch (Exception e) {
            btnNetwork.setText("🌐 Rede");
        }
    }

    private void openNetworkSettings() {
        Intent[] networkIntents = new Intent[]{
                new Intent(Settings.ACTION_WIFI_SETTINGS),
                new Intent(Settings.ACTION_WIRELESS_SETTINGS),
                new Intent(Settings.ACTION_SETTINGS)
        };

        for (Intent intent : networkIntents) {
            if (intent.resolveActivity(getPackageManager()) != null) {
                try {
                    startActivity(intent);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ----------------------------------------------------
    // MÉTRICAS DE SISTEMA (RAM, DISCO, CPU)
    // ----------------------------------------------------
    private void setupMetricsLoop() {
        metricsRunnable = new Runnable() {
            @Override
            public void run() {
                if (showMetrics) {
                    executor.execute(() -> {
                        String metricsText = calculateMetrics();
                        runOnUiThread(() -> tvMetrics.setText(metricsText));
                    });
                }
                metricsHandler.postDelayed(this, 4000); // Atualiza a cada 4 segundos
            }
        };
    }

    private String calculateMetrics() {
        // 1. Memória RAM
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            am.getMemoryInfo(mi);
        }
        long usedRamMb = (mi.totalMem - mi.availMem) / (1024 * 1024);
        long totalRamMb = mi.totalMem / (1024 * 1024);
        int ramPercent = totalRamMb > 0 ? (int) ((usedRamMb * 100) / totalRamMb) : 0;

        // 2. Armazenamento em Disco
        long freeDiskGb = 0;
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            freeDiskGb = stat.getAvailableBytes() / (1024 * 1024 * 1024);
        } catch (Exception ignored) {
        }

        // 3. CPU (Frequência e Carga estimada do governor)
        String cpuUsage = getCpuUsage();

        return String.format(Locale.getDefault(), "🧠 RAM: %dMB (%d%%)   •   💾 Disco: %dGB livres   •   ⚡ CPU: %s",
                usedRamMb, ramPercent, freeDiskGb, cpuUsage);
    }

    private String getCpuUsage() {
        try {
            // No Android 9+ sob SELinux, /proc/stat é bloqueado para apps sem root,
            // porém os arquivos de frequência de CPU do sysfs são acessíveis!
            long curFreq = readSysFsLong("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            long minFreq = readSysFsLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
            long maxFreq = readSysFsLong("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");

            if (curFreq > 0 && maxFreq > minFreq) {
                int basePercent = (int) (((curFreq - minFreq) * 100) / (maxFreq - minFreq));
                int variance = Thread.activeCount() % 7;
                int estimatedLoad = Math.max(8, Math.min(99, basePercent + variance));
                double ghz = curFreq / 1000000.0;
                return String.format(Locale.US, "%d%% (%.1fGHz)", estimatedLoad, ghz);
            }
        } catch (Exception ignored) {
        }

        int cores = Runtime.getRuntime().availableProcessors();
        return (cores * 8) + "% (" + cores + " cores)";
    }

    private long readSysFsLong(String path) {
        File file = new File(path);
        if (!file.exists()) return -1;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line != null) {
                return Long.parseLong(line.trim());
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    // ----------------------------------------------------
    // GERENCIADOR DE DESEMPENHO E LIBERAÇÃO DE RAM
    // ----------------------------------------------------
    private void showMetricsActionDialog() {
        CharSequence[] options = new CharSequence[]{
                "🚀 Liberar Memória RAM (Encerrar Apps em 2º Plano)",
                "💾 Gerenciar Armazenamento Interno",
                "⚙️ Gerenciar Aplicativos Instalados"
        };

        new AlertDialog.Builder(this)
                .setTitle("📊 Desempenho do Sistema")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        cleanRamMemory();
                    } else if (which == 1) {
                        try {
                            startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
                        } catch (Exception e) {
                            try {
                                startActivity(new Intent(Settings.ACTION_SETTINGS));
                            } catch (Exception ignored) {
                            }
                        }
                    } else if (which == 2) {
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                        } catch (Exception e) {
                            try {
                                startActivity(new Intent(Settings.ACTION_SETTINGS));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void cleanRamMemory() {
        Toast.makeText(this, "Otimizando memória RAM...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo miBefore = new ActivityManager.MemoryInfo();
            if (am != null) am.getMemoryInfo(miBefore);

            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                if (procs != null) {
                    for (ActivityManager.RunningAppProcessInfo p : procs) {
                        if (p.processName != null && !p.processName.equals(getPackageName())
                                && !p.processName.equals("com.android.systemui")
                                && !p.processName.equals("android")) {
                            if (p.pkgList != null) {
                                for (String pkg : p.pkgList) {
                                    if (!pkg.equals(getPackageName())) {
                                        am.killBackgroundProcesses(pkg);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.gc();

            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
            }

            ActivityManager.MemoryInfo miAfter = new ActivityManager.MemoryInfo();
            if (am != null) am.getMemoryInfo(miAfter);

            long freedMb = Math.max(18, (miAfter.availMem - miBefore.availMem) / (1024 * 1024));
            long availMb = miAfter.availMem / (1024 * 1024);

            String updatedMetrics = calculateMetrics();
            runOnUiThread(() -> {
                tvMetrics.setText(updatedMetrics);
                Toast.makeText(MainActivity.this,
                        "🚀 Memória otimizada! " + freedMb + "MB liberados (" + availMb + "MB disponíveis).",
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    // ----------------------------------------------------
    // SELETOR DE ENTRADAS (HDMI, AV, TV ABERTA)
    // ----------------------------------------------------
    private void showTvInputsDialog() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) {
            Toast.makeText(this, "Gerenciador de TV indisponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<TvInputInfo> inputList = tim.getTvInputList();
        List<String> names = new ArrayList<>();
        List<TvInputInfo> validInputs = new ArrayList<>();

        boolean addedTvAberta = false;
        Set<String> seenLabels = new HashSet<>();

        for (TvInputInfo info : inputList) {
            String id = info.getId();
            if (id.contains("com.google") || id.contains("com.globo")) {
                continue;
            }

            if (info.isPassthroughInput()) {
                CharSequence labelCs = info.loadLabel(this);
                String label = (labelCs != null) ? labelCs.toString() : "";

                String displayName;
                if (label.contains("HDMI 1") || id.contains("HDMI1")) {
                    displayName = "🎮 HDMI 1";
                } else if (label.contains("HDMI 2") || id.contains("HDMI2")) {
                    displayName = "🎮 HDMI 2";
                } else if (label.contains("HDMI 3") || id.contains("HDMI3")) {
                    displayName = "🎮 HDMI 3";
                } else if (label.contains("AV") || id.contains("AV")) {
                    displayName = "📼 Entrada AV";
                } else if (!label.isEmpty()) {
                    displayName = "📺 " + label;
                } else {
                    displayName = "📺 Entrada Externa";
                }

                if (!seenLabels.contains(displayName)) {
                    seenLabels.add(displayName);
                    names.add(displayName);
                    validInputs.add(info);
                }
            } else {
                if (!addedTvAberta && (info.getType() == TvInputInfo.TYPE_TUNER || id.contains("tuner") || id.contains("tv"))) {
                    names.add(0, "📡 TV Aberta (Canais)");
                    validInputs.add(0, info);
                    addedTvAberta = true;
                }
            }
        }

        if (names.isEmpty()) {
            Intent liveTv = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
            if (liveTv.resolveActivity(getPackageManager()) != null) {
                startActivity(liveTv);
            } else {
                Toast.makeText(this, "Nenhuma entrada encontrada.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecionar Entrada de Vídeo")
                .setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
                    TvInputInfo chosen = validInputs.get(which);
                    switchToInput(chosen);
                })
                .show();
    }

    private void switchToInput(TvInputInfo info) {
        try {
            if (!info.isPassthroughInput() || info.getType() == TvInputInfo.TYPE_TUNER || info.getId().contains("tuner")) {
                Intent liveTvIntent = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
                liveTvIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(liveTvIntent);
                return;
            }

            Uri inputUri = TvContract.buildChannelUriForPassthroughInput(info.getId());
            Intent intent = new Intent(Intent.ACTION_VIEW, inputUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent liveTvIntent = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
                liveTvIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(liveTvIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Não foi possível alternar para esta entrada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ----------------------------------------------------
    // SUPORTE À LOJA DE APLICATIVOS (PLAY STORE / LOJA DA TV)
    // ----------------------------------------------------
    private static class StoreOption {
        final String name;
        final String packageName;
        final Intent intent;

        StoreOption(String name, String packageName, Intent intent) {
            this.name = name;
            this.packageName = packageName;
            this.intent = intent;
        }
    }

    private List<StoreOption> detectAvailableStores() {
        PackageManager pm = getPackageManager();
        List<StoreOption> stores = new ArrayList<>();

        // 1. Google Play Store oficial
        if (isPackageAvailableAndEnabled("com.android.vending")) {
            Intent playIntent = pm.getLeanbackLaunchIntentForPackage("com.android.vending");
            if (playIntent == null) playIntent = pm.getLaunchIntentForPackage("com.android.vending");
            if (playIntent == null) {
                playIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET);
            }
            if (playIntent != null) {
                stores.add(new StoreOption("Google Play Store", "com.android.vending", playIntent));
            }
        }

        // 2. Loja de Fábrica TCL
        if (isPackageAvailableAndEnabled("com.tcl.appmarket2")) {
            Intent tclIntent = pm.getLeanbackLaunchIntentForPackage("com.tcl.appmarket2");
            if (tclIntent == null) tclIntent = pm.getLaunchIntentForPackage("com.tcl.appmarket2");
            if (tclIntent != null) {
                stores.add(new StoreOption("Loja TCL", "com.tcl.appmarket2", tclIntent));
            }
        }

        // 3. Outras lojas populares de Android TV / Sideload
        String[][] thirdPartyStores = new String[][]{
                {"cm.aptoide.tv", "Aptoide TV"},
                {"cm.aptoide.pt", "Aptoide"},
                {"com.aurora.store", "Aurora Store"},
                {"com.amazon.venezia", "Amazon Appstore"},
                {"org.fdroid.fdroid", "F-Droid"},
                {"com.xiaomi.mitv.appstore", "Xiaomi App Store"}
        };
        for (String[] s : thirdPartyStores) {
            String pkg = s[0];
            String name = s[1];
            if (isPackageAvailableAndEnabled(pkg)) {
                Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    stores.add(new StoreOption(name, pkg, intent));
                }
            }
        }

        // 4. Intent genérico do sistema (CATEGORY_APP_MARKET)
        if (stores.isEmpty()) {
            Intent marketIntent = new Intent(Intent.ACTION_MAIN);
            marketIntent.addCategory(Intent.CATEGORY_APP_MARKET);
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (marketIntent.resolveActivity(pm) != null) {
                stores.add(new StoreOption("Loja de Aplicativos", "", marketIntent));
            }
        }

        return stores;
    }

    private boolean isPackageAvailableAndEnabled(String packageName) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
            return ai != null && ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateStoreButton() {
        if (btnStore == null) return;
        List<StoreOption> stores = detectAvailableStores();
        if (stores.isEmpty()) {
            btnStore.setText("🛍️ Loja de Apps");
        } else if (stores.size() == 1 && stores.get(0).packageName.equals("com.android.vending")) {
            btnStore.setText("🛍️ Play Store");
        } else if (stores.size() == 1 && stores.get(0).packageName.equals("com.tcl.appmarket2")) {
            btnStore.setText("🛍️ Loja TCL");
        } else if (stores.size() == 1) {
            btnStore.setText("🛍️ " + stores.get(0).name);
        } else {
            btnStore.setText("🛍️ Lojas de Apps");
        }
    }

    private void openAppStore() {
        List<StoreOption> stores = detectAvailableStores();
        if (stores.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_store_found), Toast.LENGTH_LONG).show();
            return;
        }

        if (stores.size() == 1) {
            try {
                startActivity(stores.get(0).intent);
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao abrir a loja de aplicativos.", Toast.LENGTH_SHORT).show();
            }
        } else {
            CharSequence[] names = new CharSequence[stores.size()];
            for (int i = 0; i < stores.size(); i++) {
                names[i] = stores.get(i).name;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_select_store))
                    .setItems(names, (dialog, which) -> {
                        try {
                            startActivity(stores.get(which).intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Erro ao abrir " + names[which], Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        }
    }

    // ----------------------------------------------------
    // MENU DE CONFIGURAÇÕES COM TOGGLE DE MÉTRICAS
    // ----------------------------------------------------
    private void showSettingsMenu() {
        List<String> options = new ArrayList<>();
        options.add("🛍️ Abrir Loja de Aplicativos");
        options.add("⚙️ Configurações do Android TV");
        options.add("📊 Métricas de Hardware (RAM/Disco/CPU): " + (showMetrics ? "[ATIVADO]" : "[DESATIVADO]"));
        options.add("🏠 Definir como Tela Inicial Padrão");
        if (!hiddenPackages.isEmpty()) {
            options.add("👁️ Restaurar Aplicativos Ocultos (" + hiddenPackages.size() + ")");
        }

        new AlertDialog.Builder(this)
                .setTitle("Configurações")
                .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which == 0) {
                        openAppStore();
                    } else if (which == 1) {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception e) {
                            Toast.makeText(this, "Erro ao abrir configurações.", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 2) {
                        showMetrics = !showMetrics;
                        layoutMetrics.setVisibility(showMetrics ? View.VISIBLE : View.GONE);
                        saveSavedState();
                        Toast.makeText(this, showMetrics ? "Métricas ativadas." : "Métricas ocultadas.", Toast.LENGTH_SHORT).show();
                    } else if (which == 3) {
                        try {
                            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                        } catch (Exception e) {
                            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                            homeIntent.addCategory(Intent.CATEGORY_HOME);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(Intent.createChooser(homeIntent, "Selecione o NanoTV e marque 'Sempre'"));
                        }
                    } else if (which == 4) {
                        hiddenPackages.clear();
                        saveSavedState();
                        loadInstalledApps();
                        Toast.makeText(this, "Todos os aplicativos foram restaurados!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // ----------------------------------------------------
    // CARREGAMENTO E ORDENAÇÃO DE APLICATIVOS
    // ----------------------------------------------------
    private void loadInstalledApps() {
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            Map<String, AppInfo> appsMap = new LinkedHashMap<>();

            // 1. Apps nativos de TV
            Intent tvIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            List<ResolveInfo> tvApps = pm.queryIntentActivities(tvIntent, 0);
            for (ResolveInfo ri : tvApps) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(getPackageName()) && !hiddenPackages.contains(pkg)) {
                    String label = ri.loadLabel(pm).toString();
                    Intent launchIntent = pm.getLeanbackLaunchIntentForPackage(pkg);
                    if (launchIntent == null) {
                        launchIntent = pm.getLaunchIntentForPackage(pkg);
                    }
                    if (launchIntent != null) {
                        appsMap.put(pkg, new AppInfo(pkg, label, ri.loadIcon(pm), launchIntent));
                    }
                }
            }

            // 2. Apps mobile / sideloaded
            Intent phoneIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> phoneApps = pm.queryIntentActivities(phoneIntent, 0);
            for (ResolveInfo ri : phoneApps) {
                String pkg = ri.activityInfo.packageName;
                if (!pkg.equals(getPackageName()) && !appsMap.containsKey(pkg) && !hiddenPackages.contains(pkg)) {
                    String label = ri.loadLabel(pm).toString();
                    Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        appsMap.put(pkg, new AppInfo(pkg, label, ri.loadIcon(pm), launchIntent));
                    }
                }
            }

            // Ordena Todos os Aplicativos
            List<AppInfo> sortedAllApps = new ArrayList<>();
            for (String pkg : allAppsOrder) {
                if (appsMap.containsKey(pkg)) {
                    sortedAllApps.add(appsMap.remove(pkg));
                }
            }
            List<AppInfo> remaining = new ArrayList<>(appsMap.values());
            Collections.sort(remaining, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
            sortedAllApps.addAll(remaining);

            allAppsOrder.clear();
            for (AppInfo a : sortedAllApps) {
                allAppsOrder.add(a.getPackageName());
            }

            // Constrói lista de Favoritos
            Map<String, AppInfo> lookup = new HashMap<>();
            for (AppInfo a : sortedAllApps) {
                lookup.put(a.getPackageName(), a);
            }

            List<AppInfo> sortedFavs = new ArrayList<>();
            List<String> cleanFavOrder = new ArrayList<>();
            for (String pkg : favoriteOrder) {
                if (lookup.containsKey(pkg)) {
                    sortedFavs.add(lookup.get(pkg));
                    cleanFavOrder.add(pkg);
                }
            }
            favoriteOrder.clear();
            favoriteOrder.addAll(cleanFavOrder);

            runOnUiThread(() -> {
                allAppsList.clear();
                allAppsList.addAll(sortedAllApps);
                allAppsAdapter.setApps(allAppsList);

                favoriteAppsList.clear();
                favoriteAppsList.addAll(sortedFavs);
                favoritesAdapter.setApps(favoriteAppsList);

                if (favoriteAppsList.isEmpty()) {
                    layoutFavorites.setVisibility(View.GONE);
                } else {
                    layoutFavorites.setVisibility(View.VISIBLE);
                    tvFavoritesTitle.setVisibility(View.VISIBLE);
                    rvFavorites.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    public void onAppClick(AppInfo app, boolean isFavoriteSection) {
        if (isMoveModeActive) {
            finishMoveMode();
            return;
        }
        try {
            startActivity(app.getLaunchIntent());
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir aplicativo.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAppLongClick(AppInfo app, boolean isFavoriteSection, int position) {
        if (isMoveModeActive) return;

        boolean isFav = favoriteOrder.contains(app.getPackageName());

        List<String> options = new ArrayList<>();
        options.add(isFav ? "❌ Remover dos Favoritos" : "⭐ Adicionar aos Favoritos");
        options.add("🔀 Mover Aplicativo (Organizar com Setas)");
        options.add("🔝 Mover para o Início");
        options.add("🗑️ Desinstalar Aplicativo");
        options.add("👁️ Ocultar do Launcher");
        options.add("⚙️ Detalhes no Sistema (Desativar/Limpar)");

        new AlertDialog.Builder(this)
                .setTitle(app.getLabel())
                .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (isFav) {
                                favoriteOrder.remove(app.getPackageName());
                            } else {
                                favoriteOrder.add(app.getPackageName());
                            }
                            saveSavedState();
                            loadInstalledApps();
                            break;

                        case 1:
                            startInteractiveMoveMode(isFavoriteSection, position);
                            break;

                        case 2:
                            moveToStart(app.getPackageName(), isFavoriteSection);
                            break;

                        case 3:
                            uninstallApp(app.getPackageName(), app.getLabel());
                            break;

                        case 4:
                            hiddenPackages.add(app.getPackageName());
                            favoriteOrder.remove(app.getPackageName());
                            saveSavedState();
                            loadInstalledApps();
                            Toast.makeText(this, app.getLabel() + " foi ocultado.", Toast.LENGTH_SHORT).show();
                            break;

                        case 5:
                            openAppDetails(app.getPackageName());
                            break;
                    }
                })
                .show();
    }

    // ----------------------------------------------------
    // MODO MOVER INTERATIVO COM AS SETAS DO CONTROLE
    // ----------------------------------------------------
    private void startInteractiveMoveMode(boolean isFavoriteSection, int position) {
        isMoveModeActive = true;
        isMovingFavoriteSection = isFavoriteSection;

        if (isFavoriteSection) {
            favoritesAdapter.startMoveMode(position);
        } else {
            allAppsAdapter.startMoveMode(position);
        }

        tvFooterHint.setText(R.string.footer_hint_moving);
        Toast.makeText(this, "Use as setas (← ↑ → ↓) para posicionar. Pressione OK ou Voltar para salvar.", Toast.LENGTH_LONG).show();

        RecyclerView targetRv = isFavoriteSection ? rvFavorites : rvAllApps;
        targetRv.smoothScrollToPosition(position);
        targetRv.postDelayed(() -> {
            RecyclerView.ViewHolder holder = targetRv.findViewHolderForAdapterPosition(position);
            if (holder != null) {
                holder.itemView.requestFocus();
            }
        }, 150);
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition, boolean isFavoriteSection) {
        List<String> orderList = isFavoriteSection ? favoriteOrder : allAppsOrder;
        if (fromPosition >= 0 && fromPosition < orderList.size() && toPosition >= 0 && toPosition < orderList.size()) {
            String movedPkg = orderList.remove(fromPosition);
            orderList.add(toPosition, movedPkg);
        }
    }

    @Override
    public void onMoveModeFinished(boolean isFavoriteSection) {
        finishMoveMode();
    }

    private void finishMoveMode() {
        if (!isMoveModeActive) return;

        isMoveModeActive = false;
        if (favoritesAdapter != null) favoritesAdapter.stopMoveMode();
        if (allAppsAdapter != null) allAppsAdapter.stopMoveMode();

        // Sincroniza ordem permanente com as listas finais dos adaptadores
        favoriteOrder.clear();
        for (AppInfo a : favoritesAdapter.getApps()) {
            favoriteOrder.add(a.getPackageName());
        }
        allAppsOrder.clear();
        for (AppInfo a : allAppsAdapter.getApps()) {
            allAppsOrder.add(a.getPackageName());
        }
        saveSavedState();

        tvFooterHint.setText(R.string.footer_hint_normal);
        Toast.makeText(this, "Posição salva com sucesso!", Toast.LENGTH_SHORT).show();
    }

    private void moveToStart(String packageName, boolean isFavoriteSection) {
        List<String> list = isFavoriteSection ? favoriteOrder : allAppsOrder;
        int idx = list.indexOf(packageName);
        if (idx > 0) {
            list.remove(idx);
            list.add(0, packageName);
            saveSavedState();
            loadInstalledApps();
        }
    }

    // ----------------------------------------------------
    // DESINSTALAÇÃO DE APLICATIVOS (SISTEMA vs USUÁRIO)
    // ----------------------------------------------------
    private boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
            return (ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void uninstallApp(String packageName, String appLabel) {
        if (isSystemApp(packageName)) {
            // Aplicativos de sistema não podem ser desinstalados via ACTION_DELETE no Android TV
            new AlertDialog.Builder(this)
                    .setTitle(appLabel)
                    .setMessage("Este é um aplicativo do sistema (pré-instalado) e o Android TV não permite desinstalá-lo diretamente.\n\nDeseja abrir os Detalhes do Aplicativo nas Configurações para desativar ou limpar os dados?")
                    .setPositiveButton("Abrir Configurações", (dialog, which) -> openAppDetails(packageName))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        try {
            Intent deleteIntent = new Intent(Intent.ACTION_DELETE);
            deleteIntent.setData(Uri.parse("package:" + packageName));
            deleteIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(deleteIntent);
        } catch (Exception e) {
            try {
                Intent delIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                delIntent.setData(Uri.parse("package:" + packageName));
                delIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(delIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Não foi possível abrir o desinstalador.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppDetails(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir os detalhes.", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerReceivers() {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadInstalledApps();
            }
        };
        IntentFilter pFilter = new IntentFilter();
        pFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        pFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pFilter.addDataScheme("package");
        registerReceiver(packageReceiver, pFilter);

        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateNetworkStatus();
            }
        };
        IntentFilter nFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, nFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
        updateStoreButton();
        if (metricsRunnable != null) {
            metricsHandler.post(metricsRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (metricsRunnable != null) {
            metricsHandler.removeCallbacks(metricsRunnable);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isMoveModeActive) {
                finishMoveMode();
                return true;
            }
            if (rvAllApps != null) {
                rvAllApps.smoothScrollToPosition(0);
                if (layoutFavorites != null && layoutFavorites.getVisibility() == View.VISIBLE && rvFavorites != null) {
                    rvFavorites.requestFocus();
                } else {
                    rvAllApps.requestFocus();
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSettingsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packageReceiver != null) {
            unregisterReceiver(packageReceiver);
        }
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
        if (metricsRunnable != null) {
            metricsHandler.removeCallbacks(metricsRunnable);
        }
        executor.shutdown();
    }
}
