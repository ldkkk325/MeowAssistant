package com.meow.assistant.assistant

import kotlin.random.Random

data class ProcessResult(
    val text: String,
    val userEndIndex: Int,
    val processedBoundaryToOriginal: IntArray = IntArray(text.length + 1),
    val emoticon: String? = null,
    val actionText: String? = null,
)

object TextProcessor {
    data class RecoveredEdit(val text: String, val cursorIndex: Int)

    private val sentenceSeparators = Regex("([，,。！!？?\\n]+)")
    private val builtInEmoticons = arrayOf("^⌯𖥦⌯^ ੭ ^", "⌯'ㅅ'⌯", "=^𖥦^=", "⌯•ㅅ•⌯", "ฅ•̀∀•́ฅ", "ฅ ̳͒•ˑ̫• ̳͒ฅ♡", "ฅ(̳•·̫•̳ฅ)♡", "ฅ^••^ฅ", "=^•ω•^=", "₍^ >ヮ<^₎", "/ᐠ - ˕ -マ Ⳋ", "ฅ^•ﻌ•^ฅ", "ฅ՞•ﻌ•՞ฅ", "(ฅ´ω`ฅ)", "ฅ(*`ω´*)ฅ", "ฅ꒰ ⸝˶• •˶⸝꒱ฅ", "₍˄·͈༝·͈˄*₎◞ ̑̑", "₍^⸝⸝> ·̫ <⸝⸝ ^₎", "ฅ^._.^ฅ", "₍🎀˄•͈༝•͈˄₎ฅ˒˒", "^•͈༝•^ฅ", "ฅ●ω●ฅ", "(>^ω^<)", "ฅ(≧▽≦)ฅ", "ฅ(=´▽`=)ฅ", "(ฅ◑ω◑ฅ)", "(=^.^=)", "(=^-ω-^=)", "(^•ᴥ•^)", "( Φ ω Φ )")
    private val builtInTexts = arrayOf("蹭蹭", "摇尾巴", "竖耳朵", "打滚", "扑过来", "卷尾巴", "喵呜", "嗷呜", "呜呜", "嘿嘿", "呜哇", "亲亲", "抱住", "咕噜噜", "开心", "好耶", "害羞", "委屈巴巴", "吃饱饱", "困困", "贴贴", "等你哦", "冲呀", "晕乎乎")
    private val smartQuestion = arrayOf("ฅ՞•ﻌ•՞ฅ", "꒰ఎ(^ . ֑ .^)໒꒱", "=^x^=", "(｡◔‸◔｡)？", "(｡•ㅅ•｡)？", "(・ε・)ﾝ??", "(◎_◎) ﾝ?", "(￢￢)ﾎﾝﾄ???")
    private val smartExclamation = arrayOf("ฅ•̀∀•́ฅ", "ฅ(*`ω´*)ฅ", "₍^⸝⸝> ·̫ <⸝⸝ ^₎", "ฅ●ω●ฅ", "(>^ω^<)", "ฅ(≧▽≦)ฅ", "Σ(ﾟωﾟ)", "(((ﾟдﾟ)))")
    private val smartPeriod = arrayOf("^⌯𖥦⌯^ ੭ ^", "⌯'ㅅ'⌯", "=^𖥦^=", "⌯•ㅅ•⌯", "ฅ^••^ฅ", "=^•ω•^=", "₍^ >ヮ<^₎", "(ฅ´ω`ฅ)", "(｡･ω･｡)", "(≧◡≦)")

    fun process(
        text: String?,
        config: AssistantConfig,
        previousEmoticon: String? = null,
        previousActionText: String? = null,
        random: Random = Random.Default,
    ): ProcessResult {
        val original = text.orEmpty()
        if (original.isBlank() || !config.enabled) return unchanged(original)
        var mapped = MappedText(original, IntArray(original.length + 1) { it })
        mapped = applyRules(mapped, config.rules)
        val body = if (config.enableAppend) appendPerSentence(mapped, config.appendText, config.appendProbability, random) else ProcessedBody(mapped, mapped.text.length)
        val result = StringBuilder(body.value.text)
        val boundaries = body.value.boundaries.toMutableList()
        var selectedEmoticon: String? = null
        if (config.enableEmoticon && (previousEmoticon != null || chance(config.emoticonProbability, random))) {
            val pool = if (config.enableSmartEmoticon && config.customEmoticons.isEmpty()) smartPool(original)?.toList().orEmpty() else config.customEmoticons.ifEmpty { builtInEmoticons.asList() }
            selectedEmoticon = previousEmoticon?.takeIf { it in pool } ?: pool.randomOrNull(random)
            appendGenerated(result, boundaries, selectedEmoticon?.let { " $it" }.orEmpty(), original.length)
        }
        var selectedActionText: String? = null
        if (config.enableRandomText && (previousActionText != null || chance(config.randomTextProbability, random))) {
            val textPool = emotionTextPool(original) ?: config.customTexts.ifEmpty { builtInTexts.asList() }
            selectedActionText = previousActionText?.takeIf { it in textPool } ?: textPool.randomOrNull(random)
            appendGenerated(result, boundaries, selectedActionText?.let { " ($it)" }.orEmpty(), original.length)
        }
        return ProcessResult(
            result.toString(),
            body.cursorIndex.coerceIn(0, result.length),
            boundaries.toIntArray(),
            selectedEmoticon,
            selectedActionText,
        )
    }

    fun recoverEditedOriginal(previousOriginal: String, previousResult: ProcessResult, editedText: String): String {
        return recoverEdited(previousOriginal, previousResult, editedText, editedText.length).text
    }

    fun recoverEdited(
        previousOriginal: String,
        previousResult: ProcessResult,
        editedText: String,
        editedCursorIndex: Int? = null,
    ): RecoveredEdit {
        val previousText = previousResult.text
        if (previousText.isEmpty()) return RecoveredEdit(editedText, (editedCursorIndex ?: editedText.length).coerceIn(0, editedText.length))
        var prefix = 0
        val commonLimit = minOf(previousText.length, editedText.length)
        while (prefix < commonLimit && previousText[prefix] == editedText[prefix]) prefix++
        var suffix = 0
        while (suffix < previousText.length - prefix && suffix < editedText.length - prefix && previousText[previousText.lastIndex - suffix] == editedText[editedText.lastIndex - suffix]) suffix++
        val removedEnd = previousText.length - suffix
        val insertedEnd = editedText.length - suffix
        val mapping = previousResult.processedBoundaryToOriginal
        val originalStart = mapping.getOrElse(prefix) { previousOriginal.length }.coerceIn(0, previousOriginal.length)
        val originalEnd = mapping.getOrElse(removedEnd) { previousOriginal.length }.coerceIn(originalStart, previousOriginal.length)
        val insertedText = editedText.substring(prefix, insertedEnd)
        val recoveredText = previousOriginal.replaceRange(originalStart, originalEnd, insertedText)
        val cursor = (editedCursorIndex ?: insertedEnd).coerceIn(0, editedText.length)
        val recoveredCursor = when {
            cursor <= prefix -> mapping.getOrElse(cursor) { originalStart }
                .coerceIn(0, recoveredText.length)
            cursor < insertedEnd -> (originalStart + cursor - prefix)
                .coerceIn(originalStart, originalStart + insertedText.length)
            else -> {
                val previousCursor = (removedEnd + (cursor - insertedEnd))
                    .coerceIn(removedEnd, previousText.length)
                val mappedCursor = mapping.getOrElse(previousCursor) { previousOriginal.length }
                val removedOriginalLength = (originalEnd - originalStart).coerceAtLeast(0)
                (mappedCursor - removedOriginalLength + insertedText.length)
                    .coerceIn(0, recoveredText.length)
            }
        }
        return RecoveredEdit(recoveredText, recoveredCursor)
    }

    fun processedCursorIndex(result: ProcessResult, originalCursorIndex: Int): Int {
        val cursor = originalCursorIndex.coerceIn(0, result.processedBoundaryToOriginal.lastOrNull() ?: 0)
        return result.processedBoundaryToOriginal.indexOfFirst { it >= cursor }
            .takeIf { it >= 0 }
            ?.coerceIn(0, result.text.length)
            ?: result.userEndIndex
    }

    fun applyIncremental(
        previousOriginal: String,
        currentOriginal: String,
        config: AssistantConfig,
        previousEmoticon: String? = null,
        previousActionText: String? = null,
        random: Random = Random.Default,
    ): ProcessResult = process(currentOriginal, config, previousEmoticon, previousActionText, random)

    private fun unchanged(text: String) = ProcessResult(text, text.length, IntArray(text.length + 1) { it })

    private fun applyRules(source: MappedText, rules: List<ReplacementRule>): MappedText {
        val matchers = rules
            .mapIndexed { index, rule -> IndexedRule(index, rule) }
            .filter { it.rule.from.isNotEmpty() }
            .sortedWith(compareByDescending<IndexedRule> { it.rule.from.length }.thenBy { it.index })
        if (matchers.isEmpty()) return source
        val output = StringBuilder()
        val boundaries = mutableListOf(source.boundaries.first())
        var sourceIndex = 0
        while (sourceIndex < source.text.length) {
            val matched = matchers.firstOrNull { source.text.startsWith(it.rule.from, sourceIndex) }
            if (matched == null) {
                output.append(source.text[sourceIndex])
                boundaries.add(source.boundaries[sourceIndex + 1])
                sourceIndex++
            } else {
                appendReplacement(source, sourceIndex, matched.rule, output, boundaries)
                sourceIndex += matched.rule.from.length
            }
        }
        return MappedText(output.toString(), boundaries.toIntArray())
    }

    private fun appendReplacement(
        source: MappedText,
        sourceIndex: Int,
        rule: ReplacementRule,
        output: StringBuilder,
        boundaries: MutableList<Int>,
    ) {
        val matchEnd = sourceIndex + rule.from.length
        rule.to.forEachIndexed { index, character ->
            output.append(character)
            boundaries.add(if (index == rule.to.lastIndex) source.boundaries[matchEnd] else source.boundaries[sourceIndex])
        }
    }

    private fun appendPerSentence(source: MappedText, suffix: String, probability: Int, random: Random): ProcessedBody {
        if (suffix.isEmpty()) return ProcessedBody(source, source.text.length)
        val output = StringBuilder()
        val boundaries = mutableListOf(source.boundaries.first())
        var cursorIndex = 0
        var sourceIndex = 0
        sentenceSeparators.findAll(source.text).forEach { match ->
            cursorIndex = appendSentenceChunk(source, sourceIndex, match.range.first, output, boundaries, suffix, probability, random, cursorIndex)
            appendSourceRange(source, match.range.first, match.range.last + 1, output, boundaries)
            cursorIndex = output.length
            sourceIndex = match.range.last + 1
        }
        cursorIndex = appendSentenceChunk(source, sourceIndex, source.text.length, output, boundaries, suffix, probability, random, cursorIndex)
        return ProcessedBody(MappedText(output.toString(), boundaries.toIntArray()), cursorIndex)
    }

    private fun appendSentenceChunk(source: MappedText, start: Int, end: Int, output: StringBuilder, boundaries: MutableList<Int>, suffix: String, probability: Int, random: Random, previousCursor: Int): Int {
        if (start >= end) return previousCursor
        appendSourceRange(source, start, end, output, boundaries)
        val cursor = output.length
        if (source.text.substring(start, end).isNotBlank() && chance(probability, random)) appendGenerated(output, boundaries, suffix, source.boundaries[end])
        return cursor
    }

    private fun appendSourceRange(source: MappedText, start: Int, end: Int, output: StringBuilder, boundaries: MutableList<Int>) {
        for (index in start until end) { output.append(source.text[index]); boundaries.add(source.boundaries[index + 1]) }
    }

    private fun appendGenerated(output: StringBuilder, boundaries: MutableList<Int>, value: String, originalBoundary: Int) {
        value.forEach { output.append(it); boundaries.add(originalBoundary) }
    }

    private fun chance(probability: Int, random: Random): Boolean {
        val normalized = probability.coerceIn(0, 100)
        return normalized == 100 || normalized > 0 && random.nextInt(100) < normalized
    }

    private fun smartPool(text: String): Array<String>? = when (text.lastOrNull()) {
        '?', '？' -> smartQuestion
        '!', '！' -> smartExclamation
        '。', '.', '，', ',' -> smartPeriod
        else -> null
    }

    private fun emotionTextPool(text: String): List<String>? = when {
        listOf("哈哈", "笑死", "好笑", "太逗").any(text::contains) -> listOf("哈哈哈", "笑死本喵", "戳中笑点", "笑到打滚")
        listOf("难过", "难受", "哭", "委屈", "伤心", "呜呜").any(text::contains) -> listOf("呜呜", "抱抱", "摸摸头", "好委屈")
        listOf("晚安", "睡觉", "睡了", "累", "困", "休息").any(text::contains) -> listOf("困困", "晚安喵", "睡香香", "做个好梦")
        listOf("谢谢", "谢了", "多谢", "感谢", "辛苦").any(text::contains) -> listOf("不客气", "谢谢大人", "啾咪", "嘿嘿")
        listOf("生气", "气死", "可恶", "烦", "滚", "怒").any(text::contains) -> listOf("哼", "气鼓鼓", "本喵怒了", "不理你")
        listOf("爱你", "喜欢你", "想你", "亲亲", "宝贝", "抱抱").any(text::contains) -> listOf("贴贴", "唔喵", "亲亲", "好喜欢你")
        listOf("早安", "早上好", "起床").any(text::contains) -> listOf("早呀", "早安喵", "新的一天")
        listOf("拜拜", "再见", "走了", "告辞", "886").any(text::contains) -> listOf("拜拜", "下次见", "走啦", "挥手手")
        else -> null
    }

    private data class MappedText(val text: String, val boundaries: IntArray)
    private data class ProcessedBody(val value: MappedText, val cursorIndex: Int)
    private data class IndexedRule(val index: Int, val rule: ReplacementRule)
}
