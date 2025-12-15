package com.gavr123456789.github.sovazeleboba
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
@Preview
fun App() {
    val orangeDarkColorScheme = darkColorScheme(
        primary = Color(0xFFFF9800),          // main orange
        onPrimary = Color(0xFF000000),        // black text/icons on orange
        primaryContainer = Color(0xFFFFB74D), // lighter orange for containers
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFFFB74D),        // additional orange
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF333333),
        onSecondaryContainer = Color(0xFFFFE0B2),
        background = Color(0xFF000000),       // nearly fully black background
        onBackground = Color(0xFFFFE0B2),     // soft light text on black
        surface = Color(0xFF121212),          // slightly lighter for surfaces
        onSurface = Color(0xFFFFE0B2),
        error = Color(0xFFFF5252),
        onError = Color(0xFF000000),
    )

    MaterialTheme(
        colorScheme = orangeDarkColorScheme
    ) {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            WordMatchApp()
        }
    }
}

private data class WordPair(
    val original: String,
    val translation: String,
)

private enum class ScreenState {
    Start,
    Game,
    Selector,
    History,
}

@Composable
private fun WordMatchApp() {
    var screenState by remember { mutableStateOf(ScreenState.Start) }
    var wordPairs by remember { mutableStateOf<List<WordPair>>(emptyList()) }
    var lastFile by remember { mutableStateOf<File?>(null) }
    var invert by remember { mutableStateOf(false) }
    var gameId by remember { mutableStateOf(0) }
    var pageSize by remember { mutableStateOf(5) }

    when (screenState) {
        ScreenState.Start -> StartScreen(
            invert = invert,
            onInvertChanged = { invert = it },
            pageSize = pageSize,
            onPageSizeChanged = { pageSize = it },
            onFileParsed = { file, pairs ->
                lastFile = file
                wordPairs = if (invert) {
                    pairs.map { WordPair(original = it.translation, translation = it.original) }
                } else {
                    pairs
                }
                if (pairs.isNotEmpty()) {
                    screenState = ScreenState.Game
                }
            },
            onFileParsedSelector = { file, pairs ->
                lastFile = file
                wordPairs = if (invert) {
                    pairs.map { WordPair(original = it.translation, translation = it.original) }
                } else {
                    pairs
                }
                if (pairs.isNotEmpty()) {
                    screenState = ScreenState.Selector
                }
            },
            onOpenHistory = {
                screenState = ScreenState.History
            },
        )

        ScreenState.Game -> GameScreen(
            pairs = wordPairs,
            pageSize = pageSize,
            gameId = gameId,
            gameName = lastFile?.name,
            onRetry = {
                val file = lastFile
                if (file == null) {
                    lastFile = null
                    wordPairs = emptyList()
                    screenState = ScreenState.Start
                    return@GameScreen
                }

                val pairs = parseWordPairs(file)
                if (pairs.isEmpty()) {
                    lastFile = null
                    wordPairs = emptyList()
                    screenState = ScreenState.Start
                    return@GameScreen
                }

                wordPairs = if (invert) {
                    pairs.map { WordPair(original = it.translation, translation = it.original) }
                } else {
                    pairs
                }
                gameId += 1
            },
            onBackToMenu = {
                screenState = ScreenState.Start
                wordPairs = emptyList()
            },
        )

        ScreenState.History -> HistoryScreen(
            onBackToMenu = {
                screenState = ScreenState.Start
            },
        )

        ScreenState.Selector -> SelectorScreen(
            pairs = wordPairs,
            pageSize = pageSize,
            gameId = gameId,
            gameName = lastFile?.name,
            onRetry = {
                val file = lastFile
                if (file == null) {
                    lastFile = null
                    wordPairs = emptyList()
                    screenState = ScreenState.Start
                    return@SelectorScreen
                }

                val pairs = parseWordPairs(file)
                if (pairs.isEmpty()) {
                    lastFile = null
                    wordPairs = emptyList()
                    screenState = ScreenState.Start
                    return@SelectorScreen
                }

                wordPairs = if (invert) {
                    pairs.map { WordPair(original = it.translation, translation = it.original) }
                } else {
                    pairs
                }
                gameId += 1
            },
            onBackToMenu = {
                screenState = ScreenState.Start
                wordPairs = emptyList()
            },
        )
    }
}

@Composable
private fun StartScreen(
    invert: Boolean,
    onInvertChanged: (Boolean) -> Unit,
    pageSize: Int,
    onPageSizeChanged: (Int) -> Unit,
    onFileParsed: (File, List<WordPair>) -> Unit,
    onFileParsedSelector: (File, List<WordPair>) -> Unit,
    onOpenHistory: () -> Unit,
) {
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Word Matching Game", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Choose a file with word pairs in the format:\n" +
                    "\"original - translation\" or \"original translation\"",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = invert,
                onCheckedChange = { onInvertChanged(it) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Invert (translation - word)", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Items per page:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onPageSizeChanged((pageSize - 1).coerceAtLeast(1)) },
                ) {
                    Text(text = "<")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    enabled = false,
                ) {
                    Text(text = pageSize.toString(), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { onPageSizeChanged((pageSize + 1).coerceAtMost(10)) },
                ) {
                    Text(text = ">")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            val file = chooseFile()
            if (file != null) {
                selectedFilePath = file.absolutePath
                val pairs = parseWordPairs(file)
                if (pairs.isEmpty()) {
                    errorMessage = "Could not find any word pairs in the file"
                } else {
                    errorMessage = null
                    onFileParsed(file, pairs)
                }
            } else {
                selectedFilePath = null
            }
        }) {
            Text("Choose file and start")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            val file = chooseFile()
            if (file != null) {
                selectedFilePath = file.absolutePath
                val pairs = parseWordPairs(file)
                if (pairs.isEmpty()) {
                    errorMessage = "Could not find any word pairs in the file"
                } else {
                    errorMessage = null
                    onFileParsedSelector(file, pairs)
                }
            } else {
                selectedFilePath = null
            }
        }) {
            Text("Start Selector mode")
        }

        selectedFilePath?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "File: $it", style = MaterialTheme.typography.bodySmall)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenHistory) {
            Text(text = "Show history")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameScreen(
    pairs: List<WordPair>,
    pageSize: Int,
    gameId: Int,
    gameName: String?,
    onRetry: () -> Unit,
    onBackToMenu: () -> Unit,
) {
    var currentPage by remember(gameId) { mutableStateOf(0) }

    val totalPages = (pairs.size + pageSize - 1) / pageSize
    val pagePairs = pairs.drop(currentPage * pageSize).take(pageSize)

    // Game-wide statistics
    var totalAttempts by remember(gameId) { mutableStateOf(0) }
    var totalErrors by remember(gameId) { mutableStateOf(0) }
    var isCompleted by remember(gameId) { mutableStateOf(false) }

    // Timer: start at the beginning of the game and fix the result on the final screen
    val startTimeMillis by remember(gameId) { mutableStateOf(System.currentTimeMillis()) }
    var totalTimeMillis by remember(gameId) { mutableStateOf<Long?>(null) }

    var leftSelection by remember(gameId, currentPage) { mutableStateOf<Int?>(null) }
    var rightSelection by remember(gameId, currentPage) { mutableStateOf<Int?>(null) }
    var matchedLeftIndices by remember(gameId, currentPage) { mutableStateOf(setOf<Int>()) }
    var matchedRightIndices by remember(gameId, currentPage) { mutableStateOf(setOf<Int>()) }

    // Separate visual order for the left and right columns so we can animate movement
    var leftOrder by remember(gameId, currentPage) {
        mutableStateOf(pagePairs.indices.toList())
    }
    // Separate order for the right column to shuffle translations
    var rightOrder by remember(gameId, currentPage) {
        mutableStateOf(pagePairs.indices.shuffled())
    }

    fun handleMatch(leftPairIndex: Int, rightPairIndex: Int) {
        val leftPair = pagePairs.getOrNull(leftPairIndex)
        val rightPair = pagePairs.getOrNull(rightPairIndex)

        totalAttempts += 1

        if (
            leftPair != null &&
            rightPair != null &&
            !matchedLeftIndices.contains(leftPairIndex) &&
            !matchedRightIndices.contains(rightPairIndex) &&
            leftPair.translation == rightPair.translation
        ) {
            val newMatchedLeft = matchedLeftIndices + leftPairIndex
            val newMatchedRight = matchedRightIndices + rightPairIndex
            matchedLeftIndices = newMatchedLeft
            matchedRightIndices = newMatchedRight
            leftSelection = null
            rightSelection = null

            // Перемещаем правильно найденные элементы в начало списков с анимацией
            leftOrder = listOf(leftPairIndex) + leftOrder.filter { it != leftPairIndex }
            rightOrder = listOf(rightPairIndex) + rightOrder.filter { it != rightPairIndex }

            // The page is finished when all pairs on it are matched
            if (newMatchedLeft.size == pagePairs.size) {
                if (currentPage + 1 < totalPages) {
                    currentPage += 1
                } else {
                    totalTimeMillis = System.currentTimeMillis() - startTimeMillis

                    val successCount = pairs.size
                    val errorCount = totalErrors
                    val attempts = successCount + errorCount
                    val successPercent = if (attempts > 0) (successCount * 100) / attempts else 0

                    appendResultToHistory(
                        name = gameName,
                        successCount = successCount,
                        errorCount = errorCount,
                        successPercent = successPercent,
                        totalTimeMillis = totalTimeMillis,
                    )
                    isCompleted = true
                }
            }
        } else {
            totalErrors += 1
            leftSelection = null
            rightSelection = null
        }
    }

    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Кнопка "назад в меню" в верхнем левом углу
        Button(
            onClick = onBackToMenu,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text("← Меню")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                // Keyboard is not used on the results screen
                if (isCompleted) return@onPreviewKeyEvent false

                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false

                val index = when (event.key) {
                    Key.One -> 0
                    Key.Two -> 1
                    Key.Three -> 2
                    Key.Four -> 3
                    Key.Five -> 4
                    Key.Six -> 5
                    Key.Seven -> 6
                    Key.Eight -> 7
                    Key.Nine -> 8
                    else -> return@onPreviewKeyEvent false
                }

                if (index < 0 || index >= pagePairs.size) return@onPreviewKeyEvent false

                if (leftSelection == null) {
                    // Первая цифра выбирает элемент в левой колонке по визуальной позиции
                    val pairIndex = leftOrder.getOrNull(index) ?: return@onPreviewKeyEvent false
                    if (!matchedLeftIndices.contains(pairIndex)) {
                        leftSelection = if (leftSelection == pairIndex) null else pairIndex
                    }
                } else {
                    // Вторая цифра выбирает элемент в правой колонке по визуальной позиции
                    val rightPairIndex = rightOrder.getOrNull(index) ?: return@onPreviewKeyEvent false
                    if (!matchedRightIndices.contains(rightPairIndex)) {
                        rightSelection = rightPairIndex
                        handleMatch(leftSelection!!, rightPairIndex)
                    }
                }

                true
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        if (isCompleted) {
            // Final statistics screen
            val successCount = pairs.size
            val errorCount = totalErrors
            val attempts = successCount + errorCount
            val successPercent = if (attempts > 0) (successCount * 100) / attempts else 0

            val timeText = totalTimeMillis?.let { millis ->
                val totalSeconds = millis / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                "Time: %d:%02d".format(minutes, seconds)
            }

            Text(
                text = "Result",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Correct/Errors: ${successCount}/${errorCount}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Success rate: ${successPercent}%",
                style = MaterialTheme.typography.bodyLarge,
            )
            timeText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onRetry() }) {
                Text("Retry same file")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { onBackToMenu() }) {
                Text("Back to main menu")
            }
        } else {
            Text(
                text = "Page ${currentPage + 1} of $totalPages",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text("Original", style = MaterialTheme.typography.titleSmall)
                    }
                    items(leftOrder, key = { it }) { pairIndex ->
                        val pair = pagePairs[pairIndex]
                        val isMatched = matchedLeftIndices.contains(pairIndex)
                        val isSelected = leftSelection == pairIndex
                        WordButton(
                            text = pair.original,
                            enabled = !isMatched,
                            selected = isSelected,
                            correct = isMatched,
                            onClick = {
                                if (!isMatched) {
                                    leftSelection = if (leftSelection == pairIndex) null else pairIndex
                                    if (rightSelection != null) {
                                        handleMatch(pairIndex, rightSelection!!)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text("Translation", style = MaterialTheme.typography.titleSmall)
                    }
                    items(rightOrder, key = { it }) { pairIndex ->
                        val pair = pagePairs[pairIndex]
                        val isMatched = matchedRightIndices.contains(pairIndex)
                        val isSelected = rightSelection == pairIndex
                        WordButton(
                            text = pair.translation,
                            enabled = !isMatched,
                            selected = isSelected,
                            correct = isMatched,
                            onClick = {
                                if (!isMatched) {
                                    rightSelection = if (rightSelection == pairIndex) null else pairIndex
                                    if (leftSelection != null) {
                                        handleMatch(leftSelection!!, pairIndex)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SelectorScreen(
    pairs: List<WordPair>,
    pageSize: Int,
    gameId: Int,
    gameName: String?,
    onRetry: () -> Unit,
    onBackToMenu: () -> Unit,
) {
    var currentPage by remember(gameId) { mutableStateOf(0) }
    val totalPages = (pairs.size + pageSize - 1) / pageSize
    val pagePairs = pairs.drop(currentPage * pageSize).take(pageSize)

    // Статистика
    var totalCorrect by remember(gameId) { mutableStateOf(0) }
    var totalErrors by remember(gameId) { mutableStateOf(0) }
    var isCompleted by remember(gameId) { mutableStateOf(false) }

    // Таймер
    val startTimeMillis by remember(gameId) { mutableStateOf(System.currentTimeMillis()) }
    var totalTimeMillis by remember(gameId) { mutableStateOf<Long?>(null) }

    // Текущее слово в пределах страницы
    var questionIndex by remember(gameId, currentPage) { mutableStateOf(0) }

    val currentPair = pagePairs.getOrNull(questionIndex)

    // Варианты ответов для текущего слова
    val options: List<String> = remember(gameId, currentPage, questionIndex) {
        val allTranslations = pairs.map { it.translation }.distinct()
        val correct = currentPair?.translation
        if (correct == null) return@remember emptyList()
        val incorrect = allTranslations.filter { it != correct }.shuffled().take(4)
        (listOf(correct) + incorrect).shuffled()
    }

    var disabledOptionIdx by remember(gameId, currentPage, questionIndex) { mutableStateOf(setOf<Int>()) }

    fun goNext() {
        if (questionIndex + 1 < pagePairs.size) {
            questionIndex += 1
        } else {
            if (currentPage + 1 < totalPages) {
                currentPage += 1
            } else {
                totalTimeMillis = System.currentTimeMillis() - startTimeMillis
                val successCount = pairs.size
                val errorCount = totalErrors
                val attempts = successCount + errorCount
                val successPercent = if (attempts > 0) (successCount * 100) / attempts else 0

                appendResultToHistory(
                    name = gameName,
                    successCount = successCount,
                    errorCount = errorCount,
                    successPercent = successPercent,
                    totalTimeMillis = totalTimeMillis,
                )
                isCompleted = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Кнопка назад в меню
        Button(
            onClick = onBackToMenu,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text("← Меню")
        }

        if (isCompleted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Selector finished",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                val attempts = totalCorrect + totalErrors
                val successPercent = if (attempts > 0) (totalCorrect * 100) / attempts else 0
                Text("Correct: $totalCorrect, Errors: $totalErrors, Success: $successPercent%")
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRetry) { Text("Retry") }
                    Button(onClick = onBackToMenu) { Text("Back to menu") }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Selector Mode", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Заголовок с прогрессом
                val globalIndex = currentPage * pageSize + questionIndex + 1
                Text(
                    text = "Word $globalIndex of ${pairs.size}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))
                // Слово-оригинал
                Text(
                    text = currentPair?.original ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки с вариантами
                options.forEachIndexed { idx, option ->
                    val isDisabled = disabledOptionIdx.contains(idx)
                    Button(
                        onClick = {
                            if (currentPair == null) return@Button
                            if (option == currentPair.translation) {
                                totalCorrect += 1
                                // Переходим к следующему слову
                                goNext()
                            } else {
                                totalErrors += 1
                                disabledOptionIdx = disabledOptionIdx + idx
                            }
                        },
                        enabled = !isDisabled,
                        colors = ButtonDefaults.buttonColors(
                            // Контрасты для тёмной темы при disabled
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(option)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordButton(
    text: String,
    enabled: Boolean,
    selected: Boolean,
    correct: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        correct -> Color(0xFF4CAF50)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    // Explicit text color so it stays readable even on disabled buttons
    val contentColor = when {
        correct -> Color.Black
        selected -> Color.Black
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = contentColor,
        ),
        modifier = modifier,
    ) {
        Text(text = text, color = contentColor)
    }
}

private fun chooseFile(): File? {
    val dialog = FileDialog(null as Frame?, "Выберите файл", FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    if (directory != null && file != null) {
        return File(directory, file)
    }
    return null
}

private fun appendResultToHistory(
    name: String?,
    successCount: Int,
    errorCount: Int,
    successPercent: Int,
    totalTimeMillis: Long?,
) {
    try {
        val homeDir = System.getProperty("user.home") ?: return
        val dir = File(homeDir, ".config/Sova")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, "results.txt")

        val timeText = totalTimeMillis?.let { millis ->
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "%d:%02d".format(minutes, seconds)
        } ?: "-"

        val safeName = name?.ifBlank { null } ?: "-"

        val line =
            "name=$safeName;correct=$successCount;errors=$errorCount;successRate=${successPercent}%;time=$timeText" + "\n"

        file.appendText(line)
    } catch (_: Exception) {
        // Игнорируем ошибки записи, чтобы не падать из-за проблем с файловой системой
    }
}

private fun parseWordPairs(file: File): List<WordPair> {
    return file.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null

            val original: String
            val translation: String

            if ("-" in trimmed) {
                val parts = trimmed.split("-", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                original = parts[0].trim()
                translation = parts[1].trim()
            } else {
                val parts = trimmed.split(" ", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                original = parts[0].trim()
                translation = parts[1].trim()
            }

            if (original.isNotEmpty() && translation.isNotEmpty()) {
                WordPair(original, translation)
            } else {
                null
            }
        }
}