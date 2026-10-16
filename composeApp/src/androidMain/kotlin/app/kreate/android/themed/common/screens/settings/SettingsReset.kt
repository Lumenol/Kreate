package app.kreate.android.themed.common.screens.settings

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.SettingEntrySearch
import app.kreate.android.themed.common.component.settings.entry
import it.fast4x.rimusic.ui.components.themed.ConfirmationDialog
import me.knighthat.component.dialog.RestartAppDialog

/**
 * "Reset this page" at the bottom of each settings tab, and "Reset everything" in
 * Miscellaneous.
 *
 * Only what the user can change on the page goes back to its default. Never touched:
 * the YouTube and Discord logins (credentials live in the encrypted store, and the two
 * "log in" switches are left out: resetting them hides the account without logging
 * out), the auto-backup folder (its storage permission would stay granted), and
 * everything in the database (playlists, history, downloads, statistics).
 *
 * Many settings only apply at startup (theme, caches, logs, DNS, player service), so
 * Kreate restarts afterwards, the same way as the debug settings, and opens again.
 */
object SettingsReset {

    val GENERAL: List<Preferences<*>> by lazy {
        listOf(
            Preferences.CHECK_UPDATE,
            Preferences.SHOW_CHECK_UPDATE_STATUS,
            Preferences.APP_LANGUAGE,
            Preferences.APP_REGION,
            Preferences.AUDIO_QUALITY,
            Preferences.DOWNLOAD_QUALITY,
            Preferences.IS_CONNECTION_METERED,
            Preferences.SMART_REWIND,
            Preferences.QUICK_PICKS_MIN_DURATION,
            Preferences.LIMIT_SONGS_WITH_DURATION,
            Preferences.PAUSE_BETWEEN_SONGS,
            Preferences.PAUSE_HISTORY,
            Preferences.PAUSE_WHEN_VOLUME_SET_TO_ZERO,
            Preferences.AUDIO_FADE_DURATION,
            Preferences.PLAYER_KEEP_MINIMIZED,
            Preferences.MINI_DISABLE_SWIPE_DOWN_TO_DISMISS,
            Preferences.QUEUE_AUTO_APPEND,
            Preferences.MAX_NUMBER_OF_SONG_IN_QUEUE,
            Preferences.ENABLE_DISCOVER,
            Preferences.SHOW_PLAYLIST_INDICATOR,
            Preferences.NOW_PLAYING_INDICATOR,
            Preferences.RESUME_PLAYBACK_WHEN_CONNECT_TO_AUDIO_DEVICE,
            Preferences.ENABLE_PERSISTENT_QUEUE,
            Preferences.RESUME_PLAYBACK_ON_STARTUP,
            Preferences.CLOSE_APP_ON_BACK,
            Preferences.PLAYBACK_SKIP_ON_ERROR,
            Preferences.AUDIO_SKIP_SILENCE,
            Preferences.AUDIO_VOLUME_NORMALIZATION,
            Preferences.AUDIO_VOLUME_NORMALIZATION_TARGET,
            Preferences.AUDIO_BASS_BOOSTED,
            Preferences.AUDIO_BASS_BOOST_LEVEL,
            Preferences.AUDIO_REVERB_PRESET,
            Preferences.AUDIO_SMART_PAUSE_DURING_CALLS,
            Preferences.AUDIO_VOLUME_BUTTONS_CHANGE_SONG,
            Preferences.AUDIO_SHAKE_TO_SKIP,
            Preferences.IS_PIP_ENABLED,
            Preferences.PIP_MODULE,
            Preferences.IS_AUTO_PIP_ENABLED,
            Preferences.AUTO_DOWNLOAD,
            Preferences.AUTO_DOWNLOAD_ON_LIKE,
            Preferences.AUTO_DOWNLOAD_ON_ALBUM_BOOKMARKED
        )
    }

    val UI: List<Preferences<*>> by lazy {
        listOf(
            Preferences.MAIN_THEME,
            Preferences.COLOR_PALETTE,
            Preferences.THEME_MODE,
            Preferences.CUSTOM_COLOR,
            Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_0,
            Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_1,
            Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_2,
            Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_3,
            Preferences.CUSTOM_LIGHT_THEME_BACKGROUND_4,
            Preferences.CUSTOM_LIGHT_TEXT,
            Preferences.CUSTOM_LIGHT_TEXT_SECONDARY,
            Preferences.CUSTOM_LIGHT_TEXT_DISABLED,
            Preferences.CUSTOM_LIGHT_PLAY_BUTTON,
            Preferences.CUSTOM_LIGHT_ACCENT,
            Preferences.CUSTOM_DARK_THEME_BACKGROUND_0,
            Preferences.CUSTOM_DARK_THEME_BACKGROUND_1,
            Preferences.CUSTOM_DARK_THEME_BACKGROUND_2,
            Preferences.CUSTOM_DARK_THEME_BACKGROUND_3,
            Preferences.CUSTOM_DARK_THEME_BACKGROUND_4,
            Preferences.CUSTOM_DARK_TEXT,
            Preferences.CUSTOM_DARK_TEXT_SECONDARY,
            Preferences.CUSTOM_DARK_TEXT_DISABLED,
            Preferences.CUSTOM_DARK_PLAY_BUTTON,
            Preferences.CUSTOM_DARK_ACCENT,
            Preferences.NAVIGATION_BAR_POSITION,
            Preferences.NAVIGATION_BAR_TYPE,
            Preferences.MINI_PLAYER_POSITION,
            Preferences.MENU_STYLE,
            Preferences.STARTUP_SCREEN,
            Preferences.TRANSITION_EFFECT,
            Preferences.SHOW_SEARCH_IN_NAVIGATION_BAR,
            Preferences.SHOW_STATS_IN_NAVIGATION_BAR,
            Preferences.SHOW_FLOATING_ICON,
            Preferences.FONT,
            Preferences.USE_SYSTEM_FONT,
            Preferences.APPLY_FONT_PADDING,
            Preferences.ENABLE_SWIPE_ACTION,
            Preferences.QUEUE_SWIPE_LEFT_ACTION,
            Preferences.QUEUE_SWIPE_RIGHT_ACTION,
            Preferences.PLAYLIST_SWIPE_LEFT_ACTION,
            Preferences.PLAYLIST_SWIPE_RIGHT_ACTION,
            Preferences.ALBUM_SWIPE_LEFT_ACTION,
            Preferences.ALBUM_SWIPE_RIGHT_ACTION,
            Preferences.HOME_SONGS_SHOW_FAVORITES_CHIP,
            Preferences.HOME_SONGS_SHOW_CACHED_CHIP,
            Preferences.HOME_SONGS_SHOW_DOWNLOADED_CHIP,
            Preferences.HOME_SONGS_SHOW_MOST_PLAYED_CHIP,
            Preferences.HOME_SONGS_SHOW_ON_DEVICE_CHIP,
            Preferences.SONG_EMPTY_DURATION_PLACEHOLDER,
            Preferences.SONG_THUMBNAIL_ROUNDNESS_PERCENT,
            Preferences.SONG_THUMBNAIL_SIZE,
            Preferences.SHOW_PINNED_PLAYLISTS,
            Preferences.MONTHLY_PLAYLIST_COMPILATION,
            Preferences.SHOW_MONTHLY_PLAYLISTS,
            Preferences.PLAYLISTS_PLATFORM_INDICATOR,
            Preferences.PLAYLIST_THUMBNAIL_ROUNDNESS_PERCENT,
            Preferences.PLAYLIST_THUMBNAIL_SIZE,
            Preferences.ALBUMS_PLATFORM_INDICATOR,
            Preferences.ALBUM_THUMBNAIL_ROUNDNESS_PERCENT,
            Preferences.ALBUM_THUMBNAIL_SIZE,
            Preferences.ARTISTS_PLATFORM_INDICATOR,
            Preferences.ARTIST_THUMBNAIL_ROUNDNESS_PERCENT,
            Preferences.ARTIST_THUMBNAIL_SIZE,
            Preferences.MAX_NUMBER_OF_SMART_RECOMMENDATIONS,
            Preferences.MAX_NUMBER_OF_STATISTIC_ITEMS,
            Preferences.SHOW_LISTENING_STATS,
            Preferences.MAX_NUMBER_OF_TOP_PLAYED,
            Preferences.SINGLE_BACK_FROM_SEARCH
        )
    }

    val PLAYER: List<Preferences<*>> by lazy {
        listOf(
            Preferences.PLAYER_SHOW_TOP_ACTIONS_BAR,
            Preferences.PLAYER_TOP_PADDING,
            Preferences.PLAYER_TYPE,
            Preferences.QUEUE_TYPE,
            Preferences.PLAYER_SHOW_THUMBNAIL,
            Preferences.PLAYER_NO_THUMBNAIL_SWIPE_ANIMATION,
            Preferences.PLAYER_BACKGROUND_FADING_EDGE,
            Preferences.PLAYER_THUMBNAILS_CAROUSEL,
            Preferences.CAROUSEL_SIZE,
            Preferences.PLAYER_SHRINK_THUMBNAIL_ON_PAUSE,
            Preferences.LYRICS_SHOW_THUMBNAIL,
            Preferences.PLAYER_SHOW_THUMBNAIL_ON_VISUALIZER,
            Preferences.PLAYER_THUMBNAIL_ANIMATION,
            Preferences.PLAYER_THUMBNAIL_TYPE,
            Preferences.PLAYER_LANDSCAPE_THUMBNAIL_SIZE,
            Preferences.PLAYER_PORTRAIT_THUMBNAIL_SIZE,
            Preferences.THUMBNAIL_TYPE,
            Preferences.THUMBNAIL_BORDER_RADIUS,
            Preferences.PLAYER_BACKGROUND_BLUR,
            Preferences.PLAYER_STATS_FOR_NERDS,
            Preferences.PLAYER_TIMELINE_SIZE,
            Preferences.PLAYER_INFO_TYPE,
            Preferences.PLAYER_SONG_INFO_ICON,
            Preferences.MINI_PLAYER_TYPE,
            Preferences.PLAYER_IS_CONTROL_AND_TIMELINE_SWAPPED,
            Preferences.PLAYER_TIMELINE_TYPE,
            Preferences.TRANSPARENT_TIMELINE,
            Preferences.PLAYER_CONTROLS_TYPE,
            Preferences.PLAYER_PLAY_BUTTON_TYPE,
            Preferences.ZOOM_OUT_ANIMATION,
            Preferences.LIKE_ICON,
            Preferences.PLAYER_BACKGROUND,
            Preferences.ANIMATED_GRADIENT,
            Preferences.PLAYER_ROTATING_ALBUM_COVER,
            Preferences.PLAYER_BOTTOM_GRADIENT,
            Preferences.PLAYER_THUMBNAIL_ROTATION,
            Preferences.BLACK_GRADIENT,
            Preferences.TEXT_OUTLINE,
            Preferences.PLAYER_SHOW_TOTAL_QUEUE_TIME,
            Preferences.PLAYER_SHOW_SONGS_REMAINING_TIME,
            Preferences.PLAYER_SHOW_SEEK_BUTTONS,
            Preferences.PLAYER_SHOW_NEXT_IN_QUEUE,
            Preferences.MAX_NUMBER_OF_NEXT_IN_QUEUE,
            Preferences.PLAYER_SHOW_NEXT_IN_QUEUE_THUMBNAIL,
            Preferences.MARQUEE_TEXT_EFFECT,
            Preferences.PLAYER_THUMBNAIL_HORIZONTAL_SWIPE_DISABLED,
            Preferences.ROTATION_EFFECT,
            Preferences.PLAYER_TAP_THUMBNAIL_FOR_LYRICS,
            Preferences.LYRICS_JUMP_ON_TAP,
            Preferences.LYRICS_SHOW_ACCENT_BACKGROUND,
            Preferences.PLAYER_ACTION_LYRICS_POPUP_MESSAGE,
            Preferences.MINI_PLAYER_PROGRESS_BAR,
            Preferences.PLAYER_VISUALIZER,
            Preferences.PLAYER_ACTION_TABS,
            Preferences.PLAYER_TRANSPARENT_ACTIONS_BAR,
            Preferences.PLAYER_ACTION_BUTTONS_SPACED_EVENLY,
            Preferences.PLAYER_ACTIONS_BAR_TAP_TO_OPEN_QUEUE,
            Preferences.PLAYER_ACTIONS_BAR_SWIPE_UP_TO_OPEN_QUEUE,
            Preferences.PLAYER_ACTION_TOGGLE_VIDEO,
            Preferences.PLAYER_ACTION_DISCOVER,
            Preferences.PLAYER_ACTION_DOWNLOAD,
            Preferences.PLAYER_ACTION_ADD_TO_PLAYLIST,
            Preferences.PLAYER_ACTION_LOOP,
            Preferences.PLAYER_ACTION_SHUFFLE,
            Preferences.PLAYER_ACTION_SHOW_LYRICS,
            Preferences.PLAYER_ACTION_TOGGLE_EXPAND,
            Preferences.PLAYER_ACTION_SLEEP_TIMER,
            Preferences.PLAYER_ACTION_OPEN_EQUALIZER,
            Preferences.PLAYER_ACTION_OPEN_QUEUE_ARROW,
            Preferences.PLAYER_ACTION_START_RADIO,
            Preferences.PLAYER_ACTION_SHOW_MENU,
            Preferences.AUDIO_SPEED,
            Preferences.PLAYER_IS_QUEUE_DURATION_EXPANDED,
            Preferences.PLAYER_IS_TITLE_EXPANDED,
            Preferences.PLAYER_IS_TIMELINE_EXPANDED,
            Preferences.PLAYER_IS_CONTROLS_EXPANDED,
            Preferences.PLAYER_IS_STATS_FOR_NERDS_EXPANDED,
            Preferences.PLAYER_IS_ACTIONS_BAR_EXPANDED,
            Preferences.PLAYER_IS_NEXT_IN_QUEUE_EXPANDED,
            Preferences.MEDIA_NOTIFICATION_FIRST_ICON,
            Preferences.MEDIA_NOTIFICATION_SECOND_ICON,
            Preferences.LIVE_WALLPAPER,
            Preferences.LIVE_WALLPAPER_RESET_DURATION,
            Preferences.PLAYER_EXPANDED,
            Preferences.PLAYER_BACKGROUND_BLUR_STRENGTH,
            Preferences.PLAYER_THUMBNAIL_FADE,
            Preferences.PLAYER_THUMBNAIL_FADE_EX,
            Preferences.PLAYER_THUMBNAIL_SPACING
        )
    }

    val HOME: List<Preferences<*>> by lazy {
        listOf(
            Preferences.QUICK_PICKS_PAGE,
            Preferences.QUICK_PICKS_SHOW_TIPS,
            Preferences.QUICK_PICKS_TYPE,
            Preferences.QUICK_PICKS_SHOW_CHARTS,
            Preferences.QUICK_PICKS_SHOW_RELATED_ALBUMS,
            Preferences.QUICK_PICKS_SHOW_RELATED_ARTISTS,
            Preferences.QUICK_PICKS_SHOW_NEW_ALBUMS_ARTISTS,
            Preferences.QUICK_PICKS_SHOW_NEW_ALBUMS,
            Preferences.QUICK_PICKS_SHOW_MIGHT_LIKE_PLAYLISTS,
            Preferences.QUICK_PICKS_SHOW_MOODS_AND_GENRES,
            Preferences.QUICK_PICKS_SHOW_MONTHLY_PLAYLISTS
        )
    }

    val DATA: List<Preferences<*>> by lazy {
        listOf(
            Preferences.IMAGE_CACHE_SIZE,
            Preferences.EXO_CACHE_SIZE,
            Preferences.EXO_DOWNLOAD_SIZE,
            Preferences.EXO_CACHE_LOCATION,
            Preferences.AUTO_BACKUP,
            Preferences.AUTO_BACKUP_INTERVAL,
            Preferences.AUTO_BACKUP_KEEP,
            Preferences.AUTO_BACKUP_DATABASE,
            Preferences.AUTO_BACKUP_HISTORY,
            Preferences.AUTO_BACKUP_SETTINGS,
            Preferences.PAUSE_SEARCH_HISTORY
        )
    }

    val ACCOUNTS: List<Preferences<*>> by lazy {
        listOf(
            Preferences.YOUTUBE_PLAYLISTS_SYNC,
            Preferences.YOUTUBE_ARTISTS_SYNC,
            Preferences.YOUTUBE_ALBUMS_SYNC,
            Preferences.YOUTUBE_REPORT_PLAYBACK
        )
    }

    val OTHER: List<Preferences<*>> by lazy {
        listOf(
            Preferences.HOME_SONGS_ON_DEVICE_SHOW_FOLDERS,
            Preferences.LOCAL_SONGS_FOLDER,
            Preferences.PLAYER_EXTRA_SPACE,
            Preferences.KEEP_SCREEN_ON,
            Preferences.PARENTAL_CONTROL,
            Preferences.RUNTIME_LOG,
            Preferences.RUNTIME_LOG_SHARED,
            Preferences.RUNTIME_LOG_SEVERITY,
            Preferences.RUNTIME_LOG_FILE_COUNT,
            Preferences.RUNTIME_LOG_MAX_SIZE_PER_FILE
        )
    }

    val NETWORK: List<Preferences<*>> by lazy {
        listOf(
            Preferences.IS_PROXY_ENABLED,
            Preferences.PROXY_SCHEME,
            Preferences.PROXY_HOST,
            Preferences.PROXY_PORT,
            Preferences.DOH_SERVER
        )
    }
    val ALL: List<Preferences<*>> by lazy {
        GENERAL + UI + PLAYER + HOME + DATA + ACCOUNTS + OTHER + NETWORK
    }

    fun reset( context: Context, preferences: List<Preferences<*>> ) {
        if( Preferences.APP_LANGUAGE in preferences )
            AppCompatDelegate.setApplicationLocales( LocaleListCompat.getEmptyLocaleList() )

        preferences.forEach { it.reset() }
        // Values are saved in the background: write them now, the process is about to end
        Preferences.preferences.edit().commit()

        // Open Kreate again once it's closed, then close it like the debug settings do
        context.packageManager
               .getLaunchIntentForPackage( context.packageName )
               ?.let { Intent.makeRestartActivityTask( it.component ) }
               ?.also( context::startActivity )
        RestartAppDialog.onConfirm()
    }
}

/** Last entry of a settings tab: puts [preferences] back to their defaults, after a confirmation */
fun LazyListScope.resetEntry(
    search: SettingEntrySearch,
    pageTitleId: Int,
    preferences: () -> List<Preferences<*>>,
    everything: Boolean = false
) {
    val titleId = if( everything ) R.string.setting_entry_reset_all else R.string.setting_entry_reset_page
    entry( search, titleId ) {
        val context = LocalContext.current
        var showDialog by rememberSaveable { mutableStateOf( false ) }
        val pageTitle = stringResource( pageTitleId )

        if( showDialog )
            ConfirmationDialog(
                text = if( everything )
                    stringResource( R.string.setting_confirm_reset_all, preferences().size )
                else
                    stringResource( R.string.setting_confirm_reset_page, preferences().size, pageTitle ),
                onDismiss = { showDialog = false },
                onConfirm = {
                    showDialog = false
                    SettingsReset.reset( context, preferences() )
                }
            )

        SettingComponents.Text(
            title = stringResource( titleId ),
            subtitle = if( everything )
                stringResource( R.string.setting_description_reset_all )
            else
                stringResource( R.string.setting_description_reset_page, pageTitle ),
            onClick = { showDialog = true }
        )
    }
}
