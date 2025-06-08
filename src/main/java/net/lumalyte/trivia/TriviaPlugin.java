package net.lumalyte.trivia;

import net.lumalyte.trivia.commands.TriviaCommand;
import net.lumalyte.trivia.listeners.ChatListener;
import net.lumalyte.trivia.managers.TriviaManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class TriviaPlugin extends JavaPlugin {
    private static TriviaPlugin instance;
    private TriviaManager triviaManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        getLogger().info("Configuration loaded");
        
        // Initialize managers
        this.triviaManager = new TriviaManager(this);
        getLogger().info("TriviaManager initialized");
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(triviaManager), this);
        getLogger().info("Chat listener registered");
        
        // Register commands
        TriviaCommand triviaCommand = new TriviaCommand(this, triviaManager);
        getServer().getCommandMap().register("lumatrivia", triviaCommand);
        getLogger().info("Commands registered");
        
        // Log scheduled game status
        if (getConfig().getBoolean("game.schedule.enabled", false)) {
            getLogger().info("Scheduled games enabled. Times: " + 
                getConfig().getStringList("game.schedule.times"));
        } else {
            getLogger().info("Scheduled games disabled");
        }
        
        getLogger().info("LumaTrivia has been enabled! Use /trivia start to begin a game.");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("LumaTrivia has been disabled!");
    }
    
    public static TriviaPlugin getInstance() {
        return instance;
    }
    
    public TriviaManager getTriviaManager() {
        return triviaManager;
    }
} 