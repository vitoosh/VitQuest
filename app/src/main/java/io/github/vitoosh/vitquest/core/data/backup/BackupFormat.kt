package io.github.vitoosh.vitquest.core.data.backup

import io.github.vitoosh.vitquest.core.data.preferences.PreferencesBackup
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** One table's rows. Values are Long, Double, String or null, as SQLite stores them. */
typealias BackupRows = List<Map<String, Any?>>

/** Everything a backup file holds, in the shape of database [databaseVersion]. */
data class BackupSnapshot(
    val databaseVersion: Int,
    val tables: Map<String, BackupRows>,
    val preferences: PreferencesBackup,
)

class BackupException(val error: RestoreError, cause: Throwable? = null) : Exception(error.name, cause)

/**
 * The backup file: plain JSON, so it can be checked and is not tied to one phone.
 *
 * ```
 * { "app": "VitQuest", "format": 1, "databaseVersion": 6, "exportedAt": "...",
 *   "preferences": { ... }, "tables": { "weight_entries": [ { "id": 1, ... } ], ... } }
 * ```
 */
object BackupFormat {
    const val APP = "VitQuest"
    const val FORMAT = 1

    private val json = Json { prettyPrint = false }

    fun encode(snapshot: BackupSnapshot, exportedAt: Instant): String {
        val root = buildJsonObject {
            put("app", APP)
            put("format", FORMAT)
            put("databaseVersion", snapshot.databaseVersion)
            put("exportedAt", exportedAt.toString())
            put("preferences", encodePreferences(snapshot.preferences))
            put(
                "tables",
                JsonObject(
                    snapshot.tables.toSortedMap().mapValues { (_, rows) ->
                        buildJsonArray { rows.forEach { row -> add(JsonObject(row.mapValues { (_, value) -> encodeValue(value) })) } }
                    },
                ),
            )
        }
        return json.encodeToString(JsonElement.serializer(), root)
    }

    /** Throws [BackupException] when [text] is not a VitQuest backup or is incomplete. */
    fun decode(text: String): BackupSnapshot {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrElse { throw BackupException(RestoreError.NOT_A_BACKUP, it) }
        if ((root["app"] as? JsonPrimitive)?.contentOrNull != APP) throw BackupException(RestoreError.NOT_A_BACKUP)
        val format = (root["format"] as? JsonPrimitive)?.intOrNull ?: throw BackupException(RestoreError.DAMAGED)
        if (format > FORMAT) throw BackupException(RestoreError.NEWER_APP)
        val version = (root["databaseVersion"] as? JsonPrimitive)?.intOrNull ?: throw BackupException(RestoreError.DAMAGED)
        return try {
            val tables = (root["tables"] as JsonObject).mapValues { (_, rows) ->
                (rows as JsonArray).map { row -> (row as JsonObject).mapValues { (_, value) -> decodeValue(value) } }
            }
            BackupSnapshot(version, tables, decodePreferences(root["preferences"] as? JsonObject))
        } catch (error: RuntimeException) {
            // A cast failed: some part of the file is not shaped like a backup.
            throw BackupException(RestoreError.DAMAGED, error)
        }
    }

    private fun encodeValue(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Long -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        else -> error("Unsupported column value ${value::class.simpleName}")
    }

    private fun decodeValue(value: JsonElement): Any? {
        val primitive = value as JsonPrimitive
        return when {
            primitive is JsonNull -> null
            primitive.isString -> primitive.content
            else -> primitive.longOrNull ?: primitive.content.toDouble()
        }
    }

    private fun encodePreferences(preferences: PreferencesBackup) = buildJsonObject {
        preferences.themeMode?.let { put("themeMode", it) }
        preferences.stepSource?.let { put("stepSource", it) }
        put("backgroundStepTracking", preferences.backgroundStepTracking)
        put("onboardingCompleted", preferences.onboardingCompleted)
        put("seedDataVersion", preferences.seedDataVersion)
    }

    private fun decodePreferences(prefs: JsonObject?): PreferencesBackup {
        fun primitive(key: String) = prefs?.get(key) as? JsonPrimitive
        return PreferencesBackup(
            themeMode = primitive("themeMode")?.contentOrNull,
            stepSource = primitive("stepSource")?.contentOrNull,
            backgroundStepTracking = primitive("backgroundStepTracking")?.booleanOrNull ?: false,
            onboardingCompleted = primitive("onboardingCompleted")?.booleanOrNull ?: false,
            seedDataVersion = primitive("seedDataVersion")?.intOrNull ?: 0,
        )
    }
}
