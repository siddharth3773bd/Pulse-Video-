package com.example.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AudioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
                .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_STOP_PLAYBACK) {
                stopPlaybackAndService()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == ACTION_TOGGLE_SHUFFLE) {
                AudioPlaybackManager.getInstance(this@AudioPlaybackService).toggleShuffle()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // WAV files with a slightly wrong header still play (see WavHeaderFixingDataSource).
        val mediaSourceFactory = DefaultMediaSourceFactory(
            WavHeaderFixingDataSource.Factory(DefaultDataSource.Factory(this))
        )
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        player = exoPlayer
        val shuffleNow = AudioPlaybackManager.existingInstance()?.state?.value?.shuffleEnabled == true
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(extraButtons(shuffleNow))
            .build()
        // The shuffle button follows the app's shuffle (on/off icon).
        val manager = AudioPlaybackManager.existingInstance()
        if (manager != null) {
            serviceScope.launch {
                manager.state
                    .map { it.shuffleEnabled }
                    .distinctUntilChanged()
                    .collect { on -> mediaSession?.setMediaButtonPreferences(extraButtons(on)) }
            }
        }
    }

    private val serviceScope = MainScope()

    /**
     * The two extra buttons in the system media player and notification, in this order: Shuffle,
     * then ✕ (stop). Phones put the first extra button before ⏮ and the second after ⏭, so ✕
     * ends up on the right.
     */
    private fun extraButtons(shuffleOn: Boolean): ImmutableList<CommandButton> {
        val shuffle = CommandButton.Builder(
            if (shuffleOn) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
        )
            .setDisplayName(if (shuffleOn) "Shuffle on" else "Shuffle off")
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
            .build()
        val stop = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Stop")
            .setCustomIconResId(R.drawable.ic_stop_playback)
            .setSessionCommand(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
            .build()
        return ImmutableList.of(shuffle, stop)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /** Stops playback, clears the queue, removes the notification and stops the service. */
    private fun stopPlaybackAndService() {
        mediaSession?.player?.let { p ->
            p.pause()
            p.stop()
            p.clearMediaItems()
        }
        try {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        pauseAllPlayersAndStopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player
        if (currentPlayer == null || !currentPlayer.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP_PLAYBACK = "com.example.player.STOP_PLAYBACK"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.player.TOGGLE_SHUFFLE"
    }
}
