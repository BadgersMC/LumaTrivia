# LumaTrivia

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/140ea0d145e7454caed55e18955214e3)](https://app.codacy.com/gh/BadgersMC/LumaTrivia?utm_source=github.com&utm_medium=referral&utm_content=BadgersMC/LumaTrivia&utm_campaign=Badge_Grade)

A Minecraft trivia game plugin powered by OpenTriviaDB. Challenge your players with questions across various categories and difficulties!
Make these darn iPad kids learn a thing or two while they play block game.
Powered by https://opentdb.com/

![KnOwLeDgE](https://github.com/user-attachments/assets/dd7507df-4521-40ca-bdc6-7f8595874f85)
## Features

- Multiple choice and true/false questions
- Questions from various categories (General Knowledge, Video Games, Science & Nature, etc.)
- Three difficulty levels (Easy, Medium, Hard)
- Point-based reward system
- Player statistics and leaderboard
- Configurable cooldowns and timers
- Optional scheduled games
- Async question fetching and caching
- Configurable messages and colors
![trivia_example](https://github.com/user-attachments/assets/54e50c28-4fbd-49dc-a38b-f8cf2e004a79)
![trivia_leaderboard](https://github.com/user-attachments/assets/1aaca8c6-2f4b-4d15-88b6-348a3edce8b3)
![trivia_stats](https://github.com/user-attachments/assets/0c1f2b79-7b05-4f2e-bddf-5c18f7b4212e)

## Installation

1. Download the latest release from the [releases page](https://github.com/BadgersMC/LumaTrivia/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/LumaTrivia/config.yml`

## Commands

- `/trivia start` - Start a new trivia game
- `/trivia stats` - View your trivia statistics
- `/trivia top` - View the leaderboard
- `/trivia reload` - Reload the plugin configuration

## Permissions

- `lumatrivia.use` (default: true) - Allows viewing stats/leaderboard
- `lumatrivia.start` (default: op) - Allows starting games
- `lumatrivia.admin` (default: op) - Allows reload command

## Configuration

The plugin is highly configurable. Here's an example configuration:

```yaml
# Game Settings
game:
  answer-time: 30    # Seconds to answer each question
  cooldown: 300      # Seconds between trivia games
  schedule:          # Optional scheduled games
    enabled: true
    times:
      - "12:00"
      - "18:00"
      - "21:00"

# Reward Settings
rewards:
  easy:
    commands:
      - "eco give %player% 100"
      - "give %player% diamond 1"
    points: 1
  medium:
    commands:
      - "eco give %player% 250"
      - "give %player% diamond 3"
    points: 2
  hard:
    commands:
      - "eco give %player% 500"
      - "give %player% diamond 5"
    points: 3
```

See the [Configuration Guide](https://github.com/BadgersMC/LumaTrivia/wiki/Configuration) for full documentation.

## API Usage

LumaTrivia provides an API for other plugins to interact with. Here's an example:

```java
public class ExamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Get LumaTrivia instance
        TriviaPlugin triviaPlugin = TriviaPlugin.getInstance();
        
        // Get player stats
        PlayerStats stats = triviaPlugin.getTriviaManager()
            .getLeaderboardManager()
            .getPlayerStats(player.getUniqueId());
                
        // Start a game
        triviaPlugin.getTriviaManager().startGame();
            
        // Listen for chat answers
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerChat(AsyncChatEvent event) {
                if (event.message() instanceof TextComponent textComponent) {
                    String answer = textComponent.content().trim().toLowerCase();
                    // Process the answer as needed
                    getLogger().info(event.getPlayer().getName() + " answered: " + answer);
                }
            }
        }, this);
    }
}
```

## Building

This plugin uses Gradle. To build:

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Testing

Run the test suite:

```bash
./gradlew test
```

The plugin includes:
- Unit tests for core functionality
- Integration tests with Paper API
- Test coverage reporting

## Dependencies

- Paper 1.20.4+
- Java 17+

## Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting pull requests.

## License

This project is licensed under the GNU GENERAL PUBLIC LICENSE - see the LICENSE file for details.

## Support

- [Issue Tracker](https://github.com/BadgersMC/LumaTrivia/issues)
- [Discord Server](https://discord.gg/badgersmc)
- [Wiki](https://github.com/BadgersMC/LumaTrivia/wiki)
