package org.lingolocal.project.presentation.textmanager

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.lingolocal.project.presentation.theme.LingoIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import lingolocal.composeapp.generated.resources.Res
import lingolocal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.lingolocal.project.domain.model.Quiz

class QuizScreen(private val quiz: Quiz) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<QuizScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        // Inizializza il quiz nel ScreenModel
        LaunchedEffect(quiz) {
            screenModel.initQuiz(quiz)
        }

        val state = uiState ?: return // Attendiamo l'inizializzazione

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.quiz.title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        // Impediamo uscite accidentali, ma lasciamo un tasto Esci
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = LingoIcons.Close, contentDescription = "Esci")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    )
            ) {
                if (state.isCompleted) {
                    QuizResultsDashboard(
                        score = state.score,
                        totalQuestions = state.quiz.questions.size,
                        isSaving = state.isSaving,
                        onBackToHome = {
                            navigator.popUntilRoot()
                        }
                    )
                } else {
                    QuizActiveContent(
                        state = state,
                        onSelectOption = screenModel::selectOption,
                        onValidate = screenModel::validateAnswer,
                        onNext = screenModel::nextQuestion
                    )
                }
            }
        }
    }

    @Composable
    private fun QuizActiveContent(
        state: QuizUiState,
        onSelectOption: (Int) -> Unit,
        onValidate: () -> Unit,
        onNext: () -> Unit
    ) {
        val currentQuestion = state.currentQuestion ?: return
        val totalQuestions = state.quiz.questions.size
        val progress = (state.currentQuestionIndex + 1).toFloat() / totalQuestions

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indicatore di avanzamento premium
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            Res.string.quiz_question_progress,
                            state.currentQuestionIndex + 1,
                            totalQuestions
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score: ${state.score}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            // Box Domanda
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .heightIn(min = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Opzioni di Risposta ("Crocette")
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                currentQuestion.options.forEachIndexed { index, option ->
                    QuizOptionRow(
                        option = option,
                        index = index,
                        isSelected = state.selectedOptionIndex == index,
                        isValidated = state.isValidated,
                        isCorrectOption = currentQuestion.correctOptionIndex == index,
                        onSelect = { onSelectOption(index) }
                    )
                }
            }

            // Area Convalida e Spiegazione Didattica
            AnimatedVisibility(
                visible = state.isValidated,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val isCorrect = state.selectedOptionIndex == currentQuestion.correctOptionIndex
                val feedbackColor = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                val feedbackBg = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = feedbackBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, feedbackColor.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCorrect) LingoIcons.Check else LingoIcons.Close,
                                contentDescription = null,
                                tint = feedbackColor
                            )
                            Text(
                                text = if (isCorrect) stringResource(Res.string.correct_answer) else stringResource(Res.string.incorrect_answer),
                                fontWeight = FontWeight.Bold,
                                color = feedbackColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        HorizontalDivider(color = feedbackColor.copy(alpha = 0.15f), thickness = 1.dp)

                        Text(
                            text = stringResource(Res.string.explanation_label),
                            fontWeight = FontWeight.SemiBold,
                            color = feedbackColor.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottone d'azione a fondo pagina
            Button(
                onClick = {
                    if (state.isValidated) {
                        onNext()
                    } else {
                        onValidate()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = state.selectedOptionIndex != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isValidated) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state.isValidated) {
                        stringResource(Res.string.next_question_button)
                    } else {
                        stringResource(Res.string.validate_answer_button)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    @Composable
    private fun QuizOptionRow(
        option: String,
        index: Int,
        isSelected: Boolean,
        isValidated: Boolean,
        isCorrectOption: Boolean,
        onSelect: () -> Unit
    ) {
        val backgroundColor = when {
            isValidated && isCorrectOption -> Color(0xFFE8F5E9) // Verde per risposta esatta
            isValidated && isSelected && !isCorrectOption -> Color(0xFFFFEBEE) // Rosso per risposta errata selezionata
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) // Selezione temporanea
            else -> MaterialTheme.colorScheme.surface
        }

        val borderColor = when {
            isValidated && isCorrectOption -> Color(0xFF2E7D32)
            isValidated && isSelected && !isCorrectOption -> Color(0xFFC62828)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }

        val borderWidth = if (isSelected || (isValidated && (isCorrectOption || isSelected))) 2.dp else 1.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !isValidated, onClick = onSelect),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = BorderStroke(borderWidth, borderColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cerchietto con la lettera dell'opzione (A, B, C, D)
                val letter = ('A' + index).toString()
                val badgeColor = when {
                    isValidated && isCorrectOption -> Color(0xFF2E7D32)
                    isValidated && isSelected && !isCorrectOption -> Color(0xFFC62828)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Icona di spunta/croce se validato
                if (isValidated) {
                    if (isCorrectOption) {
                        Icon(
                            imageVector = LingoIcons.Check,
                            contentDescription = "Corretto",
                            tint = Color(0xFF2E7D32)
                        )
                    } else if (isSelected) {
                        Icon(
                            imageVector = LingoIcons.Close,
                            contentDescription = "Errato",
                            tint = Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun QuizResultsDashboard(
        score: Int,
        totalQuestions: Int,
        isSaving: Boolean,
        onBackToHome: () -> Unit
    ) {
        val percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions) else 0f
        val ratingMessage = when {
            percentage >= 0.8f -> "Straordinario! Comprensione eccellente."
            percentage >= 0.6f -> "Ottimo lavoro! Ci sei quasi!"
            else -> "Buon tentativo! Continua a studiare e migliorerai."
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.quiz_results_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    // Cerchio grande con punteggio
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score/$totalQuestions",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 36.sp
                            )
                            Text(
                                text = "Punteggio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Messaggio di valutazione
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = ratingMessage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Risultato salvato nello storico offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(Res.string.saving_result))
                        } else {
                            Text(stringResource(Res.string.back_to_home_button))
                        }
                    }
                }
            }
        }
    }
}
