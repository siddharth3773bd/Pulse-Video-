package com.example.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
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

        // "X" button shown in the media notification / quick-settings media card / live alert.
        val stopButton = CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(R.drawable.ic_stop_playback)
            .setSessionCommand(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
            .build()

        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .setCustomLayout(ImmutableList.of(stopButton))
            .build()
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
    }
}
