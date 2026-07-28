package net.badgersmc.trivia.domain

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionTest {
    private lateinit var multipleChoiceQuestion: Question
    private lateinit var trueFalseQuestion: Question

    @BeforeEach
    fun setUp() {
        multipleChoiceQuestion = Question(
            question = "What is the capital of France?",
            correctAnswer = "Paris",
            incorrectAnswers = listOf("London", "Berlin", "Madrid"),
            category = "Geography",
            difficulty = "easy",
            type = "multiple"
        )

        trueFalseQuestion = Question(
            question = "The Earth is flat.",
            correctAnswer = "False",
            incorrectAnswers = listOf("True"),
            category = "Science",
            difficulty = "easy",
            type = "boolean"
        )
    }

    @Test
    fun `letter answer is correct for multiple choice`() {
        val correctIndex = multipleChoiceQuestion.shuffledAnswers.indexOf("Paris")
        val letter = ('a' + correctIndex).toString()
        assertTrue(multipleChoiceQuestion.isCorrectAnswer(letter))
    }

    @Test
    fun `uppercase letter answer is correct`() {
        val correctIndex = multipleChoiceQuestion.shuffledAnswers.indexOf("Paris")
        val letter = ('A' + correctIndex).toString()
        assertTrue(multipleChoiceQuestion.isCorrectAnswer(letter))
    }

    @Test
    fun `wrong letter answer returns false`() {
        val correctIndex = multipleChoiceQuestion.shuffledAnswers.indexOf("Paris")
        val wrongIndex = if (correctIndex == 0) 1 else 0
        val letter = ('a' + wrongIndex).toString()
        assertFalse(multipleChoiceQuestion.isCorrectAnswer(letter))
    }

    @Test
    fun `case insensitive direct answer is correct`() {
        assertTrue(multipleChoiceQuestion.isCorrectAnswer("PARIS"))
        assertTrue(multipleChoiceQuestion.isCorrectAnswer("paris"))
        assertTrue(multipleChoiceQuestion.isCorrectAnswer("Paris"))
    }

    @Test
    fun `direct wrong answer returns false`() {
        assertFalse(multipleChoiceQuestion.isCorrectAnswer("London"))
    }

    @Test
    fun `true false T and F map correctly`() {
        assertTrue(trueFalseQuestion.isCorrectAnswer("f"))
        assertTrue(trueFalseQuestion.isCorrectAnswer("F"))
        assertTrue(trueFalseQuestion.isCorrectAnswer("false"))
        assertTrue(trueFalseQuestion.isCorrectAnswer("False"))
        assertTrue(trueFalseQuestion.isCorrectAnswer("FALSE"))

        assertFalse(trueFalseQuestion.isCorrectAnswer("t"))
        assertFalse(trueFalseQuestion.isCorrectAnswer("T"))
        assertFalse(trueFalseQuestion.isCorrectAnswer("true"))
    }

    @Test
    fun `shuffled answers contain correct answer`() {
        assertTrue(multipleChoiceQuestion.shuffledAnswers.contains("Paris"))
        assertEquals(4, multipleChoiceQuestion.shuffledAnswers.size)
    }

    @Test
    fun `formatted answers for multiple choice shows ABCD`() {
        val formatted = multipleChoiceQuestion.formattedAnswers
        assertTrue(formatted.contains("<yellow>A)"))
        assertTrue(formatted.contains("<yellow>B)"))
        assertTrue(formatted.contains("<yellow>C)"))
        assertTrue(formatted.contains("<yellow>D)"))
        assertTrue(formatted.contains("Paris"))
        assertTrue(formatted.contains("London"))
    }

    @Test
    fun `formatted answers for boolean shows True as A False as B`() {
        val formatted = trueFalseQuestion.formattedAnswers
        assertTrue(formatted.contains("<yellow>A) <white>True"))
        assertTrue(formatted.contains("<yellow>B) <white>False"))
    }

    @Test
    fun `correctAnswerLetter for boolean returns A or B`() {
        assertEquals("B", trueFalseQuestion.correctAnswerLetter)
    }

    @Test
    fun `correctAnswerLetter for multiple choice matches index`() {
        val letter = multipleChoiceQuestion.correctAnswerLetter
        val index = letter[0] - 'A'
        assertEquals("Paris", multipleChoiceQuestion.shuffledAnswers[index])
    }

    @Test
    fun `getters return constructor values`() {
        assertEquals("What is the capital of France?", multipleChoiceQuestion.question)
        assertEquals("Paris", multipleChoiceQuestion.correctAnswer)
        assertEquals("Geography", multipleChoiceQuestion.category)
        assertEquals("easy", multipleChoiceQuestion.difficulty)
        assertEquals("multiple", multipleChoiceQuestion.type)
        assertEquals(3, multipleChoiceQuestion.incorrectAnswers.size)
        assertEquals(4, multipleChoiceQuestion.shuffledAnswers.size)
    }
}
