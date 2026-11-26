package com.movementlab.app.data

/**
 * Default demo video (YouTube video ID) for each built-in exercise. They play
 * inside the app. If one ever gets taken down, the player shows a "Find
 * another video" button, and any exercise's video can be swapped from its
 * screen (pencil icon) by pasting a different YouTube link.
 */
object Videos {
    val defaults: Map<String, String> = mapOf(
        // Push-up ladder
        "wall_pushup" to "lIRpibTFB9I",
        "incline_pushup" to "Gvm5Q29UHbk",
        "knee_pushup" to "lFR1GWy1Dcs",
        "pushup" to "IODxDxX7oi4",
        "diamond_pushup" to "_4EGPVJuqfA",
        "archer_pushup" to "91e47oNbSdA",
        "one_arm_pushup" to "xp1tgjT_3k0",
        // Pull-up ladder
        "dead_hang" to "PlAE67ovNEo",
        "scap_pull" to "-ZIpSoTRsuE",
        "australian_row" to "dnpDUwqMX04",
        "negative_pullup" to "R4XbkMXGXaI",
        "chinup" to "qV7vOUcUfD4",
        "pullup" to "iBtL9nX2qOs",
        "weighted_pullup" to "XxPnmbSBSZo",
        "archer_pullup" to "_LGLKUiQH5k",
        // Dips
        "bench_dip" to "WVeZDBhZwLA",
        "negative_dip" to "hsEKg3M45dc",
        "dip" to "XqBuu8Qp4mA",
        "weighted_dip" to "OH5iSZRzkso",
        // Muscle-up
        "explosive_pullup" to "f69g4QA0aUo",
        "straight_bar_dip" to "TSQP6rZqjiM",
        "muscle_up" to "F-rxP91Ymmo",
        // Squat ladder
        "assisted_squat" to "SoZJR09o2nU",
        "squat" to "ZLJBfYF_oO0",
        "split_squat" to "4aqrcebiFZo",
        "bulgarian_split_squat" to "oGTVhFgRnes",
        "shrimp_squat" to "_FBuC-VPbRY",
        "pistol_squat" to "hHxm3VbuS-w",
        // Hinge
        "glute_bridge" to "KI6fjDqGZh4",
        "single_leg_bridge" to "kZwLhgwfkwc",
        "nordic_negative" to "ie7yQ5k5ZyU",
        // Core
        "plank" to "6LqqeBtFn9M",
        "hollow_hold" to "HAfUt2Cco74",
        "leg_raise" to "3oIpxsn6FxQ",
        "hanging_knee_raise" to "KhPTiWP6lB4",
        "hanging_leg_raise" to "Pr1ieGZ5atk",
        "toes_to_bar" to "UdJYY1Ilk_o",
        "tuck_lsit" to "NnT-GIxd3O4",
        "lsit" to "YtvZf1N1qVQ",
        // Handstand
        "pike_pushup" to "pHR5yG6xBps",
        "elevated_pike_pushup" to "GoMrYWx6Cy8",
        "wall_handstand" to "udZZb1mif_I",
        "wall_hspu" to "DBv3BvBS4H8",
        // Front lever
        "tuck_front_lever" to "0yY1m3PDbok",
        "adv_tuck_front_lever" to "D-dOt8Ey5Ig",
        // Mobility
        "arm_circles" to "tYo5ghpLksg",
        "cat_cow" to "5KYLaU6Lg2E",
        "deep_squat_hold" to "0wzrgyAurT8",
        "shoulder_dislocates" to "6AZ6lW2TNGM",
        "hip_flexor_stretch" to "F55tzqJggAY",
    )

    fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"

    private val idPattern = Regex("^[A-Za-z0-9_-]{11}$")

    /**
     * Pulls the 11-character video ID out of any common YouTube link:
     * watch?v=, youtu.be/, /shorts/, /embed/, /live/, m.youtube.com, etc.
     * Returns null for search pages and non-YouTube links.
     */
    fun youtubeId(url: String): String? {
        val u = url.trim()
        if (idPattern.matches(u)) return u
        val patterns = listOf(
            Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
            Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""youtube(?:-nocookie)?\.com/(?:shorts|embed|live|v)/([A-Za-z0-9_-]{11})"""),
        )
        if (!u.contains("youtu")) return null
        return patterns.firstNotNullOfOrNull { it.find(u)?.groupValues?.get(1) }
    }
}
