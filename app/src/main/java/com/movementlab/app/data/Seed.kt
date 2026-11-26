package com.movementlab.app.data

import java.net.URLEncoder

/**
 * Built-in exercise library and starter programs.
 *
 * Each built-in exercise has a demo video (see [Videos]) that plays inside the
 * app. Any exercise's video can be replaced from its screen in the app.
 */
object Seed {

    fun youtubeSearch(name: String): String =
        "https://www.youtube.com/results?search_query=" +
            URLEncoder.encode("$name calisthenics proper form tutorial", "UTF-8")

    /** The built-in video for an exercise, or a YouTube search if there isn't one. */
    fun defaultVideo(id: String, name: String): String =
        Videos.defaults[id]?.let(Videos::watchUrl) ?: youtubeSearch(name)

    private fun ex(
        id: String,
        name: String,
        category: String,
        level: Level,
        ladder: String?,
        step: Int,
        equipment: String,
        cues: List<String>,
        tracking: Tracking = Tracking.REPS,
    ) = Exercise(id, name, category, level, tracking, ladder, step, equipment, cues, defaultVideo(id, name), custom = false)

    private val B = Level.BEGINNER
    private val I = Level.INTERMEDIATE
    private val A = Level.ADVANCED
    private val S = Tracking.SECONDS

    val exercises: List<Exercise> = listOf(
        // ---- Push-up ladder
        ex("wall_pushup", "Wall Push-up", "Push", B, "pushup", 1, "Wall",
            listOf("Hands at chest height, shoulder-width", "Body in one straight line", "Chest to the wall, then press away")),
        ex("incline_pushup", "Incline Push-up", "Push", B, "pushup", 2, "Bench or counter",
            listOf("The higher the surface, the easier", "Squeeze glutes so hips don't sag", "Elbows about 45° from the body")),
        ex("knee_pushup", "Knee Push-up", "Push", B, "pushup", 3, "None",
            listOf("Straight line from knees to head", "Lower chest to an inch off the floor", "Full lockout at the top")),
        ex("pushup", "Push-up", "Push", B, "pushup", 4, "None",
            listOf("Hands under shoulders", "Brace abs and glutes", "Chest touches the floor every rep")),
        ex("diamond_pushup", "Diamond Push-up", "Push", I, "pushup", 5, "None",
            listOf("Thumbs and index fingers form a diamond", "Elbows track back, close to ribs", "Targets triceps")),
        ex("archer_pushup", "Archer Push-up", "Push", A, "pushup", 6, "None",
            listOf("Very wide hands", "Shift onto one arm, other arm stays straight", "Alternate sides each rep")),
        ex("one_arm_pushup", "One-Arm Push-up", "Push", A, "pushup", 7, "None",
            listOf("Feet wide for balance", "Free hand behind back", "Keep hips square to the floor")),

        // ---- Pull-up ladder
        ex("dead_hang", "Dead Hang", "Pull", B, "pullup", 1, "Pull-up bar",
            listOf("Full grip, thumbs around the bar", "Relax the shoulders up by the ears", "Builds grip and shoulder health"), S),
        ex("scap_pull", "Scapular Pull-up", "Pull", B, "pullup", 2, "Pull-up bar",
            listOf("Hang with straight arms", "Pull shoulder blades down and back", "Body rises a few inches, arms stay straight")),
        ex("australian_row", "Australian Row", "Pull", B, "pullup", 3, "Low bar or table",
            listOf("Body straight, heels on the floor", "Pull chest to the bar", "Walk feet forward to make it easier")),
        ex("negative_pullup", "Negative Pull-up", "Pull", B, "pullup", 4, "Pull-up bar",
            listOf("Jump or step to the top position", "Lower as slowly as you can (3-5 s)", "Full hang at the bottom")),
        ex("chinup", "Chin-up", "Pull", I, "pullup", 5, "Pull-up bar",
            listOf("Palms facing you, shoulder-width", "Chin clearly over the bar", "No kipping or swinging")),
        ex("pullup", "Pull-up", "Pull", I, "pullup", 6, "Pull-up bar",
            listOf("Palms away, slightly wider than shoulders", "Start from a full dead hang", "Drive elbows down toward hips")),
        ex("weighted_pullup", "Weighted Pull-up", "Pull", A, "pullup", 7, "Bar + vest or dip belt",
            listOf("Log the added weight", "Same strict form as bodyweight", "Add load only when 3×8 strict is easy")),
        ex("archer_pullup", "Archer Pull-up", "Pull", A, "pullup", 8, "Pull-up bar",
            listOf("Wide grip", "Pull toward one hand, other arm straightens", "Step toward the one-arm pull-up")),

        // ---- Dip ladder
        ex("bench_dip", "Bench Dip", "Push", B, "dip", 1, "Bench or chair",
            listOf("Hands behind you on the edge", "Lower until elbows are ~90°", "Keep shoulders down, away from ears")),
        ex("negative_dip", "Negative Dip", "Push", B, "dip", 2, "Parallel bars",
            listOf("Start at the top, arms locked", "Lower slowly for 3-5 seconds", "Step back up and repeat")),
        ex("dip", "Parallel Bar Dip", "Push", I, "dip", 3, "Parallel bars or dip station",
            listOf("Slight forward lean", "Shoulders just below elbows at the bottom", "Lock out fully at the top")),
        ex("weighted_dip", "Weighted Dip", "Push", A, "dip", 4, "Dip bars + belt or vest",
            listOf("Log the added weight", "Control the bottom position", "Add load once 3×12 is easy")),

        // ---- Muscle-up ladder
        ex("explosive_pullup", "Explosive Pull-up", "Skill", I, "muscleup", 1, "Pull-up bar",
            listOf("Pull as fast and high as possible", "Aim to get the bar to your lower chest", "Full reset between reps")),
        ex("straight_bar_dip", "Straight Bar Dip", "Skill", I, "muscleup", 2, "Pull-up bar",
            listOf("Support on top of the bar", "Lean over the bar as you lower", "Teaches the top half of a muscle-up")),
        ex("muscle_up", "Muscle-up", "Skill", A, "muscleup", 3, "Pull-up bar or rings",
            listOf("False grip or slightly over the bar", "Explosive pull, then lean over", "Press out to a locked support")),

        // ---- Squat ladder
        ex("assisted_squat", "Assisted Squat", "Legs", B, "squat", 1, "Doorframe or pole",
            listOf("Hold on lightly for balance", "Sit back and down", "Knees track over toes")),
        ex("squat", "Bodyweight Squat", "Legs", B, "squat", 2, "None",
            listOf("Feet shoulder-width", "Hips below knees if mobility allows", "Chest up, heels down")),
        ex("split_squat", "Split Squat", "Legs", B, "squat", 3, "None",
            listOf("Long stance, one foot forward", "Back knee lowers toward the floor", "Do all reps on one side, then switch")),
        ex("bulgarian_split_squat", "Bulgarian Split Squat", "Legs", I, "squat", 4, "Bench or chair",
            listOf("Back foot up on a bench", "Most weight on the front leg", "Slow on the way down")),
        ex("shrimp_squat", "Shrimp Squat", "Legs", A, "squat", 5, "None",
            listOf("Hold the back foot behind you", "Lower until the back knee touches", "Reps are per leg")),
        ex("pistol_squat", "Pistol Squat", "Legs", A, "squat", 6, "None",
            listOf("Other leg straight out in front", "Arms forward for balance", "Reps are per leg")),

        // ---- Hinge / posterior chain
        ex("glute_bridge", "Glute Bridge", "Legs", B, "hinge", 1, "None",
            listOf("Feet flat, close to hips", "Drive through heels", "Squeeze glutes at the top for 1 s")),
        ex("single_leg_bridge", "Single-Leg Glute Bridge", "Legs", I, "hinge", 2, "None",
            listOf("One leg straight in the air", "Hips stay level", "Reps are per leg")),
        ex("nordic_negative", "Nordic Curl Negative", "Legs", A, "hinge", 3, "Anchor for feet",
            listOf("Kneel with heels anchored", "Fall forward as slowly as possible", "Catch yourself with your hands")),

        // ---- Core
        ex("plank", "Plank", "Core", B, "plank", 1, "None",
            listOf("Elbows under shoulders", "Squeeze glutes, tuck pelvis", "Don't let hips sag"), S),
        ex("hollow_hold", "Hollow Body Hold", "Core", B, "plank", 2, "None",
            listOf("Lower back pressed into the floor", "Arms and legs extended, off the ground", "Bend knees to make it easier"), S),
        ex("leg_raise", "Lying Leg Raise", "Core", B, "legraise", 1, "None",
            listOf("Lower back flat", "Legs straight, raise to vertical", "Lower slowly without touching the floor")),
        ex("hanging_knee_raise", "Hanging Knee Raise", "Core", I, "legraise", 2, "Pull-up bar",
            listOf("No swinging", "Knees to chest", "Control the way down")),
        ex("hanging_leg_raise", "Hanging Leg Raise", "Core", I, "legraise", 3, "Pull-up bar",
            listOf("Legs straight", "Raise to at least hip height", "Shoulders engaged, not shrugged")),
        ex("toes_to_bar", "Toes to Bar", "Core", A, "legraise", 4, "Pull-up bar",
            listOf("Strict, no kip", "Touch the bar with your toes", "Slow negative")),
        ex("tuck_lsit", "Tuck L-sit", "Core", I, "lsit", 1, "Floor, parallettes or dip bars",
            listOf("Arms locked, shoulders pushed down", "Knees tucked to chest", "Hips off the floor"), S),
        ex("lsit", "L-sit", "Core", A, "lsit", 2, "Floor, parallettes or dip bars",
            listOf("Legs straight and together", "Toes pointed", "Push the floor away"), S),

        // ---- Handstand / overhead
        ex("pike_pushup", "Pike Push-up", "Push", I, "handstand", 1, "None",
            listOf("Hips high, body in an upside-down V", "Head lowers in front of hands", "Builds toward handstand push-ups")),
        ex("elevated_pike_pushup", "Elevated Pike Push-up", "Push", I, "handstand", 2, "Bench or box",
            listOf("Feet up on a bench", "Hips stacked over shoulders", "Head to the floor, press back up")),
        ex("wall_handstand", "Wall Handstand Hold", "Skill", I, "handstand", 3, "Wall",
            listOf("Chest facing the wall is best for form", "Arms locked, push tall through shoulders", "Squeeze glutes and legs"), S),
        ex("wall_hspu", "Wall Handstand Push-up", "Push", A, "handstand", 4, "Wall",
            listOf("Hands a hand-width from the wall", "Lower head to the floor with control", "Press to full lockout")),

        // ---- Front lever
        ex("tuck_front_lever", "Tuck Front Lever", "Skill", A, "frontlever", 1, "Pull-up bar",
            listOf("Arms straight", "Knees tucked to chest", "Back flat and horizontal"), S),
        ex("adv_tuck_front_lever", "Advanced Tuck Front Lever", "Skill", A, "frontlever", 2, "Pull-up bar",
            listOf("Open the hips so the back is flat", "Knees at 90°", "Pull the bar toward your hips"), S),

        // ---- Mobility (good for warm-ups and cool-downs)
        ex("arm_circles", "Arm Circles", "Mobility", B, null, 0, "None",
            listOf("Small to big circles", "Both directions"), S),
        ex("cat_cow", "Cat-Cow", "Mobility", B, null, 0, "None",
            listOf("On hands and knees", "Round then arch the spine slowly")),
        ex("deep_squat_hold", "Deep Squat Hold", "Mobility", B, null, 0, "None",
            listOf("Heels down, chest up", "Elbows push knees out"), S),
        ex("shoulder_dislocates", "Shoulder Dislocates", "Mobility", B, null, 0, "Band or broomstick",
            listOf("Wide grip", "Arms straight, bring stick overhead and behind")),
        ex("hip_flexor_stretch", "Hip Flexor Stretch", "Mobility", B, null, 0, "None",
            listOf("Half-kneeling", "Squeeze back glute, lean forward gently", "Time is per side"), S),
    )

    private fun item(id: String, sets: Int, target: Int, rest: Int = 90) = RoutineItem(id, sets, target, rest)

    val routines: List<Routine> = listOf(
        Routine("b_full_a", "Beginner Full Body A", Level.BEGINNER,
            "3 days a week, alternating A and B. Rest a day between workouts.",
            listOf(
                item("arm_circles", 1, 30, 15),
                item("incline_pushup", 3, 10),
                item("australian_row", 3, 8),
                item("squat", 3, 12),
                item("glute_bridge", 3, 12, 60),
                item("plank", 3, 30, 60),
            ), builtIn = true),
        Routine("b_full_b", "Beginner Full Body B", Level.BEGINNER,
            "Alternate with Full Body A.",
            listOf(
                item("arm_circles", 1, 30, 15),
                item("knee_pushup", 3, 8),
                item("negative_pullup", 3, 5, 120),
                item("split_squat", 3, 10),
                item("bench_dip", 3, 8),
                item("hollow_hold", 3, 20, 60),
                item("dead_hang", 3, 20, 60),
            ), builtIn = true),
        Routine("i_upper", "Intermediate Upper", Level.INTERMEDIATE,
            "4 days a week: Upper, Lower, rest, Upper, Lower.",
            listOf(
                item("shoulder_dislocates", 1, 10, 15),
                item("pullup", 4, 6, 120),
                item("dip", 4, 8, 120),
                item("chinup", 3, 8),
                item("diamond_pushup", 3, 10),
                item("pike_pushup", 3, 8),
            ), builtIn = true),
        Routine("i_lower", "Intermediate Lower + Skills", Level.INTERMEDIATE,
            "Legs, core and handstand practice.",
            listOf(
                item("deep_squat_hold", 1, 45, 15),
                item("bulgarian_split_squat", 4, 8),
                item("single_leg_bridge", 3, 10, 60),
                item("hanging_knee_raise", 3, 10, 60),
                item("tuck_lsit", 3, 15, 60),
                item("wall_handstand", 3, 30, 90),
            ), builtIn = true),
        Routine("a_push", "Advanced Push", Level.ADVANCED,
            "Push / Pull / Legs, 6 days a week or each once a week with rest days.",
            listOf(
                item("shoulder_dislocates", 1, 15, 15),
                item("weighted_dip", 4, 6, 150),
                item("wall_hspu", 4, 5, 150),
                item("archer_pushup", 3, 6, 120),
                item("lsit", 3, 15, 90),
            ), builtIn = true),
        Routine("a_pull", "Advanced Pull", Level.ADVANCED,
            "Heavy pulling plus muscle-up and lever skill work.",
            listOf(
                item("dead_hang", 1, 30, 30),
                item("muscle_up", 4, 3, 150),
                item("weighted_pullup", 4, 5, 150),
                item("archer_pullup", 3, 4, 120),
                item("tuck_front_lever", 4, 10, 90),
                item("toes_to_bar", 3, 8, 90),
            ), builtIn = true),
        Routine("a_legs", "Advanced Legs + Core", Level.ADVANCED,
            "Single-leg strength and hamstrings.",
            listOf(
                item("deep_squat_hold", 1, 60, 15),
                item("pistol_squat", 4, 5, 120),
                item("shrimp_squat", 3, 5, 120),
                item("nordic_negative", 3, 5, 120),
                item("hanging_leg_raise", 3, 10, 60),
            ), builtIn = true),
    )
}
