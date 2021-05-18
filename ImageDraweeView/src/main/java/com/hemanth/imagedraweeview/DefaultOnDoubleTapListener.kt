package com.hemanth.imagedraweeview

import android.view.GestureDetector
import android.view.MotionEvent

class DefaultOnDoubleTapListener constructor(val mAttacher: Attacher) : GestureDetector.OnDoubleTapListener {

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val draweeView = mAttacher.draweeView

        val displayRect = mAttacher.displayRect
        if (null != displayRect) {
            val x = e.x
            val y = e.y
            if (displayRect.contains(x, y)) {
                val xResult = (x - displayRect.left) / displayRect.width()
                val yResult = (y - displayRect.top) / displayRect.height()
                mAttacher.getOnPhotoTapListener().onPhotoTap(draweeView!!, xResult, yResult)
                return true
            }
        }

        mAttacher.getOnViewTapListener().onViewTap(draweeView!!, e.x, e.y)
        return true
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        try {
            val scale = mAttacher.getScale()
            val x = event.x
            val y = event.y

            // min, mid, max
            when {
                scale < mAttacher.getMediumScale() -> {
                    mAttacher.setScale(mAttacher.getMediumScale(), x, y, true)
                }
                scale >= mAttacher.getMediumScale() && scale < mAttacher.getMaximumScale() -> {
                    mAttacher.setScale(mAttacher.getMaximumScale(), x, y, true)
                }
                else -> {
                    mAttacher.setScale(mAttacher.getMinimumScale(), x, y, true)
                }
            }
        } catch (e: Exception) {
            // Can sometimes happen when getX() and getY() is called
        }
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean = false

}
