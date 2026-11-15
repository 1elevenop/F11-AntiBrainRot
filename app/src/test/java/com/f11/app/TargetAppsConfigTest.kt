package com.f11.app

import com.f11.app.data.TargetAppsConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** View ids below were captured from a real device (Instagram, Nov 2026). */
class TargetAppsConfigTest {

    private val instagram = "com.instagram.android"

    @Test
    fun instagramReelsViewerCounts() {
        assertTrue(
            TargetAppsConfig.isShortFormSurface(
                instagram,
                listOf(
                    "androidx.viewpager.widget.ViewPager",
                    "com.instagram.android:id/clips_viewer_view_pager",
                    "com.instagram.android:id/swipeable_tab_view_pager",
                ),
            ),
        )
    }

    @Test
    fun instagramFeedAndDmListsDoNotCount() {
        assertFalse(TargetAppsConfig.isShortFormSurface(instagram, listOf("androidx.recyclerview.widget.RecyclerView")))
        assertFalse(
            TargetAppsConfig.isShortFormSurface(
                instagram,
                listOf("androidx.recyclerview.widget.RecyclerView", "android:id/list", null,
                    "com.instagram.android:id/swipeable_tab_view_pager"),
            ),
        )
    }

    @Test
    fun instagramStoriesDoNotCount() {
        // Instagram names its Stories viewer "reel_viewer" internally.
        assertFalse(
            TargetAppsConfig.isShortFormSurface(instagram, listOf("com.instagram.android:id/reel_viewer_root")),
        )
    }

    @Test
    fun feedOnlyAppsAlwaysCount() {
        assertTrue(TargetAppsConfig.isShortFormSurface("com.zhiliaoapp.musically", emptyList()))
    }

    @Test
    fun unknownAppsNeverCount() {
        assertFalse(TargetAppsConfig.isShortFormSurface("com.whatsapp", listOf("clips_viewer")))
        assertFalse(TargetAppsConfig.isShortFormSurface(null, listOf("clips_viewer")))
    }
}
