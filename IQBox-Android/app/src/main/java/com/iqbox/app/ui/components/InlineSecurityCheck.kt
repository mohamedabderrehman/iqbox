package com.iqbox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.iqbox.app.ui.theme.*
import kotlin.random.Random

/**
 * Inline Security Check - تحقق أمني مدمج في سطر واحد
 * أبسط وأكثر احترافية من الـ Card المنفصل
 */
@Composable
fun InlineSecurityCheck(
    state: SecurityCheckState,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Label
        Text(
            text = "Security Check",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkMode) DarkTextMuted else TextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        // Question and Answer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isDarkMode) DarkCard else BackgroundGray,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = when {
                        state.showError -> ErrorColor
                        state.isCorrect && state.answer.isNotBlank() -> SuccessColor
                        else -> if (isDarkMode) DarkBorder else BorderLight
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Question
            Text(
                text = "${state.question.first} + ${state.question.second} = ",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
            
            // Answer Input
            OutlinedTextField(
                value = state.answer,
                onValueChange = { state.updateAnswer(it) },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    unfocusedTextColor = if (isDarkMode) DarkTextPrimary else TextPrimary,
                    cursorColor = AccentBlue,
                    focusedContainerColor = if (isDarkMode) DarkSurface else BackgroundWhite,
                    unfocusedContainerColor = if (isDarkMode) DarkSurface else BackgroundWhite,
                    errorBorderColor = ErrorColor
                ),
                isError = state.showError,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        // Error/Success Message
        if (state.showError) {
            Text(
                text = "Incorrect answer",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorColor,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        } else if (state.isCorrect && state.answer.isNotBlank()) {
            Text(
                text = "Verified",
                style = MaterialTheme.typography.bodySmall,
                color = SuccessColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }
    }
}

/**
 * State for Inline Security Check
 */
class SecurityCheckState {
    private var _question by mutableStateOf(generateQuestion())
    
    val question: Pair<Int, Int>
        get() = _question
    
    var answer by mutableStateOf("")
        private set
    
    var showError by mutableStateOf(false)
        private set
    
    val expectedAnswer: Int
        get() = _question.first + _question.second
    
    val isCorrect: Boolean
        get() = answer.toIntOrNull() == expectedAnswer
    
    fun updateAnswer(newAnswer: String) {
        answer = newAnswer.filter { it.isDigit() }.take(2)
        showError = false
    }
    
    fun validate(): Boolean {
        val isValid = isCorrect
        showError = !isValid
        return isValid
    }
    
    fun reset() {
        _question = generateQuestion()
        answer = ""
        showError = false
    }
    
    private fun generateQuestion(): Pair<Int, Int> {
        val num1 = Random.nextInt(1, 10)
        val num2 = Random.nextInt(1, 10)
        return Pair(num1, num2)
    }
}

@Composable
fun rememberSecurityCheckState(): SecurityCheckState {
    return remember { SecurityCheckState() }
}
