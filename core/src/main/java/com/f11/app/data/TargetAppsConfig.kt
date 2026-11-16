package com.f11.app.data

/**
 * A short-form video app F11 guards.
 *
 * @param feedOnly true when the whole app is a short-form feed (every scroll counts).
 * @param surfaceHints lowercase substrings matched against the scrolled view's resource id or
 *        class name to tell the short-form surface (Reels, Shorts...) apart from the rest of the app.
 * @param feedViewId full resource id of the short-form feed's paging list, used to tell whether
 *        the feed is still on screen.
 * @param pollFeed true for feeds that hide their scrolling from accessibility (no scroll events);
 *        F11 then watches which page of [feedViewId] is on screen instead.
 */
data class TargetApp(
    val packageName: String,
    val displayName: String,
    val surfaceName: String,
    val feedOnly: Boolean = false,
    val surfaceHints: List<String> = emptyList(),
    val feedViewId: String? = null,
    val pollFeed: Boolean = false,
)

object TargetAppsConfig {

    val apps: List<TargetApp> = listOf(
        TargetApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            surfaceName = "Reels",
            // Instagram calls Stories "reels" internally, so a bare "reel" would catch Stories.
            surfaceHints = listOf("clips_viewer", "clips_video"),
            feedViewId = "com.instagram.android:id/clips_viewer_view_pager",
        ),
        TargetApp(
            packageName = "com.google.android.youtube",
            displayName = "YouTube",
            surfaceName = "Shorts",
            surfaceHints = listOf("reel_recycler", "reel_watch", "reel_player", "shorts_player"),
            // Shorts' list reports scrollable=false, so it never sends scroll events.
            feedViewId = "com.google.android.youtube:id/reel_recycler",
            pollFeed = true,
        ),
        TargetApp(
            packageName = "com.zhiliaoapp.musically",
            displayName = "TikTok",
            surfaceName = "For You",
            feedOnly = true,
        ),
        TargetApp(
            packageName = "com.ss.android.ugc.trill",
            displayName = "TikTok (Asia)",
            surfaceName = "For You",
            feedOnly = true,
        ),
        TargetApp(
            packageName = "com.facebook.katana",
            displayName = "Facebook",
            surfaceName = "Reels",
            surfaceHints = listOf("reels_viewer", "fb_shorts", "reels_tab"),
        ),
        TargetApp(
            packageName = "com.snapchat.android",
            displayName = "Snapchat",
            surfaceName = "Spotlight",
            surfaceHints = listOf("spotlight"),
        ),
    )

    private val byPackage: Map<String, TargetApp> = apps.associateBy { it.packageName }

    fun find(packageName: String?): TargetApp? = packageName?.let(byPackage::get)

    fun isTarget(packageName: String?): Boolean = find(packageName) != null

    /**
     * True when a scroll from [packageName] on a view identified by [viewIds] (resource ids or
     * class names of the scrolled node and its ancestors) is a short-form feed swipe.
     */
    fun isShortFormSurface(packageName: String?, viewIds: List<String?>): Boolean {
        val app = find(packageName) ?: return false
        if (app.feedOnly) return true
        return viewIds.any { id ->
            val normalized = id?.lowercase() ?: return@any false
            app.surfaceHints.any(normalized::contains)
        }
    }
}
