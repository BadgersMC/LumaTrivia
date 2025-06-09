package net.lumalyte.trivia.models;

import net.lumalyte.trivia.util.MessageUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    private final String question;
    private final String correctAnswer;
    private final List<String> incorrectAnswers;
    private final String category;
    private final String difficulty;
    private final String type;
    private final List<String> shuffledAnswers;
    private final int correctAnswerIndex;

    public Question(String question, String correctAnswer, List<String> incorrectAnswers,
                   String category, String difficulty, String type) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.incorrectAnswers = incorrectAnswers;
        this.category = category;
        this.difficulty = difficulty;
        this.type = type;

        // Create and shuffle answers
        this.shuffledAnswers = new ArrayList<>(incorrectAnswers);
        this.shuffledAnswers.add(correctAnswer);
        Collections.shuffle(this.shuffledAnswers);
        this.correctAnswerIndex = this.shuffledAnswers.indexOf(correctAnswer);
    }

    public String getQuestion() {
        return question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public String getCategory() {
        return category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getType() {
        return type;
    }

    public List<String> getShuffledAnswers() {
        return shuffledAnswers;
    }

    public boolean isCorrectAnswer(String answer) {
        // Strip color codes and convert answer to lowercase for case-insensitive comparison
        answer = MessageUtil.stripColor(answer.toLowerCase().trim());

        // Handle true/false questions
        if (type.equalsIgnoreCase("boolean")) {
            // Convert T/F to True/False
            if (answer.equals("t")) answer = "true";
            if (answer.equals("f")) answer = "false";
            return correctAnswer.equalsIgnoreCase(answer);
        }

        // Handle multiple choice questions
        if (answer.length() == 1) {
            int index = answer.charAt(0) - 'a';
            return index == correctAnswerIndex;
        }

        // Direct answer comparison
        return correctAnswer.equalsIgnoreCase(answer);
    }

    public String getFormattedAnswers() {
        StringBuilder sb = new StringBuilder();
        
        // For true/false questions, always show True as A and False as B
        if (type.equalsIgnoreCase("boolean")) {
            sb.append("\n&eA) &fTrue");
            sb.append("\n&eB) &fFalse");
            return sb.toString();
        }

        // For multiple choice, show all shuffled answers
        for (int i = 0; i < shuffledAnswers.size(); i++) {
            sb.append("\n&e").append((char)('A' + i)).append(") &f")
              .append(shuffledAnswers.get(i));
        }
        return sb.toString();
    }

    public String getCorrectAnswerLetter() {
        // For true/false questions, determine if True is correct (A) or False is correct (B)
        if (type.equalsIgnoreCase("boolean")) {
            return correctAnswer.equalsIgnoreCase("true") ? "A" : "B";
        }
        
        // For multiple choice, use the index
        return String.valueOf((char)('A' + correctAnswerIndex));
    }
} 