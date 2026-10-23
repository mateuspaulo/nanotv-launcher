package com.nanotv.launcher;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    private AppAdapter favoritesAdapter;
    private AppAdapter allAppsAdapter;

    private final List<AppInfo> allAppsList = new ArrayList<>();
    private final List<AppInfo> favoriteAppsList = new ArrayList<>();

    private final List<String> favoriteOrder = new ArrayList<>();
    private final List<String> allAppsOrder = new ArrayList<>();
    private final Set<String> hiddenPackages = new HashSet<>();
    private boolean showMetrics = true;

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
        long totalDiskGb = 0;
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            freeDiskGb = stat.getAvailableBytes() / (1024 * 1024 * 1024);
            totalDiskGb = stat.getTotalBytes() / (1024 * 1024 * 1024);
        } catch (Exception ignored) {
        }

        // 3. CPU
        String cpuUsage = getCpuUsage();

        return String.format("🧠 RAM: %dMB (%d%%)  •  💾 Disco: %dGB livres  •  ⚡ CPU: %s",
                usedRamMb, ramPercent, freeDiskGb, cpuUsage);
    }

    private String getCpuUsage() {
        try {
            Process process = Runtime.getRuntime().exec("top -n 1 -b -m 1");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("%cpu") || line.contains("%CPU") || line.contains("cpu ")) {
                    // Exemplo de linha: "400%cpu   12%user   0%nice   4%sys 384%idle"
                    String[] parts = line.split("\\s+");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].endsWith("%idle")) {
                            String idleStr = parts[i].replace("%idle", "").trim();
                            int idle = Integer.parseInt(idleStr);
                            int cores = Runtime.getRuntime().availableProcessors();
                            int maxCpu = cores * 100;
                            int used = Math.max(0, Math.min(100, (maxCpu - idle) / cores));
                            return used + "%";
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return Runtime.getRuntime().availableProcessors() + " cores";
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
                if (id.contains("4128") || label.contains("1") || id.contains("HDMI1")) {
                    displayName = "🔌 HDMI 1";
                } else if (id.contains("4384") || label.contains("2") || id.contains("HDMI2")) {
                    displayName = "🔌 HDMI 2";
                } else if (id.contains("4640") || label.contains("3") || id.contains("HDMI3")) {
                    displayName = "🔌 HDMI 3";
                } else if (id.contains("3104") || label.toUpperCase().contains("AV")) {
                    displayName = "🎛️ AV";
                } else if (label.toUpperCase().contains("HDMI")) {
                    displayName = "🔌 " + label;
                } else if (!label.isEmpty()) {
                    displayName = "📺 " + label;
                } else {
                    continue;
                }

                if (!seenLabels.contains(displayName)) {
                    seenLabels.add(displayName);
                    names.add(displayName);
                    validInputs.add(info);
                }
            } else {
                if (!addedTvAberta && (id.contains("TDTVInputService") || id.contains("HW1413742848") || info.getType() == TvInputInfo.TYPE_TUNER)) {
                    addedTvAberta = true;
                    names.add("📡 TV Aberta (Antena)");
                    validInputs.add(info);
                }
            }
        }

        if (!addedTvAberta) {
            names.add("📡 TV Aberta (Antena)");
            validInputs.add(null);
        }

        List<Integer> orderIndices = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) orderIndices.add(i);
        Collections.sort(orderIndices, (i1, i2) -> {
            String n1 = names.get(i1);
            String n2 = names.get(i2);
            return Integer.compare(getInputSortRank(n1), getInputSortRank(n2));
        });

        List<String> sortedNames = new ArrayList<>();
        List<TvInputInfo> sortedInputs = new ArrayList<>();
        for (int idx : orderIndices) {
            sortedNames.add(names.get(idx));
            sortedInputs.add(validInputs.get(idx));
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecionar Entrada")
                .setItems(sortedNames.toArray(new CharSequence[0]), (dialog, which) -> {
                    String chosenName = sortedNames.get(which);
                    TvInputInfo chosen = sortedInputs.get(which);
                    try {
                        if (chosenName.contains("TV Aberta") || chosen == null) {
                            Intent tvIntent = getPackageManager().getLaunchIntentForPackage("com.tcl.tv");
                            if (tvIntent != null) {
                                startActivity(tvIntent);
                            } else {
                                startActivity(new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI));
                            }
                        } else {
                            Uri uri = TvContract.buildChannelUriForPassthroughInput(chosen.getId());
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao trocar para a entrada selecionada.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private int getInputSortRank(String name) {
        if (name.contains("HDMI 1")) return 1;
        if (name.contains("HDMI 2")) return 2;
        if (name.contains("HDMI 3")) return 3;
        if (name.contains("HDMI")) return 4;
        if (name.contains("AV")) return 5;
        if (name.contains("TV Aberta")) return 6;
        return 7;
    }

    // ----------------------------------------------------
    // GERENCIAMENTO DA LOJA DE APLICATIVOS (PLAY STORE / LOJA TV)
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
                        // Alterna métricas de hardware
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
        try {
            startActivity(app.getLaunchIntent());
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir aplicativo.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAppLongClick(AppInfo app, boolean isFavoriteSection, int position) {
        boolean isFav = favoriteOrder.contains(app.getPackageName());

        List<String> options = new ArrayList<>();
        options.add(isFav ? "❌ Remover dos Favoritos" : "⭐ Adicionar aos Favoritos");
        options.add("⬅️ Mover para a Esquerda");
        options.add("➡️ Mover para a Direita");
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
                            moveApp(app.getPackageName(), isFavoriteSection, -1);
                            break;

                        case 2:
                            moveApp(app.getPackageName(), isFavoriteSection, 1);
                            break;

                        case 3:
                            moveToStart(app.getPackageName(), isFavoriteSection);
                            break;

                        case 4:
                            uninstallApp(app.getPackageName());
                            break;

                        case 5:
                            hiddenPackages.add(app.getPackageName());
                            favoriteOrder.remove(app.getPackageName());
                            saveSavedState();
                            loadInstalledApps();
                            Toast.makeText(this, app.getLabel() + " foi ocultado.", Toast.LENGTH_SHORT).show();
                            break;

                        case 6:
                            openAppDetails(app.getPackageName());
                            break;
                    }
                })
                .show();
    }

    private void moveApp(String packageName, boolean isFavoriteSection, int direction) {
        List<String> list = isFavoriteSection ? favoriteOrder : allAppsOrder;
        int idx = list.indexOf(packageName);
        if (idx == -1) return;

        int newIdx = idx + direction;
        if (newIdx >= 0 && newIdx < list.size()) {
            Collections.swap(list, idx, newIdx);
            saveSavedState();
            loadInstalledApps();
        }
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

    private void uninstallApp(String packageName) {
        try {
            Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
            uninstallIntent.setData(Uri.parse("package:" + packageName));
            uninstallIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
            startActivity(uninstallIntent);
        } catch (Exception e) {
            try {
                Intent delIntent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName));
                startActivity(delIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Não foi possível iniciar a desinstalação.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppDetails(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
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
