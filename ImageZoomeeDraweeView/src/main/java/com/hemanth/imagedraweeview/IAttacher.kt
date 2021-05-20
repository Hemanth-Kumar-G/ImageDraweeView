package com.hemanth.imagedraweeview

import android.view.GestureDetector
import android.view.View

interface IAttacher {

    companion object {

        const val DEFAULT_MAX_SCALE = 3.0f
        const val DEFAULT_MID_SCALE = 1.75f
        const val DEFAULT_MIN_SCALE = 1.0f
        const val ZOOM_DURATION = 200L

    }

    fun getMinimumScale(): Float

    fun getMediumScale(): Float

    fun getMaximumScale(): Float

    fun setMaximumScale(maximumScale: Float)

    fun setMediumScale(mediumScale: Float)

    fun setMinimumScale(minimumScale: Float)

    fun getScale(): Float

    fun setScale(scale: Float)

    fun setScale(scale: Float, animate: Boolean)

    fun setScale(scale: Float, focalX: Float, focalY: Float, animate: Boolean)

    fun setOrientation(@Attacher.OrientationMode orientation: Int)

    fun setZoomTransitionDuration(duration: Long)

    fun setAllowParentInterceptOnEdge(allow: Boolean)

    fun setOnDoubleTapListener(listener: GestureDetector.OnDoubleTapListener?)

    fun setOnScaleChangeListener(listener: OnScaleChangeListener)

    fun setOnLongClickListenerr(listener: View.OnLongClickListener)

    fun setOnPhotoTapListener(listener: OnPhotoTapListener)

    fun setOnViewTapListener(listener: OnViewTapListener)

    fun getOnPhotoTapListener(): OnPhotoTapListener

    fun getOnViewTapListener(): OnViewTapListener

    fun update(imageInfoWidth: Int, imageInfoHeight: Int)
}
