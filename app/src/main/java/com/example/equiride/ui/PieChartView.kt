package com.example.equiride.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var data: Map<String, Double> = emptyMap()

    /** Zavolej pro nastavení nových dat */
    fun setData(data: Map<String, Double>) {
        this.data = data
        invalidate()
    }

    /** Vyčistí data */
    fun clearData() {
        this.data = emptyMap()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        // spočítat celkový součet
        val total = data.values.sum().takeIf { it > 0 } ?: return

        // příprava pro kreslení
        val padding = 16f
        rect.set(padding, padding, width - padding, height - padding)

        var startAngle = -90f
        data.entries.forEach { (label, value) ->
            val sweep = (value / total * 360f).toFloat()
            paint.style = Paint.Style.FILL
            paint.color = randomColor(label.hashCode())  // barvu si určíme hashem
            canvas.drawArc(rect, startAngle, sweep, true, paint)
            startAngle += sweep
        }
    }

    private fun randomColor(seed: Int): Int {
        val r = (0x80 + (seed and 0x7F)) and 0xFF
        val g = (0x80 + ((seed shr 8) and 0x7F)) and 0xFF
        val b = (0x80 + ((seed shr 16) and 0x7F)) and 0xFF
        return 0xFF shl 24 or (r shl 16) or (g shl 8) or b
    }
}
