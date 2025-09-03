
package it.chiarani.utils

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import kotlin.math.max
import kotlin.math.min

/**
 * [VideoTextureView] is a [TextureView] wrapper around [MediaPlayer] that
 * automatically applies a transform matrix to center the video content
 * inside the view.
 *
 * Features:
 * - Bind video from [Uri].
 * - Play, pause, mute/unmute, enable/disable looping.
 * - Safe resource release on detach.
 * - Auto center video with optional [ScaleMode] (FIT or CROP).
 *
 * Typical usage in Compose:
 * ```
 * AndroidView(factory = { context ->
 *     VideoTextureView(context).apply {
 *         bindUrl(myUri)
 *     }
 * })
 * ```
 */
@Suppress("TooManyFunctions")
class VideoTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : TextureView(context, attrs), TextureView.SurfaceTextureListener {

    /**
     * Defines how the video content should be scaled inside the view.
     * - [FIT]: preserve aspect ratio, video fully visible (letterbox if needed).
     * - [CROP]: preserve aspect ratio, video fills the view (cropped if needed).
     */
    enum class ScaleMode { FIT, CROP }

    /** Current scaling mode (default = [FIT]). */
    var scaleMode: ScaleMode = ScaleMode.FIT
        set(value) {
            field = value
            applyTransform()
        }

    private var mediaPlayer: MediaPlayer? = null
    private var surface: Surface? = null
    private var pendingUri: Uri? = null

    private var isPrepared = false
    private var isMuted = true
    private var isLoopingEnabled = true
    private var shouldPlayWhenReady = true

    private var videoWidth = 0
    private var videoHeight = 0

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    /** Bind a video [Uri]. The video will auto-play if [playWhenReady] is true. */
    fun bindUrl(uri: Uri, playWhenReady: Boolean = true) {
        pendingUri = uri
        shouldPlayWhenReady = playWhenReady
        if (surface != null) preparePlayer()
    }

    /** Mute or unmute playback. */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        val v = if (muted) 0f else 1f
        mediaPlayer?.setVolume(v, v)
    }

    /** Enable or disable looping playback. */
    fun setLooping(looping: Boolean) {
        isLoopingEnabled = looping
        mediaPlayer?.isLooping = looping
    }

    /** Start playback if prepared. */
    fun play() {
        shouldPlayWhenReady = true
        if (isPrepared) mediaPlayer?.start()
    }

    /** Pause playback if active. */
    fun pause() {
        shouldPlayWhenReady = false
        mediaPlayer?.pause()
    }

    /** Release all resources. Should be called when the view is removed. */
    fun release() {
        isPrepared = false
        mediaPlayer?.apply {
            runCatching { setSurface(null) }
            runCatching { reset() }
            runCatching { release() }
        }
        mediaPlayer = null
        surface?.release()
        surface = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(st)
        applyTransform()
        if (pendingUri != null) preparePlayer()
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
        applyTransform()
    }

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit

    /** Returns true when MediaPlayer is prepared and ready. */
    fun isReady(): Boolean = isPrepared

    /** Convenience to pause/resume with a boolean. */
    fun setPaused(paused: Boolean) {
        if (paused) pause() else play()
    }

    private fun preparePlayer() {
        val uri = pendingUri ?: return
        releasePlayerOnly()

        val currentSurface = surface ?: return
        val mp = MediaPlayer().also { mediaPlayer = it }

        mp.setSurface(currentSurface)
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
        )
        mp.isLooping = isLoopingEnabled

        mp.setOnPreparedListener {
            isPrepared = true
            setMuted(isMuted)
            applyTransform()
            if (shouldPlayWhenReady) mp.start()
        }
        mp.setOnVideoSizeChangedListener { _, w, h ->
            videoWidth = w
            videoHeight = h
            applyTransform()
        }
        mp.setOnErrorListener { _, _, _ -> false }

        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } ?: mp.setDataSource(context, uri)
            mp.prepareAsync()
            mp.setOnCompletionListener { onCompleted?.invoke() }
        }.onFailure {
            runCatching {
                mp.reset()
                mp.setSurface(currentSurface)
                mp.setDataSource(context, uri)
                mp.prepareAsync()
            }
        }
    }

    private fun releasePlayerOnly() {
        isPrepared = false
        mediaPlayer?.apply {
            runCatching { setSurface(null) }
            runCatching { reset() }
            runCatching { release() }
        }
        mediaPlayer = null
    }

    private var onCompleted: (() -> Unit)? = null

    /** Current playback position in ms, or null if not ready. */
    fun currentPositionMs(): Int? = mediaPlayer?.takeIf { isPrepared }?.currentPosition

    /** Duration in ms, or null if not ready. */
    fun durationMs(): Int? = mediaPlayer?.takeIf { isPrepared }?.duration

    /** Set completion callback (called when video ends and non-looping). */
    fun setOnCompletedListener(listener: (() -> Unit)?) {
        onCompleted = listener
        mediaPlayer?.setOnCompletionListener { onCompleted?.invoke() }
    }

    /**
     * Apply scaling/centering transform matrix:
     * - Uses [videoWidth] / [videoHeight] and view [width] / [height].
     * - Preserves aspect ratio based on [scaleMode].
     * - Always centers video content inside the view.
     */
    @Suppress("ComplexCondition")
    private fun applyTransform() {
        val vw = videoWidth
        val vh = videoHeight
        val w = width
        val h = height
        if (vw <= 0 || vh <= 0 || w <= 0 || h <= 0) return

        val sx = vw.toFloat() / w.toFloat()
        val sy = vh.toFloat() / h.toFloat()
        val tx = (w - w * sx) / 2f
        val ty = (h - h * sy) / 2f

        val m = Matrix().apply {
            setScale(sx, sy)
            postTranslate(tx, ty)
        }
        setTransform(m)
        invalidate()
    }
}
