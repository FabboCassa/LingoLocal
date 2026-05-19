package org.lingolocal.project.presentation.textmanager

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.lingolocal.project.domain.model.Quiz
import org.lingolocal.project.domain.model.QuizQuestion
import org.lingolocal.project.domain.usecase.SaveQuizResultUseCase
import org.lingolocal.project.util.logInfo

data class QuizUiState(
    val quiz: Quiz,
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isValidated: Boolean = false,
    val score: Int = 0,
    val isCompleted: Boolean = false,
    val isSaving: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = quiz.questions.getOrNull(currentQuestionIndex)
}

class QuizScreenModel(
    private val saveQuizResultUseCase: SaveQuizResultUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<QuizUiState?>(null)
    val uiState: StateFlow<QuizUiState?> = _uiState.asStateFlow()

    fun initQuiz(quiz: Quiz) {
        if (_uiState.value == null) {
            _uiState.value = QuizUiState(quiz = quiz)
        }
    }

    fun selectOption(index: Int) {
        val state = _uiState.value ?: return
        if (state.isValidated) return // non si può cambiare risposta dopo la convalida
        _uiState.value = state.copy(selectedOptionIndex = index)
    }

    fun validateAnswer() {
        val state = _uiState.value ?: return
        val selected = state.selectedOptionIndex ?: return
        if (state.isValidated) return

        val question = state.currentQuestion ?: return
        val isCorrect = selected == question.correctOptionIndex
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.value = state.copy(
            isValidated = true,
            score = newScore
        )
    }

    fun nextQuestion() {
        val state = _uiState.value ?: return
        if (!state.isValidated) return

        val nextIndex = state.currentQuestionIndex + 1
        if (nextIndex < state.quiz.questions.size) {
            _uiState.value = state.copy(
                currentQuestionIndex = nextIndex,
                selectedOptionIndex = null,
                isValidated = false
            )
        } else {
            // Quiz terminato! Salviamo il risultato nel database locale
            completeAndSaveQuiz()
        }
    }

    private fun completeAndSaveQuiz() {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(isSaving = true)

        screenModelScope.launch {
            logInfo(TAG, "Salvataggio punteggio quiz '${state.quiz.title}': ${state.score}/${state.quiz.questions.size}")
            try {
                saveQuizResultUseCase(
                    title = state.quiz.title,
                    questionsCount = state.quiz.questions.size,
                    correctAnswers = state.score
                )
            } catch (t: Throwable) {
                logInfo(TAG, "Errore salvataggio punteggio quiz: ${t.message}")
            } finally {
                _uiState.value = _uiState.value?.copy(
                    isSaving = false,
                    isCompleted = true
                )
            }
        }
    }

    private companion object {
        private const val TAG = "QuizScreenModel"
    }
}
