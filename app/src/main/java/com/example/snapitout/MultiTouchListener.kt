package com.example.snapitout

import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlin.math.atan2
import kotlin.math.sqrt

class MultiTouchListener : View.OnTouchListener {

    private var mode = NONE

    private val lastEvent = FloatArray(4)
    private var oldDist = 1f
    private var rotation = 0f

    private val start = PointF()
    private val mid = PointF()
    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    override fun onTouch(view: View, event: MotionEvent): Boolean {

        // IMPORTANT FIX — CAST TO IMAGEVIEW
        val image = view as? ImageView ?: return true

        when (event.action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }

                lastEvent[0] = event.getX(0)
                lastEvent[1] = event.getX(1)
                lastEvent[2] = event.getY(0)
                lastEvent[3] = event.getY(1)
                rotation = rotation(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    matrix.postTranslate(event.x - start.x, event.y - start.y)

                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }

                    val newRotation = rotation(event)
                    matrix.postRotate(newRotation - rotation, mid.x, mid.y)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        image.imageMatrix = matrix
        image.invalidate()

        return true
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = event.getX(0) - event.getX(1)
        val deltaY = event.getY(0) - event.getY(1)
        return Math.toDegrees(atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
