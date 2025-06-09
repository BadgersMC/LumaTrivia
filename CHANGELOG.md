# Changelog

All notable changes to LumaTrivia will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2024-03-19
### Fixed
- Fixed mute system not working properly with other chat plugins
- Improved chat event handling for better plugin compatibility
- Fixed stats loading causing NullPointerException on server start
- Fixed true/false answer validation

## [1.0.0] - 2024-03-19
### Added
- Initial release
- Basic trivia game functionality
- Multiple choice and true/false questions
- Points system and leaderboard
- Temporary muting for incorrect answers
- Scheduled games support

### Features
- Dynamic trivia questions from OpenTriviaDB
- Various difficulty levels (Easy, Medium, Hard)
- Point-based reward system with configurable rewards
- Player statistics and leaderboard tracking
- Scheduled games with configurable times
- Async question fetching and caching
- Content filtering system for family-friendly questions

### Commands
- `/trivia start` - Start a new trivia game
- `/trivia stats [player]` - View player statistics
- `/trivia leaderboard` - View global leaderboard
- `/trivia reload` - Reload plugin configuration

### Configuration
- Customizable messages and colors
- Configurable game settings:
  - Answer time limit
  - Game cooldown
  - Scheduled game times
  - Reward commands per difficulty
- Content filtering options:
  - Regex-based blocked patterns
  - Required patterns
  - Logging of filtered questions
- Muting system settings:
  - Enable/disable muting
  - Custom mute messages
  - Message visibility options

### Technical Details
- Built for Paper 1.20.4
- Async operations for performance
- Modern chat API implementation
- Comprehensive test coverage
- Automated dependency updates via Dependabot

### API
- Public API for plugin integration
- Event system for game interactions
- Player statistics access
- Game state management

For more information, see the [README.md](README.md) file.

### Changed
- N/A (Initial release)

### Deprecated
- N/A (Initial release)

### Removed
- N/A (Initial release)

### Fixed
- N/A (Initial release)

### Security
- N/A (Initial release) 