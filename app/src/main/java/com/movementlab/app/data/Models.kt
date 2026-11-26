package com.movementlab.app.data

import org.json.JSONArray
import org.json.JSONObject

enum class Level(val label: String, val blurb: String) {
    BEGINNER("Beginner", "New to training or coming back after a long break. Builds the base: push-ups, rows, squats, planks."),
    INTERMEDIATE("Intermediate", "Can do a few strict pull-ups and dips. Adds skill work like L-sits and handstands."),
    ADVANCED("Advanced", "Solid pull-ups and dips already. Weighted work, muscle-ups, levers and pistols.");
}

/** How a set is measured: number of reps, or seconds held. */
enum class Tracking { REPS, SECONDS }

data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val level: Level,
    val tracking: Tracking,
    /** Exercises in the same ladder are easier/harder versions of one movement. */
    val ladder: String?,
    val step: Int,
    val equipment: String,
    val cues: List<String>,
    val videoUrl: String,
    val custom: Boolean = false,
)

data class RoutineItem(
    val exerciseId: String,
    val sets: Int,
    /** Target reps, or target seconds for holds. */
    val target: Int,
    val restSec: Int,
)

data class Routine(
    val id: String,
    val name: String,
    val level: Level?,
    val description: String,
    val items: List<RoutineItem>,
    val builtIn: Boolean = false,
)

/**
 * One logged set. [weightKg] is extra load: positive for a vest/belt/dumbbell,
 * negative for band or machine assistance, 0 for plain bodyweight.
 */
data class SetLog(val value: Int, val weightKg: Double)

data class ExerciseLog(val exerciseId: String, val sets: List<SetLog>, val target: Int = 0)

data class Session(
    val id: String,
    val startedAt: Long,
    val durationSec: Int,
    val routineName: String,
    val entries: List<ExerciseLog>,
)

data class BodyEntry(val date: Long, val weightKg: Double)

data class Settings(
    val level: Level? = null,
    val useLb: Boolean = false,
    val defaultRestSec: Int = 90,
)

data class AppData(
    val settings: Settings = Settings(),
    val customExercises: List<Exercise> = emptyList(),
    val videoOverrides: Map<String, String> = emptyMap(),
    val routines: List<Routine> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val body: List<BodyEntry> = emptyList(),
)

// ---------------------------------------------------------------------------
// JSON (uses Android's built-in org.json so the app needs no extra libraries)
// ---------------------------------------------------------------------------

private fun <T> JSONArray?.mapObjects(f: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let { runCatching { f(it) }.getOrNull() } }
}

private fun <E : Enum<E>> enumOrNull(values: Array<E>, name: String?): E? =
    values.firstOrNull { it.name == name }

fun Exercise.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("category", category)
    .put("level", level.name).put("tracking", tracking.name)
    .put("ladder", ladder ?: JSONObject.NULL).put("step", step)
    .put("equipment", equipment).put("cues", JSONArray(cues))
    .put("videoUrl", videoUrl).put("custom", custom)

fun exerciseFromJson(o: JSONObject): Exercise = Exercise(
    id = o.getString("id"),
    name = o.getString("name"),
    category = o.optString("category", "Other"),
    level = enumOrNull(Level.values(), o.optString("level")) ?: Level.BEGINNER,
    tracking = enumOrNull(Tracking.values(), o.optString("tracking")) ?: Tracking.REPS,
    ladder = if (o.isNull("ladder")) null else o.optString("ladder"),
    step = o.optInt("step", 0),
    equipment = o.optString("equipment", "None"),
    cues = o.optJSONArray("cues")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
    videoUrl = o.optString("videoUrl", ""),
    custom = o.optBoolean("custom", true),
)

fun Routine.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("level", level?.name ?: JSONObject.NULL)
    .put("description", description)
    .put("items", JSONArray(items.map {
        JSONObject().put("exerciseId", it.exerciseId).put("sets", it.sets)
            .put("target", it.target).put("restSec", it.restSec)
    }))

fun routineFromJson(o: JSONObject): Routine = Routine(
    id = o.getString("id"),
    name = o.getString("name"),
    level = if (o.isNull("level")) null else enumOrNull(Level.values(), o.optString("level")),
    description = o.optString("description", ""),
    items = o.optJSONArray("items").mapObjects {
        RoutineItem(it.getString("exerciseId"), it.optInt("sets", 3), it.optInt("target", 10), it.optInt("restSec", 90))
    },
)

fun Session.toJson(): JSONObject = JSONObject()
    .put("id", id).put("startedAt", startedAt).put("durationSec", durationSec)
    .put("routineName", routineName)
    .put("entries", JSONArray(entries.map { e ->
        JSONObject().put("exerciseId", e.exerciseId).put("target", e.target)
            .put("sets", JSONArray(e.sets.map { JSONObject().put("value", it.value).put("weightKg", it.weightKg) }))
    }))

fun sessionFromJson(o: JSONObject): Session = Session(
    id = o.getString("id"),
    startedAt = o.getLong("startedAt"),
    durationSec = o.optInt("durationSec", 0),
    routineName = o.optString("routineName", "Workout"),
    entries = o.optJSONArray("entries").mapObjects { e ->
        ExerciseLog(
            exerciseId = e.getString("exerciseId"),
            sets = e.optJSONArray("sets").mapObjects { SetLog(it.optInt("value"), it.optDouble("weightKg", 0.0)) },
            target = e.optInt("target", 0),
        )
    },
)

fun AppData.toJson(): JSONObject {
    val overrides = JSONObject()
    videoOverrides.forEach { (k, v) -> overrides.put(k, v) }
    return JSONObject()
        .put("version", 1)
        .put("settings", JSONObject()
            .put("level", settings.level?.name ?: JSONObject.NULL)
            .put("useLb", settings.useLb)
            .put("defaultRestSec", settings.defaultRestSec))
        .put("customExercises", JSONArray(customExercises.map { it.toJson() }))
        .put("videoOverrides", overrides)
        .put("routines", JSONArray(routines.map { it.toJson() }))
        .put("sessions", JSONArray(sessions.map { it.toJson() }))
        .put("body", JSONArray(body.map { JSONObject().put("date", it.date).put("weightKg", it.weightKg) }))
}

fun appDataFromJson(o: JSONObject): AppData {
    val s = o.optJSONObject("settings")
    val overrides = mutableMapOf<String, String>()
    o.optJSONObject("videoOverrides")?.let { v -> v.keys().forEach { k -> overrides[k] = v.getString(k) } }
    return AppData(
        settings = Settings(
            level = if (s == null || s.isNull("level")) null else enumOrNull(Level.values(), s.optString("level")),
            useLb = s?.optBoolean("useLb", false) ?: false,
            defaultRestSec = s?.optInt("defaultRestSec", 90) ?: 90,
        ),
        customExercises = o.optJSONArray("customExercises").mapObjects(::exerciseFromJson),
        videoOverrides = overrides,
        routines = o.optJSONArray("routines").mapObjects(::routineFromJson),
        sessions = o.optJSONArray("sessions").mapObjects(::sessionFromJson),
        body = o.optJSONArray("body").mapObjects { BodyEntry(it.getLong("date"), it.getDouble("weightKg")) },
    )
}
