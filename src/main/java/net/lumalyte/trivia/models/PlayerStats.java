package net.lumalyte.trivia.models;

import java.util.UUID;

public class PlayerStats implements Comparable<PlayerStats> {
    private final UUID playerId;
    private final String playerName;
    private int totalCorrect;
    private int easyCorrect;
    private int mediumCorrect;
    private int hardCorrect;
    private int points;

    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.totalCorrect = 0;
        this.easyCorrect = 0;
        this.mediumCorrect = 0;
        this.hardCorrect = 0;
        this.points = 0;
    }

    public void addCorrectAnswer(String difficulty) {
        totalCorrect++;
        switch (difficulty.toLowerCase()) {
            case "easy":
                easyCorrect++;
                points += 1;
                break;
            case "medium":
                mediumCorrect++;
                points += 2;
                break;
            case "hard":
                hardCorrect++;
                points += 3;
                break;
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
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

    @Override
    public int compareTo(PlayerStats other) {
        return Integer.compare(other.points, this.points); // Sort by points descending
    }
} 