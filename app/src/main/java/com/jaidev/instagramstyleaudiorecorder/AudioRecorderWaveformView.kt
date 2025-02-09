package com.jaidev.instagramstyleaudiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.instagramstyleaudiorecorder.R


class AudioRecorderWaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr)
{

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val amplitudes = mutableListOf<Float>()
    private var barWidth = 8f
    private var barGap = 4f
    private var cornerRadius = 4f
    private var minBarHeightRatio = 0.15f // Minimum bar height as a ratio of view height
    private var maxBars: Int = 0
    private val barRect = RectF()
    private var isInitialized = false

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.AudioRecorderWaveformView, 0, 0
        )
        try {
            barWidth = typedArray.getDimension(
                R.styleable.AudioRecorderWaveformView_wave_width, barWidth
            )
            barGap = typedArray.getDimension(
                R.styleable.AudioRecorderWaveformView_wave_gap, barGap
            )
            cornerRadius = typedArray.getDimension(
                R.styleable.AudioRecorderWaveformView_wave_corner_radius, cornerRadius
            )
            wavePaint.color = typedArray.getColor(
                R.styleable.AudioRecorderWaveformView_wave_progress_color,
                ContextCompat.getColor(context, android.R.color.white)
            )
            minBarHeightRatio = typedArray.getFloat(
                R.styleable.AudioRecorderWaveformView_wave_min_height_ratio, minBarHeightRatio
            )
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Updates the waveform by adding a new amplitude value.
     */
    fun updateAmplitude(amplitude: Float) {
        if (!isInitialized) return
        if (amplitudes.size >= maxBars && amplitudes.isNotEmpty()) {
            amplitudes.removeAt(0) // Remove the oldest amplitude to keep the size constant
        }
        amplitudes.add(amplitude)
        invalidate() // Redraw the waveform
    }

    /**
     * Resets the waveform by clearing all stored amplitudes.
     */
    fun resetWaveform() {
        amplitudes.clear()
        invalidate() // Redraw the view
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxBars = calculateMaxBars()
        isInitialized = true
    }

    /**
     * Calculates the maximum number of bars that can fit within the available width.
     */
    private fun calculateMaxBars(): Int {
        // Calculate how many bars can fit in the view's width based on bar width and gap
        val totalBarWidth = barWidth + barGap
        return if (totalBarWidth > 0) {
            (width / totalBarWidth).toInt()
        } else {
            0
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInitialized) return

        val centerY = height / 2f
        val minHeight = height * minBarHeightRatio // Set minimum bar height based on ratio

        // Calculate the total width that will be used by the waveform
        val totalWidth = (maxBars * (barWidth + barGap))

        // Loop through the amplitudes and draw the bars
        for (i in amplitudes.indices) {
            val x =
                (i * (barWidth + barGap)) % totalWidth // This wraps the waveform back to the start when it exceeds totalWidth

            // Scale amplitude to fit view height
            var scaledHeight = (amplitudes[i] * height).coerceAtMost(height.toFloat())

            // Ensure a minimum height for silent parts
            if (scaledHeight < minHeight) {
                scaledHeight = minHeight
            }

            val top = centerY - (scaledHeight / 2)
            val bottom = centerY + (scaledHeight / 2)

            barRect.set(x, top, x + barWidth, bottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, wavePaint)
        }
    }
}
