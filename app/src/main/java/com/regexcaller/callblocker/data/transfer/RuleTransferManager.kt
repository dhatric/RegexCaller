package com.regexcaller.callblocker.data.transfer

import android.content.Context
import android.net.Uri
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction
import com.regexcaller.callblocker.data.repository.BlockRuleRepository
import com.regexcaller.callblocker.engine.PatternMatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class RuleTransferManager(
    context: Context,
    private val repository: BlockRuleRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val contentResolver = context.contentResolver

    suspend fun exportRules(uri: Uri): Int = withContext(ioDispatcher) {
        val rules = repository.getAllRulesSnapshot()
        val backup = RuleBackupFile(
            version = RuleTransferCodec.VERSION,
            exportedAt = System.currentTimeMillis(),
            rules = rules.map {
                ExportedRule(
                    label = it.label,
                    pattern = it.pattern,
                    isRegex = it.isRegex,
                    action = it.action,
                    isEnabled = it.isEnabled
                )
            }
        )
        val json = RuleTransferCodec.encode(backup)
        val outputStream = contentResolver.openOutputStream(uri)
            ?: throw RuleTransferException("Unable to open export destination.")

        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
        }

        rules.size
    }

    suspend fun importRules(uri: Uri): RuleImportStats = withContext(ioDispatcher) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw RuleTransferException("Unable to open selected backup file.")
        val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val backup = try {
            RuleTransferCodec.decode(content)
        } catch (e: JSONException) {
            throw RuleTransferException("Backup file is not valid JSON.")
        }

        val validatedRules = RuleTransferValidator.validate(backup)
        repository.importRules(validatedRules.map { exportedRule ->
            BlockRule(
                label = exportedRule.label,
                pattern = exportedRule.pattern,
                isRegex = exportedRule.isRegex,
                action = exportedRule.action,
                isEnabled = exportedRule.isEnabled,
                matchCount = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        })
    }
}

data class RuleBackupFile(
    val version: Int,
    val exportedAt: Long,
    val rules: List<ExportedRule>
)

data class ExportedRule(
    val label: String,
    val pattern: String,
    val isRegex: Boolean,
    val action: String,
    val isEnabled: Boolean
)

data class RuleImportStats(
    val importedCount: Int,
    val skippedCount: Int
)

object RuleTransferCodec {
    const val VERSION = 1

    fun encode(backup: RuleBackupFile): String {
        val rulesArray = JSONArray()
        backup.rules.forEach { rule ->
            rulesArray.put(
                JSONObject()
                    .put("label", rule.label)
                    .put("pattern", rule.pattern)
                    .put("isRegex", rule.isRegex)
                    .put("action", rule.action)
                    .put("isEnabled", rule.isEnabled)
            )
        }

        return JSONObject()
            .put("version", backup.version)
            .put("exportedAt", backup.exportedAt)
            .put("rules", rulesArray)
            .toString(2)
    }

    fun decode(json: String): RuleBackupFile {
        val root = JSONObject(json)
        val rules = root.getJSONArray("rules").toExportedRules()
        return RuleBackupFile(
            version = root.getInt("version"),
            exportedAt = root.getLong("exportedAt"),
            rules = rules
        )
    }

    private fun JSONArray.toExportedRules(): List<ExportedRule> =
        buildList(length()) {
            for (index in 0 until length()) {
                val rule = getJSONObject(index)
                add(
                    ExportedRule(
                        label = rule.getString("label"),
                        pattern = rule.getString("pattern"),
                        isRegex = rule.getBoolean("isRegex"),
                        action = rule.getString("action"),
                        isEnabled = rule.getBoolean("isEnabled")
                    )
                )
            }
        }
}

object RuleTransferValidator {
    fun validate(backup: RuleBackupFile): List<ExportedRule> {
        if (backup.version != RuleTransferCodec.VERSION) {
            throw RuleTransferException("Unsupported backup version: ${backup.version}.")
        }

        backup.rules.forEachIndexed { index, rule ->
            if (rule.pattern.isBlank()) {
                throw RuleTransferException("Rule ${index + 1} has an empty pattern.")
            }
            if (!BlockAction.isValid(rule.action)) {
                throw RuleTransferException("Rule ${index + 1} has an invalid action.")
            }

            val patternError = PatternMatcher.validatePattern(rule.pattern, rule.isRegex)
            if (patternError != null) {
                throw RuleTransferException("Rule ${index + 1} is invalid: $patternError")
            }
        }

        return backup.rules
    }
}

class RuleTransferException(message: String) : IOException(message)
