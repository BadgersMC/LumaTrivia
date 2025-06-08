package net.lumalyte.trivia.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class QuestionTest {
    private Question multipleChoiceQuestion;
    private Question trueFalseQuestion;

    @BeforeEach
    void setUp() {
        // Set up a multiple choice question
        multipleChoiceQuestion = new Question(
            "What is the capital of France?",
            "Paris",
            Arrays.asList("London", "Berlin", "Madrid"),
            "Geography",
            "easy",
            "multiple"
        );

        // Set up a true/false question
        trueFalseQuestion = new Question(
            "The Earth is flat.",
            "False",
            List.of("True"),
            "Science",
            "easy",
            "boolean"
        );
    }

    @Test
    void testMultipleChoiceAnswerValidation() {
        // Test letter-based answers
        assertTrue(multipleChoiceQuestion.isCorrectAnswer(
            Character.toString((char)('a' + multipleChoiceQuestion.getShuffledAnswers().indexOf("Paris")))
        ));

        // Test case insensitivity
        assertTrue(multipleChoiceQuestion.isCorrectAnswer("PARIS"));
        assertTrue(multipleChoiceQuestion.isCorrectAnswer("paris"));
        assertTrue(multipleChoiceQuestion.isCorrectAnswer("Paris"));
    }

    @Test
    void testTrueFalseAnswerValidation() {
        // Test various ways to answer false
        assertTrue(trueFalseQuestion.isCorrectAnswer("f"));
        assertTrue(trueFalseQuestion.isCorrectAnswer("F"));
        assertTrue(trueFalseQuestion.isCorrectAnswer("false"));
        assertTrue(trueFalseQuestion.isCorrectAnswer("False"));
        assertTrue(trueFalseQuestion.isCorrectAnswer("FALSE"));

        // Test various ways to answer true (should be incorrect)
        assertFalse(trueFalseQuestion.isCorrectAnswer("t"));
        assertFalse(trueFalseQuestion.isCorrectAnswer("T"));
        assertFalse(trueFalseQuestion.isCorrectAnswer("true"));
        assertFalse(trueFalseQuestion.isCorrectAnswer("True"));
        assertFalse(trueFalseQuestion.isCorrectAnswer("TRUE"));
    }

    @Test
    void testFormattedAnswers() {
        // Test multiple choice formatting
        String mcAnswers = multipleChoiceQuestion.getFormattedAnswers();
        assertTrue(mcAnswers.contains("&eA)"));
        assertTrue(mcAnswers.contains("&eB)"));
        assertTrue(mcAnswers.contains("&eC)"));
        assertTrue(mcAnswers.contains("&eD)"));
        assertTrue(mcAnswers.contains("&f" + "Paris"));
        assertTrue(mcAnswers.contains("&f" + "London"));
        assertTrue(mcAnswers.contains("&f" + "Berlin"));
        assertTrue(mcAnswers.contains("&f" + "Madrid"));

        // Test true/false formatting
        String tfAnswers = trueFalseQuestion.getFormattedAnswers();
        assertTrue(tfAnswers.contains("&eA) &fTrue"));
        assertTrue(tfAnswers.contains("&eB) &fFalse"));
    }

    @Test
    void testGetCorrectAnswerLetter() {
        // Test true/false letter
        assertEquals("B", trueFalseQuestion.getCorrectAnswerLetter()); // False is always B

        // Test multiple choice letter
        String letter = multipleChoiceQuestion.getCorrectAnswerLetter();
        int index = letter.charAt(0) - 'A';
        assertEquals("Paris", multipleChoiceQuestion.getShuffledAnswers().get(index));
    }

    @Test
    void testGetters() {
        assertEquals("What is the capital of France?", multipleChoiceQuestion.getQuestion());
        assertEquals("Paris", multipleChoiceQuestion.getCorrectAnswer());
        assertEquals("Geography", multipleChoiceQuestion.getCategory());
        assertEquals("easy", multipleChoiceQuestion.getDifficulty());
        assertEquals("multiple", multipleChoiceQuestion.getType());
        assertEquals(3, multipleChoiceQuestion.getIncorrectAnswers().size());
        assertEquals(4, multipleChoiceQuestion.getShuffledAnswers().size());
    }
} 