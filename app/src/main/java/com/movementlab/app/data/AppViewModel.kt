package com.movementlab.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

data class ActiveItem(
    val exerciseId: String,
    val sets: Int,
    val target: Int,
    val restSec: Int,
    val logged: List<SetLog> = emptyList(),
)

data class ActiveWorkout(
    val routineName: String,
    val startedAt: Long,
    val items: List<ActiveItem>,
    val index: Int = 0,
)

/** One point on an exercise's progress chart. */
data class ProgressPoint(val date: Long, val bestValue: Int, val bestWeightKg: Double, val totalValue: Int)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dataFile = File(app.filesDir, "movementlab.json")
    private val activeFile = File(app.filesDir, "active_workout.json")

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _active = MutableStateFlow(loadActive())
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

    /** Wall-clock millis when the current rest period ends, or null. */
    private val _restEndsAt = MutableStateFlow<Long?>(null)
    val restEndsAt: StateFlow<Long?> = _restEndsAt.asStateFlow()

    // ------------------------------------------------------------------ load/save

    private fun load(): AppData = runCatching {
        if (dataFile.exists()) appDataFromJson(JSONObject(dataFile.readText())) else AppData()
    }.getOrElse { AppData() }

    private fun persist() {
        val json = _data.value.toJson().toString()
        viewModelScope.launch(Dispatchers.IO) {
            val tmp = File(dataFile.parentFile, "movementlab.json.tmp")
            tmp.writeText(json)
            tmp.renameTo(dataFile)
        }
    }

    private fun update(f: (AppData) -> AppData) {
        _data.value = f(_data.value)
        persist()
    }

    private fun loadActive(): ActiveWorkout? = runCatching {
        if (!activeFile.exists()) return@runCatching null
        val o = JSONObject(activeFile.readText())
        val items = o.getJSONArray("items")
        ActiveWorkout(
            routineName = o.getString("routineName"),
            startedAt = o.getLong("startedAt"),
            index = o.optInt("index", 0),
            items = (0 until items.length()).map { i ->
                val it = items.getJSONObject(i)
                val logged = it.getJSONArray("logged")
                ActiveItem(
                    it.getString("exerciseId"), it.getInt("sets"), it.getInt("target"), it.getInt("restSec"),
                    (0 until logged.length()).map { j ->
                        val s = logged.getJSONObject(j)
                        SetLog(s.getInt("value"), s.getDouble("weightKg"))
                    },
                )
            },
        )
    }.getOrNull()

    private fun setActive(w: ActiveWorkout?) {
        _active.value = w
        viewModelScope.launch(Dispatchers.IO) {
            if (w == null) {
                activeFile.delete()
            } else {
                val o = JSONObject().put("routineName", w.routineName).put("startedAt", w.startedAt)
                    .put("index", w.index)
                    .put("items", JSONArray(w.items.map { it2 ->
                        JSONObject().put("exerciseId", it2.exerciseId).put("sets", it2.sets)
                            .put("target", it2.target).put("restSec", it2.restSec)
                            .put("logged", JSONArray(it2.logged.map {
                                JSONObject().put("value", it.value).put("weightKg", it.weightKg)
                            }))
                    }))
                activeFile.writeText(o.toString())
            }
        }
    }

    // ------------------------------------------------------------------ exercises

    fun allExercises(d: AppData = _data.value): List<Exercise> =
        Seed.exercises.map { e -> d.videoOverrides[e.id]?.let { e.copy(videoUrl = it) } ?: e } + d.customExercises

    fun exercise(id: String, d: AppData = _data.value): Exercise? = allExercises(d).firstOrNull { it.id == id }

    fun ladderOf(e: Exercise, d: AppData = _data.value): List<Exercise> =
        if (e.ladder == null) listOf(e)
        else allExercises(d).filter { it.ladder == e.ladder }.sortedBy { it.step }

    fun easier(e: Exercise): Exercise? = ladderOf(e).let { l -> l.getOrNull(l.indexOfFirst { it.id == e.id } - 1) }
    fun harder(e: Exercise): Exercise? = ladderOf(e).let { l ->
        val i = l.indexOfFirst { it.id == e.id }
        if (i < 0) null else l.getOrNull(i + 1)
    }

    fun setVideo(exerciseId: String, url: String) = update { d ->
        val custom = d.customExercises.firstOrNull { it.id == exerciseId }
        if (custom != null) {
            d.copy(customExercises = d.customExercises.map { if (it.id == exerciseId) it.copy(videoUrl = url) else it })
        } else {
            val seed = Seed.exercises.firstOrNull { it.id == exerciseId }
            val overrides = if (url.isBlank() && seed != null) d.videoOverrides - exerciseId
            else d.videoOverrides + (exerciseId to url)
            d.copy(videoOverrides = overrides)
        }
    }

    fun addCustomExercise(name: String, category: String, tracking: Tracking, videoUrl: String, cues: List<String>): Exercise {
        val e = Exercise(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            category = category,
            level = _data.value.settings.level ?: Level.BEGINNER,
            tracking = tracking,
            ladder = null,
            step = 0,
            equipment = "",
            cues = cues,
            videoUrl = videoUrl.ifBlank { Seed.youtubeSearch(name) },
            custom = true,
        )
        update { it.copy(customExercises = it.customExercises + e) }
        return e
    }

    fun deleteCustomExercise(id: String) = update { d ->
        d.copy(
            customExercises = d.customExercises.filterNot { it.id == id },
            routines = d.routines.map { r -> r.copy(items = r.items.filterNot { it.exerciseId == id }) },
        )
    }

    // ------------------------------------------------------------------ routines

    fun allRoutines(d: AppData = _data.value): List<Routine> = d.routines + Seed.routines

    fun routine(id: String): Routine? = allRoutines().firstOrNull { it.id == id }

    fun saveRoutine(r: Routine): Routine {
        val saved = if (r.builtIn || r.id.isBlank()) r.copy(id = "r_" + UUID.randomUUID().toString().take(8), builtIn = false) else r
        update { d ->
            val exists = d.routines.any { it.id == saved.id }
            d.copy(routines = if (exists) d.routines.map { if (it.id == saved.id) saved else it } else listOf(saved) + d.routines)
        }
        return saved
    }

    fun deleteRoutine(id: String) = update { d -> d.copy(routines = d.routines.filterNot { it.id == id }) }

    // ------------------------------------------------------------------ settings

    fun setLevel(level: Level) = update { it.copy(settings = it.settings.copy(level = level)) }
    fun setUseLb(useLb: Boolean) = update { it.copy(settings = it.settings.copy(useLb = useLb)) }
    fun setDefaultRest(sec: Int) = update { it.copy(settings = it.settings.copy(defaultRestSec = sec.coerceIn(15, 600))) }

    // ------------------------------------------------------------------ workout

    fun startWorkout(r: Routine) {
        setActive(ActiveWorkout(r.name, System.currentTimeMillis(), r.items.map { ActiveItem(it.exerciseId, it.sets, it.target, it.restSec) }))
        _restEndsAt.value = null
    }

    fun startEmptyWorkout() {
        setActive(ActiveWorkout("Quick workout", System.currentTimeMillis(), emptyList()))
        _restEndsAt.value = null
    }

    private fun updateActive(f: (ActiveWorkout) -> ActiveWorkout) {
        _active.value?.let { setActive(f(it)) }
    }

    fun goTo(index: Int) = updateActive { w -> w.copy(index = index.coerceIn(0, (w.items.size - 1).coerceAtLeast(0))) }

    fun logSet(itemIndex: Int, value: Int, weightKg: Double) {
        val w = _active.value ?: return
        val item = w.items.getOrNull(itemIndex) ?: return
        updateActive { a ->
            a.copy(items = a.items.mapIndexed { i, it -> if (i == itemIndex) it.copy(logged = it.logged + SetLog(value, weightKg)) else it })
        }
        val rest = if (item.restSec > 0) item.restSec else _data.value.settings.defaultRestSec
        _restEndsAt.value = System.currentTimeMillis() + rest * 1000L
    }

    fun removeSet(itemIndex: Int, setIndex: Int) = updateActive { a ->
        a.copy(items = a.items.mapIndexed { i, it ->
            if (i == itemIndex) it.copy(logged = it.logged.filterIndexed { j, _ -> j != setIndex }) else it
        })
    }

    fun adjustRest(deltaSec: Int) {
        _restEndsAt.value = _restEndsAt.value?.let { it + deltaSec * 1000L }
    }

    fun skipRest() { _restEndsAt.value = null }

    /** Replace the exercise at [itemIndex] with an easier/harder version (keeps any sets already logged as a separate entry). */
    fun swapExercise(itemIndex: Int, newId: String) = updateActive { a ->
        val old = a.items.getOrNull(itemIndex) ?: return@updateActive a
        val replacement = old.copy(exerciseId = newId, logged = emptyList())
        val items = a.items.toMutableList()
        if (old.logged.isEmpty()) {
            items[itemIndex] = replacement
        } else {
            items[itemIndex] = old.copy(sets = old.logged.size)
            items.add(itemIndex + 1, replacement.copy(sets = (old.sets - old.logged.size).coerceAtLeast(1)))
        }
        a.copy(items = items, index = if (old.logged.isEmpty()) itemIndex else itemIndex + 1)
    }

    fun addExerciseToWorkout(exerciseId: String) = updateActive { a ->
        val e = exercise(exerciseId)
        val target = if (e?.tracking == Tracking.SECONDS) 30 else 10
        a.copy(items = a.items + ActiveItem(exerciseId, 3, target, _data.value.settings.defaultRestSec), index = a.items.size)
    }

    fun removeExerciseFromWorkout(itemIndex: Int) = updateActive { a ->
        val items = a.items.filterIndexed { i, _ -> i != itemIndex }
        a.copy(items = items, index = a.index.coerceAtMost((items.size - 1).coerceAtLeast(0)))
    }

    /** Saves the workout. Returns the saved session, or null if nothing was logged. */
    fun finishWorkout(): Session? {
        val w = _active.value ?: return null
        val entries = w.items.filter { it.logged.isNotEmpty() }.map { ExerciseLog(it.exerciseId, it.logged, it.target) }
        setActive(null)
        _restEndsAt.value = null
        if (entries.isEmpty()) return null
        val s = Session(
            id = UUID.randomUUID().toString(),
            startedAt = w.startedAt,
            durationSec = ((System.currentTimeMillis() - w.startedAt) / 1000).toInt(),
            routineName = w.routineName,
            entries = entries,
        )
        update { it.copy(sessions = listOf(s) + it.sessions) }
        return s
    }

    fun discardWorkout() {
        setActive(null)
        _restEndsAt.value = null
    }

    fun deleteSession(id: String) = update { d -> d.copy(sessions = d.sessions.filterNot { it.id == id }) }

    // ------------------------------------------------------------------ history / progress

    /** Most recent logged sets for an exercise, excluding the workout in progress. */
    fun lastLog(exerciseId: String, d: AppData = _data.value): ExerciseLog? =
        d.sessions.asSequence().mapNotNull { s -> s.entries.firstOrNull { it.exerciseId == exerciseId } }.firstOrNull()

    fun progress(exerciseId: String, d: AppData = _data.value): List<ProgressPoint> =
        d.sessions.mapNotNull { s ->
            val e = s.entries.firstOrNull { it.exerciseId == exerciseId } ?: return@mapNotNull null
            if (e.sets.isEmpty()) return@mapNotNull null
            ProgressPoint(s.startedAt, e.sets.maxOf { it.value }, e.sets.maxOf { it.weightKg }, e.sets.sumOf { it.value })
        }.sortedBy { it.date }

    /**
     * True when the last time this exercise was done, every set hit its target
     * (at least 2 sets). That's the signal to move to the next step of the ladder.
     */
    fun readyToProgress(exerciseId: String, d: AppData = _data.value): Boolean {
        val log = lastLog(exerciseId, d) ?: return false
        return log.target > 0 && log.sets.size >= 2 && log.sets.all { it.value >= log.target }
    }

    /**
     * Consecutive weeks (Mon-Sun) with at least one workout. Rest days are part
     * of training, so streaks count weeks rather than days. The current week
     * doesn't break the streak until it's over.
     */
    fun weekStreak(d: AppData = _data.value): Int {
        val weeks = d.sessions.map { weekKey(it.startedAt) }.toSet()
        val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
        var streak = 0
        if (weekKey(cal.timeInMillis) in weeks) streak++
        while (true) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            if (weekKey(cal.timeInMillis) in weeks) streak++ else break
        }
        return streak
    }

    fun workoutsThisWeek(d: AppData = _data.value): Int {
        val now = weekKey(System.currentTimeMillis())
        return d.sessions.count { weekKey(it.startedAt) == now }
    }

    // ------------------------------------------------------------------ body stats

    fun addBodyWeight(kg: Double) = update { d -> d.copy(body = (d.body + BodyEntry(System.currentTimeMillis(), kg)).sortedBy { it.date }) }
    fun deleteBodyEntry(date: Long) = update { d -> d.copy(body = d.body.filterNot { it.date == date }) }

    // ------------------------------------------------------------------ backup

    fun exportJson(): String = _data.value.toJson().toString(2)

    fun importJson(text: String): Boolean = runCatching {
        val o = JSONObject(text)
        require(o.has("version") && o.has("sessions")) { "Not a MovementLab backup" }
        val imported = appDataFromJson(o)
        _data.value = imported
        persist()
        true
    }.getOrElse { false }

    companion object {
        fun dayKey(millis: Long): Int {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }

        /** Day key of the Monday starting the week that contains [millis]. */
        fun weekKey(millis: Long): Int {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            val back = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            c.add(Calendar.DAY_OF_YEAR, -back)
            return dayKey(c.timeInMillis)
        }
    }
}
