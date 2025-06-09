package net.lumalyte.trivia.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final String playerName;
    private boolean muted;
    private long muteExpiry;
    private int totalCorrect;
    private int easyCorrect;
    private int mediumCorrect;
    private int hardCorrect;
    private int points;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.muted = false;
        this.muteExpiry = 0;
        this.totalCorrect = 0;
        this.easyCorrect = 0;
        this.mediumCorrect = 0;
        this.hardCorrect = 0;
        this.points = 0;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isMuted() {
        return muted && !isMuteExpired();
    }

    public boolean isMuteExpired() {
        return muteExpiry > 0 && System.currentTimeMillis() >= muteExpiry;
    }

    public void mute(long duration) {
        this.muted = true;
        this.muteExpiry = System.currentTimeMillis() + duration;
    }

    public void unmute() {
        this.muted = false;
        this.muteExpiry = 0;
    }

    public void addCorrectAnswer(String difficulty) {
        this.totalCorrect++;
        switch (difficulty.toLowerCase()) {
            case "easy":
                this.easyCorrect++;
                this.points += 1;
                break;
            case "medium":
                this.mediumCorrect++;
                this.points += 2;
                break;
            case "hard":
                this.hardCorrect++;
                this.points += 3;
                break;
        }
    }

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public int getEasyCorrect() {
        return easyCorrect;
    }

    public int getMediumCorrect() {
        return mediumCorrect;
    }

    public int getHardCorrect() {
        return hardCorrect;
    }

    public int getPoints() {
        return points;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
} 