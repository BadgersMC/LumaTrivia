package net.lumalyte.trivia.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.lumalyte.trivia.TriviaPlugin;
import net.lumalyte.trivia.models.Question;
import net.lumalyte.trivia.util.Base64Decoder;
import net.lumalyte.trivia.util.ContentFilter;
import net.lumalyte.trivia.util.MessageUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TriviaManager {
    private final TriviaPlugin plugin;
    private final OkHttpClient httpClient;
    private final Queue<Question> questionCache;
    private final LeaderboardManager leaderboardManager;
    private final List<BukkitTask> scheduledGames;
    private final Set<UUID> answeredPlayers;
    private final ContentFilter contentFilter;
    private Question currentQuestion;
    private boolean gameActive;
    private int taskId;
    private long globalCooldown;

    public TriviaManager(TriviaPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        this.questionCache = new ConcurrentLinkedQueue<>();
        this.leaderboardManager = new LeaderboardManager(plugin);
        this.scheduledGames = new ArrayList<>();
        this.answeredPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.contentFilter = new ContentFilter(plugin);
        this.gameActive = false;
        setupScheduledGames();
    }

    public TriviaPlugin getPlugin() {
        return plugin;
    }

    public void startGame() {
        if (gameActive) {
            plugin.getLogger().info("Game not started: Another game is already active");
            return;
        }

        // Check global cooldown
        if (System.currentTimeMillis() < globalCooldown) {
            long remaining = (globalCooldown - System.currentTimeMillis()) / 1000;
            plugin.getLogger().info("Game not started: On cooldown for " + remaining + " seconds");
            String cooldownMsg = plugin.getConfig().getString("messages.game.cooldown");
            if (cooldownMsg == null) {
                plugin.getLogger().severe("Missing config: messages.game.cooldown");
                return;
            }
            MessageUtil.broadcast(cooldownMsg.replace("%time%", String.valueOf(remaining)));
            return;
        }

        if (questionCache.isEmpty()) {
            plugin.getLogger().info("Cache empty, fetching new questions from API...");
            fetchQuestions();
            return;
        }

        // Clear answered players from previous game
        answeredPlayers.clear();
        currentQuestion = questionCache.poll();
        gameActive = true;
        plugin.getLogger().info("Starting new trivia game! Questions in cache: " + questionCache.size());

        // Get message templates
        String questionTemplate = plugin.getConfig().getString("messages.game.question");
        String optionsTemplate = plugin.getConfig().getString("messages.game.options");
        
        if (questionTemplate == null || optionsTemplate == null) {
            plugin.getLogger().severe("Missing config: messages.game.question or messages.game.options");
            gameActive = false;
            return;
        }

        // Announce question and options
        String questionMsg = questionTemplate.replace("%question%", currentQuestion.getQuestion());
        String optionsMsg = optionsTemplate.replace("%answers%", currentQuestion.getFormattedAnswers());
        
        MessageUtil.broadcast(questionMsg);
        MessageUtil.broadcast(optionsMsg);

        // Start timer
        int answerTime = plugin.getConfig().getInt("game.answer-time", 30);
        plugin.getLogger().info("Question timer set for " + answerTime + " seconds");
        
        taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (gameActive) {
                timeUp();
            }
        }, answerTime * 20L); // Convert to ticks

        // Set global cooldown
        int cooldown = plugin.getConfig().getInt("game.cooldown", 300);
        globalCooldown = System.currentTimeMillis() + (cooldown * 1000L);
        plugin.getLogger().info("Set cooldown for " + cooldown + " seconds");
    }

    public boolean hasPlayerAnswered(UUID playerId) {
        return answeredPlayers.contains(playerId);
    }

    public void checkAnswer(Player player, String answer) {
        if (!gameActive || currentQuestion == null) {
            return;
        }

        // Check if player has already answered
        if (hasPlayerAnswered(player.getUniqueId())) {
            String alreadyAnsweredMsg = plugin.getConfig().getString("messages.game.already-answered");
            if (alreadyAnsweredMsg != null) {
                MessageUtil.sendMessage(player, alreadyAnsweredMsg);
            }
            return;
        }

        // Mark player as having answered
        answeredPlayers.add(player.getUniqueId());

        if (currentQuestion.isCorrectAnswer(answer)) {
            gameActive = false;
            Bukkit.getScheduler().cancelTask(taskId);
            
            // Announce winner
            String winMsg = plugin.getConfig().getString("messages.game.correct-answer")
                .replace("%player%", player.getName())
                .replace("%answer%", currentQuestion.getCorrectAnswer())
                .replace("%letter%", currentQuestion.getCorrectAnswerLetter());
            MessageUtil.broadcast(winMsg);

            // Update stats
            leaderboardManager.addCorrectAnswer(player, currentQuestion.getDifficulty());

            // Give rewards
            giveRewards(player, currentQuestion.getDifficulty());
        } else {
            // Handle wrong answer
            String wrongAnswerMsg = plugin.getConfig().getString("messages.game.wrong-answer");
            if (wrongAnswerMsg != null) {
                wrongAnswerMsg = wrongAnswerMsg
                    .replace("%player%", player.getName())
                    .replace("%answer%", answer);
                MessageUtil.broadcast(wrongAnswerMsg);
            }
        }
    }

    private void timeUp() {
        if (!gameActive) {
            return;
        }

        gameActive = false;
        String timeUpTemplate = plugin.getConfig().getString("messages.game.time-up");
        if (timeUpTemplate == null) {
            plugin.getLogger().severe("Missing config: messages.game.time-up");
            return;
        }
        
        String timeUpMsg = timeUpTemplate
            .replace("%answer%", currentQuestion.getCorrectAnswer())
            .replace("%letter%", currentQuestion.getCorrectAnswerLetter());
        MessageUtil.broadcast(timeUpMsg);
    }

    private void giveRewards(Player player, String difficulty) {
        ConfigurationSection rewards = plugin.getConfig()
            .getConfigurationSection("rewards." + difficulty.toLowerCase());
        if (rewards == null) return;

        // Run commands on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String command : rewards.getStringList("commands")) {
                String finalCommand = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            }
        });
    }

    private void fetchQuestions() {
        plugin.getLogger().info("Fetching questions from OpenTriviaDB...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int batchSize = plugin.getConfig().getInt("api.batch-size", 24);
                String url = plugin.getConfig().getString("api.url") + 
                    "?amount=" + batchSize + "&encode=base64";

                plugin.getLogger().info("Making API request to: " + url);
                Request request = new Request.Builder()
                    .url(url)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        plugin.getLogger().warning("Failed to fetch questions from API: " + response.code());
                        return;
                    }

                    JsonObject json = new Gson().fromJson(response.body().string(), JsonObject.class);
                    JsonArray results = json.getAsJsonArray("results");
                    plugin.getLogger().info("Received " + results.size() + " questions from API");

                    int filtered = 0;
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject q = results.get(i).getAsJsonObject();
                        String decodedQuestion = Base64Decoder.decode(q.get("question").getAsString());
                        String decodedCategory = Base64Decoder.decode(q.get("category").getAsString());
                        
                        // Apply content filter
                        if (!contentFilter.isAllowed(decodedQuestion, decodedCategory)) {
                            filtered++;
                            continue;
                        }

                        Question question = new Question(
                            decodedQuestion,
                            Base64Decoder.decode(q.get("correct_answer").getAsString()),
                            Base64Decoder.decodeList(q.get("incorrect_answers").getAsJsonArray()
                                .asList().stream()
                                .map(e -> e.getAsString())
                                .toList()),
                            decodedCategory,
                            Base64Decoder.decode(q.get("difficulty").getAsString()),
                            Base64Decoder.decode(q.get("type").getAsString())
                        );
                        questionCache.offer(question);
                    }

                    if (filtered > 0) {
                        plugin.getLogger().info("Filtered " + filtered + " questions based on content rules");
                    }

                    // If a game was waiting for questions, start it now
                    if (!gameActive && !questionCache.isEmpty()) {
                        plugin.getLogger().info("Questions fetched, starting pending game...");
                        Bukkit.getScheduler().runTask(plugin, this::startGame);
                    } else if (questionCache.isEmpty()) {
                        plugin.getLogger().warning("All questions were filtered! Trying to fetch more...");
                        Bukkit.getScheduler().runTaskLater(plugin, this::fetchQuestions, 20L); // Try again in 1 second
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching questions: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void setupScheduledGames() {
        // Cancel any existing scheduled games
        scheduledGames.forEach(BukkitTask::cancel);
        scheduledGames.clear();

        if (!plugin.getConfig().getBoolean("game.schedule.enabled", false)) {
            return;
        }

        List<String> times = plugin.getConfig().getStringList("game.schedule.times");
        for (String timeStr : times) {
            try {
                LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
                Calendar now = Calendar.getInstance();
                Calendar next = Calendar.getInstance();
                next.set(Calendar.HOUR_OF_DAY, time.getHour());
                next.set(Calendar.MINUTE, time.getMinute());
                next.set(Calendar.SECOND, 0);

                if (next.before(now)) {
                    next.add(Calendar.DAY_OF_MONTH, 1);
                }

                long delay = (next.getTimeInMillis() - now.getTimeInMillis()) / 50; // Convert to ticks
                BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::startGame, delay, 24 * 60 * 60 * 20); // Run daily
                scheduledGames.add(task);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid time format in schedule: " + timeStr);
            }
        }
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
} 