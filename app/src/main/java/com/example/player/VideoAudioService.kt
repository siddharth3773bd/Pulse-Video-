package com.example.player

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
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

/**
 * Keeps a video's sound playing after leaving the app ("Audio only" in the video player).
 *
 * The video player's own ExoPlayer is shared with this service through a Media3 MediaSession, so
 * nothing reloads: Android sees a normal media app (notification, lock-screen controls) and keeps
 * the process alive. Tapping the notification brings the app back to the same player.
 */
@OptIn(UnstableApi::class)
class VideoAudioService : MediaSessionService() {

    private var session: MediaSession? = null

    private val callback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_STOP, Bundle.EMPTY))
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
            if (customCommand.customAction == ACTION_STOP) {
                session.player.pause()
                stopSelf()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasSession = ensureSession()
        // Media3 goes first: it also answers start requests left over from an instance that was
        // already stopped (they arrive after the player closed), the safe way.
        super.onStartCommand(intent, flags, startId)
        if (!hasSession) {
            // Nothing to play any more. Android may have been promised a foreground service by
            // that late start request, so keep the promise for a moment before stopping;
            // stopping without it is what crashed the app ("did not call startForeground").
            stopKeepingPromise()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InlinedApi")
    private fun stopKeepingPromise() {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Video sound", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stop_playback)
                .setContentTitle("Stopping")
                .setSilent(true)
                .build()
            ServiceCompat.startForeground(
                this,
                STOP_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
            // Not allowed from the background (Android 12+): then no promise was made either.
        } finally {
            stopSelf()
        }
    }

    /** Ends background sound: the session goes first (no more notification updates), then the service. */
    private fun shutdown() {
        session?.let { s ->
            removeSession(s)
            s.release()
        }
        session = null
        stopSelf()
    }

    private fun ensureSession(): Boolean {
        if (session != null) return true
        val p = sharedPlayer ?: return false
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopButton = CommandButton.Builder()
            .setDisplayName("Stop")
            .setIconResId(R.drawable.ic_stop_playback)
            .setSessionCommand(SessionCommand(ACTION_STOP, Bundle.EMPTY))
            .build()
        val s = MediaSession.Builder(this, p)
            .setId(SESSION_ID)
            .setSessionActivity(openApp)
            .setCallback(callback)
            .setCustomLayout(ImmutableList.of(stopButton))
            .build()
        session = s
        addSession(s)
        return true
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away from recents stops the sound too.
        session?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        session?.let { s ->
            removeSession(s)
            s.release() // the player belongs to the video screen; it is not released here
        }
        session = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "app.doppler.player.VIDEO_AUDIO_STOP"
        private const val SESSION_ID = "doppler_video_audio"
        private const val CHANNEL_ID = "doppler_video_audio_stop"
        private const val STOP_NOTIFICATION_ID = 7341

        private var sharedPlayer: Player? = null
        private var instance: VideoAudioService? = null

        val isActive: Boolean get() = sharedPlayer != null

        /** Starts background audio for [player]. Called while the app is still on screen. */
        fun start(context: Context, player: Player) {
            sharedPlayer = player
            val running = instance
            if (running?.session != null) {
                running.session?.player = player
                return
            }
            runCatching {
                context.startService(Intent(context, VideoAudioService::class.java))
            }
        }

        /** The video screen made a new player (e.g. another decoder): hand it over. */
        fun swapPlayer(player: Player) {
            if (sharedPlayer == null) return
            sharedPlayer = player
            instance?.session?.player = player
        }

        fun stop(context: Context) {
            sharedPlayer = null
            val running = instance
            if (running != null) {
                running.shutdown()
            } else {
                runCatching { context.stopService(Intent(context, VideoAudioService::class.java)) }
            }
        }
    }
}
