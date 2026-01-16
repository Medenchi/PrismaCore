package com.plasma.core;

import com.plasma.core.database.Database;
import com.plasma.core.modules.auth.AuthManager;
import com.plasma.core.modules.homes.HomesManager;
import com.plasma.core.modules.compass.CompassManager;
import com.plasma.core.modules.coins.CoinsManager;
import com.plasma.core.modules.market.MarketManager;
import com.plasma.core.modules.gender.GenderManager;
import com.plasma.core.modules.logs.LogsManager;
import com.plasma.core.modules.admin.AdminManager;
import com.plasma.core.modules.hud.HUDManager;
import com.plasma.core.modules.scoreboard.ScoreboardManager;
import com.plasma.core.modules.tab.TabManager;
import com.plasma.core.modules.damage.DamageIndicator;
import com.plasma.core.modules.sit.SitManager;
import com.plasma.core.modules.trades.TradesManager;
import com.plasma.core.modules.heads.HeadsManager;
import com.plasma.core.modules.friends.FriendsManager;
import com.plasma.core.modules.emotes.EmotesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PlasmaCore extends JavaPlugin {

    private static PlasmaCore instance;
    private Database database;
    private AuthManager authManager;
    private HomesManager homesManager;
    private CompassManager compassManager;
    private CoinsManager coinsManager;
    private MarketManager marketManager;
    private GenderManager genderManager;
    private LogsManager logsManager;
    private AdminManager adminManager;
    private HUDManager hudManager;
    private ScoreboardManager scoreboardManager;
    private TabManager tabManager;
    private DamageIndicator damageIndicator;
    private SitManager sitManager;
    private TradesManager tradesManager;
    private HeadsManager headsManager;
    private FriendsManager friendsManager;
    private EmotesManager emotesManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        getLogger().info("§b╔═══════════════════════════════════╗");
        getLogger().info("§b║     §f§lPLASMA§b§lCORE §bv1.0.0          ║");
        getLogger().info("§b╚═══════════════════════════════════╝");
        
        database = new Database(this);
        database.initialize();
        
        initModules();
        
        getLogger().info("§a✓ PlasmaCore успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (sitManager != null) sitManager.unsitAll();
        if (database != null) database.close();
        getLogger().info("§c✗ PlasmaCore отключен!");
    }

    private void initModules() {
        if (getConfig().getBoolean("auth.enabled")) {
            authManager = new AuthManager(this);
        }
        homesManager = new HomesManager(this);
        compassManager = new CompassManager(this);
        coinsManager = new CoinsManager(this);
        marketManager = new MarketManager(this);
        if (getConfig().getBoolean("gender.enabled")) {
            genderManager = new GenderManager(this);
        }
        if (getConfig().getBoolean("logs.enabled")) {
            logsManager = new LogsManager(this);
        }
        adminManager = new AdminManager(this);
        if (getConfig().getBoolean("hud.enabled")) {
            hudManager = new HUDManager(this);
        }
        if (getConfig().getBoolean("scoreboard.enabled")) {
            scoreboardManager = new ScoreboardManager(this);
        }
        if (getConfig().getBoolean("tab.enabled")) {
            tabManager = new TabManager(this);
        }
        if (getConfig().getBoolean("damage-indicator.enabled")) {
            damageIndicator = new DamageIndicator(this);
        }
        if (getConfig().getBoolean("sit.enabled")) {
            sitManager = new SitManager(this);
        }
        if (getConfig().getBoolean("trades.enabled")) {
            tradesManager = new TradesManager(this);
        }
        if (getConfig().getBoolean("heads.enabled")) {
            headsManager = new HeadsManager(this);
        }
        friendsManager = new FriendsManager(this);
        if (getConfig().getBoolean("emotes.enabled")) {
            emotesManager = new EmotesManager(this);
        }
    }

    public static PlasmaCore getInstance() { return instance; }
    public Database getDatabase() { return database; }
    public AuthManager getAuthManager() { return authManager; }
    public HomesManager getHomesManager() { return homesManager; }
    public CompassManager getCompassManager() { return compassManager; }
    public CoinsManager getCoinsManager() { return coinsManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public GenderManager getGenderManager() { return genderManager; }
    public LogsManager getLogsManager() { return logsManager; }
    public AdminManager getAdminManager() { return adminManager; }
    public HUDManager getHudManager() { return hudManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TabManager getTabManager() { return tabManager; }
    public SitManager getSitManager() { return sitManager; }
    public TradesManager getTradesManager() { return tradesManager; }
    public HeadsManager getHeadsManager() { return headsManager; }
    public FriendsManager getFriendsManager() { return friendsManager; }
    public EmotesManager getEmotesManager() { return emotesManager; }
}
