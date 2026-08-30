package com.meow.assistant.assistant

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class ProcessingMode(val value: String) {
    PUNCTUATION("punctuation"),
    REALTIME("realtime"),
    FLOATING("floating");

    companion object {
        fun fromValue(value: String?): ProcessingMode = entries.firstOrNull { it.value == value } ?: PUNCTUATION
    }
}

data class ReplacementRule(val from: String, val to: String) {
    override fun toString(): String = "$from=$to"
}

data class AssistantConfig(
    val enabled: Boolean = false,
    val processingMode: ProcessingMode = ProcessingMode.PUNCTUATION,
    val enableAppend: Boolean = true,
    val appendText: String = "喵~",
    val appendProbability: Int = 100,
    val enableEmoticon: Boolean = true,
    val emoticonProbability: Int = 100,
    val enableSmartEmoticon: Boolean = false,
    val enableRandomText: Boolean = true,
    val randomTextProbability: Int = 100,
    val protectInputMethods: Boolean = true,
    val protectPasswords: Boolean = true,
    val floatBallSize: Int = DEFAULT_FLOAT_BALL_SIZE,
    val floatBallAlpha: Float = DEFAULT_FLOAT_BALL_ALPHA,
    val floatBallX: Int = DEFAULT_FLOAT_BALL_X,
    val floatBallY: Int = DEFAULT_FLOAT_BALL_Y,
    val selectedPackages: Set<String> = emptySet(),
    val rules: List<ReplacementRule> = defaultRules,
    val customEmoticons: List<String> = emptyList(),
    val customTexts: List<String> = emptyList(),
) {
    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, enabled)
            putString(KEY_MODE, processingMode.value)
            putBoolean(KEY_APPEND_ENABLED, enableAppend)
            putString(KEY_APPEND_TEXT, appendText)
            putInt(KEY_APPEND_PROBABILITY, appendProbability.coerceIn(0, 100))
            putBoolean(KEY_EMOTICON_ENABLED, enableEmoticon)
            putInt(KEY_EMOTICON_PROBABILITY, emoticonProbability.coerceIn(0, 100))
            putBoolean(KEY_SMART_EMOTICON, enableSmartEmoticon)
            putBoolean(KEY_RANDOM_TEXT, enableRandomText)
            putInt(KEY_RANDOM_TEXT_PROBABILITY, randomTextProbability.coerceIn(0, 100))
            putBoolean(KEY_PROTECT_INPUT_METHODS, protectInputMethods)
            putBoolean(KEY_PROTECT_PASSWORDS, protectPasswords)
            remove(KEY_FLOAT_BALL)
            putInt(KEY_FLOAT_BALL_SIZE, floatBallSize.coerceIn(MIN_FLOAT_BALL_SIZE, MAX_FLOAT_BALL_SIZE))
            putFloat(KEY_FLOAT_BALL_ALPHA, floatBallAlpha.coerceIn(MIN_FLOAT_BALL_ALPHA, MAX_FLOAT_BALL_ALPHA))
            putInt(KEY_FLOAT_BALL_X, floatBallX)
            putInt(KEY_FLOAT_BALL_Y, floatBallY)
            putStringSet(KEY_SELECTED_PACKAGES, selectedPackages.toSet())
            putString(KEY_RULES, rulesToText(rules))
            putString(KEY_CUSTOM_EMOTICONS, customEmoticons.joinToString("\n"))
            putString(KEY_CUSTOM_TEXTS, customTexts.joinToString("\n"))
            putInt(CONFIG_VERSION_KEY, CONFIG_VERSION)
        }
    }

    companion object {
        const val PREFS_NAME = "cat_config"
        const val KEY_ENABLED = "enable_master"
        const val KEY_MODE = "processing_mode"
        const val KEY_APPEND_ENABLED = "enable_append"
        const val KEY_APPEND_TEXT = "append_text"
        const val KEY_APPEND_PROBABILITY = "append_probability"
        const val KEY_EMOTICON_ENABLED = "enable_emoticon"
        const val KEY_EMOTICON_PROBABILITY = "emoticon_probability"
        const val KEY_SMART_EMOTICON = "enable_smart_emoticon"
        const val KEY_RANDOM_TEXT = "enable_random_text"
        const val KEY_RANDOM_TEXT_PROBABILITY = "random_text_probability"
        const val KEY_PROTECT_INPUT_METHODS = "protect_input_methods"
        const val KEY_PROTECT_PASSWORDS = "protect_passwords"
        private const val KEY_FLOAT_BALL = "float_ball_enabled"
        const val KEY_FLOAT_BALL_SIZE = "float_ball_size"
        const val KEY_FLOAT_BALL_ALPHA = "float_ball_alpha"
        const val KEY_FLOAT_BALL_X = "float_ball_x"
        const val KEY_FLOAT_BALL_Y = "float_ball_y"
        const val KEY_SELECTED_PACKAGES = "selected_packages"
        const val KEY_RULES = "rules"
        const val KEY_CUSTOM_EMOTICONS = "custom_emoticons"
        const val KEY_CUSTOM_TEXTS = "custom_texts"

        const val DEFAULT_RULES_TEXT = "我=本喵"
        private const val PREVIOUS_DEFAULT_RULES_TEXT = "我=本喵\n你=大人\n哥哥=大人\n乐乐=杂鱼🐟♡\n乐子=杂鱼🐟♡\n傻逼=杂鱼🐟♡\n。= "
        private const val CONFIG_VERSION_KEY = "config_version"
        private const val CONFIG_VERSION = 3

        const val DEFAULT_FLOAT_BALL_SIZE = 56
        const val MIN_FLOAT_BALL_SIZE = 40
        const val MAX_FLOAT_BALL_SIZE = 96
        const val DEFAULT_FLOAT_BALL_ALPHA = 0.78f
        const val MIN_FLOAT_BALL_ALPHA = 0.2f
        const val MAX_FLOAT_BALL_ALPHA = 1f
        const val DEFAULT_FLOAT_BALL_X = -1
        const val DEFAULT_FLOAT_BALL_Y = -1

        val defaultRules = parseRules(DEFAULT_RULES_TEXT)

        fun load(context: Context): AssistantConfig = load(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

        fun load(preferences: SharedPreferences): AssistantConfig {
            val rulesText = preferences.getString(KEY_RULES, null)
            val useCurrentDefault = preferences.getInt(CONFIG_VERSION_KEY, 0) < CONFIG_VERSION &&
                (rulesText == null || rulesText == PREVIOUS_DEFAULT_RULES_TEXT)
            val legacyFloatBallEnabled = preferences.getBoolean(KEY_FLOAT_BALL, false)
            val storedMode = ProcessingMode.fromValue(preferences.getString(KEY_MODE, ProcessingMode.PUNCTUATION.value))
            return AssistantConfig(
                enabled = preferences.getBoolean(KEY_ENABLED, false),
                processingMode = if (legacyFloatBallEnabled) ProcessingMode.FLOATING else storedMode,
                enableAppend = preferences.getBoolean(KEY_APPEND_ENABLED, true),
                appendText = preferences.getString(KEY_APPEND_TEXT, "喵~").orEmpty(),
                appendProbability = preferences.getInt(KEY_APPEND_PROBABILITY, 100).coerceIn(0, 100),
                enableEmoticon = preferences.getBoolean(KEY_EMOTICON_ENABLED, true),
                emoticonProbability = preferences.getInt(KEY_EMOTICON_PROBABILITY, 100).coerceIn(0, 100),
                enableSmartEmoticon = preferences.getBoolean(KEY_SMART_EMOTICON, false),
                enableRandomText = preferences.getBoolean(KEY_RANDOM_TEXT, true),
                randomTextProbability = preferences.getInt(KEY_RANDOM_TEXT_PROBABILITY, 100).coerceIn(0, 100),
                protectInputMethods = preferences.getBoolean(KEY_PROTECT_INPUT_METHODS, true),
                protectPasswords = preferences.getBoolean(KEY_PROTECT_PASSWORDS, true),
                floatBallSize = preferences.getInt(KEY_FLOAT_BALL_SIZE, DEFAULT_FLOAT_BALL_SIZE)
                    .coerceIn(MIN_FLOAT_BALL_SIZE, MAX_FLOAT_BALL_SIZE),
                floatBallAlpha = preferences.getFloat(KEY_FLOAT_BALL_ALPHA, DEFAULT_FLOAT_BALL_ALPHA)
                    .coerceIn(MIN_FLOAT_BALL_ALPHA, MAX_FLOAT_BALL_ALPHA),
                floatBallX = preferences.getInt(KEY_FLOAT_BALL_X, DEFAULT_FLOAT_BALL_X),
                floatBallY = preferences.getInt(KEY_FLOAT_BALL_Y, DEFAULT_FLOAT_BALL_Y),
                rules = if (useCurrentDefault) defaultRules else parseRules(rulesText),
                customEmoticons = splitLines(preferences.getString(KEY_CUSTOM_EMOTICONS, null)),
                customTexts = splitLines(preferences.getString(KEY_CUSTOM_TEXTS, null)),
                selectedPackages = preferences.getStringSet(KEY_SELECTED_PACKAGES, emptySet()).orEmpty().toSet(),
            )
        }

        fun toJson(config: AssistantConfig): String = JSONObject().apply {
            put("version", 1)
            put("processingMode", config.processingMode.value)
            put("enableAppend", config.enableAppend)
            put("appendText", config.appendText)
            put("appendProbability", config.appendProbability)
            put("enableEmoticon", config.enableEmoticon)
            put("emoticonProbability", config.emoticonProbability)
            put("enableSmartEmoticon", config.enableSmartEmoticon)
            put("enableRandomText", config.enableRandomText)
            put("randomTextProbability", config.randomTextProbability)
            put("protectInputMethods", config.protectInputMethods)
            put("protectPasswords", config.protectPasswords)
            put("floatBallSize", config.floatBallSize)
            put("floatBallAlpha", config.floatBallAlpha.toDouble())
            put("selectedPackages", JSONArray(config.selectedPackages.sorted()))
            put("rules", JSONArray(config.rules.map { JSONObject().put("from", it.from).put("to", it.to) }))
            put("customEmoticons", JSONArray(config.customEmoticons))
            put("customTexts", JSONArray(config.customTexts))
        }.toString(2)

        fun fromJson(json: String, current: AssistantConfig): AssistantConfig {
            val root = JSONObject(json)
            fun stringList(name: String): List<String> {
                val values = root.optJSONArray(name) ?: return emptyList()
                return buildList {
                    for (index in 0 until values.length()) values.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }
            val importedRules = root.optJSONArray("rules")?.let { values ->
                buildList {
                    for (index in 0 until values.length()) {
                        val item = values.optJSONObject(index) ?: continue
                        val from = item.optString("from").trim()
                        if (from.isNotEmpty()) add(ReplacementRule(from, item.optString("to")))
                    }
                }
            }.orEmpty()
            return current.copy(
                processingMode = ProcessingMode.fromValue(root.optString("processingMode", current.processingMode.value)),
                enableAppend = root.optBoolean("enableAppend", current.enableAppend),
                appendText = root.optString("appendText", current.appendText),
                appendProbability = root.optInt("appendProbability", current.appendProbability).coerceIn(0, 100),
                enableEmoticon = root.optBoolean("enableEmoticon", current.enableEmoticon),
                emoticonProbability = root.optInt("emoticonProbability", current.emoticonProbability).coerceIn(0, 100),
                enableSmartEmoticon = root.optBoolean("enableSmartEmoticon", current.enableSmartEmoticon),
                enableRandomText = root.optBoolean("enableRandomText", current.enableRandomText),
                randomTextProbability = root.optInt("randomTextProbability", current.randomTextProbability).coerceIn(0, 100),
                protectInputMethods = root.optBoolean("protectInputMethods", current.protectInputMethods),
                protectPasswords = root.optBoolean("protectPasswords", current.protectPasswords),
                floatBallSize = root.optInt("floatBallSize", current.floatBallSize).coerceIn(MIN_FLOAT_BALL_SIZE, MAX_FLOAT_BALL_SIZE),
                floatBallAlpha = root.optDouble("floatBallAlpha", current.floatBallAlpha.toDouble()).toFloat()
                    .coerceIn(MIN_FLOAT_BALL_ALPHA, MAX_FLOAT_BALL_ALPHA),
                selectedPackages = stringList("selectedPackages").toSet(),
                rules = importedRules.ifEmpty { current.rules },
                customEmoticons = stringList("customEmoticons"),
                customTexts = stringList("customTexts"),
            )
        }

        fun parseRules(text: String?): List<ReplacementRule> = text.orEmpty()
            .lineSequence()
            .mapNotNull(::parseRule)
            .toList()

        fun parseRule(line: String?): ReplacementRule? {
            val value = line?.trim().orEmpty()
            if (value.isEmpty()) return null
            val separator = value.indexOfFirst { it == '=' || it == '＝' || it == '→' }
            if (separator <= 0) return null
            val from = value.substring(0, separator).trim()
            if (from.isEmpty()) return null
            return ReplacementRule(from, value.substring(separator + 1).replace("\r", "").replace("\n", ""))
        }

        fun rulesToText(rules: List<ReplacementRule>): String = rules
            .filter { it.from.isNotBlank() }
            .joinToString("\n")

        private fun splitLines(value: String?): List<String> = value.orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
    }
}

