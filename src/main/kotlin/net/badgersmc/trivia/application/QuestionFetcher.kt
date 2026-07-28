package net.badgersmc.trivia.application

import net.badgersmc.trivia.domain.Question
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Fetches trivia questions from OpenTriviaDB and caches them (REQ-008).
 */
class QuestionFetcher(
    private val httpClient: okhttp3.OkHttpClient,
    private val apiUrl: String,
    private val batchSize: Int,
    private val contentFilter: ContentFilter,
) {
    private val cache: ConcurrentLinkedQueue<Question> = ConcurrentLinkedQueue()

    val isEmpty: Boolean get() = cache.isEmpty()
    val size: Int get() = cache.size

    fun poll(): Question? = cache.poll()

    /** Fetch a batch of questions synchronously from the API. */
    @Suppress("TooGenericExceptionCaught")
    fun fetchQuestions() {
        try {
            val request = okhttp3.Request.Builder()
                .url("$apiUrl?amount=$batchSize&encode=base64")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) return

                val json = com.google.gson.Gson().fromJson(
                    response.body!!.string(), com.google.gson.JsonObject::class.java
                )
                val results = json.getAsJsonArray("results") ?: return

                for (element in results) {
                    val q = element.asJsonObject
                    val decodedQuestion = decodeBase64(q.get("question").asString)
                    val decodedCategory = decodeBase64(q.get("category").asString)

                    if (!contentFilter.isAllowed(decodedQuestion, decodedCategory)) continue

                    cache.offer(
                        Question(
                            question = decodedQuestion,
                            correctAnswer = decodeBase64(q.get("correct_answer").asString),
                            incorrectAnswers = q.getAsJsonArray("incorrect_answers").map {
                                decodeBase64(it.asString)
                            },
                            category = decodedCategory,
                            difficulty = decodeBase64(q.get("difficulty").asString),
                            type = decodeBase64(q.get("type").asString),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Network failures are non-fatal — next game start will retry
        }
    }

    private fun decodeBase64(encoded: String): String =
        String(java.util.Base64.getDecoder().decode(encoded))
}
