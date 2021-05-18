package com.hemanth.imagedraweeview

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Animatable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo

class ImageDraweeView : SimpleDraweeView, IAttacher {

    private lateinit var attacher: Attacher

    private var isEnableDraweeMatrix = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        init()
    }

    private fun init() {
        if (!this::attacher.isInitialized || attacher?.draweeView == null) {
            attacher = Attacher(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val saveCount = canvas.save()
        if (isEnableDraweeMatrix) {
            canvas.concat(attacher.drawMatrix)
        }
        super.onDraw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun onAttachedToWindow() {
        init()
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        attacher.onDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    override fun getMinimumScale(): Float = attacher.getMinimumScale()

    override fun getMediumScale(): Float = attacher.getMediumScale()

    override fun getMaximumScale(): Float = attacher.getMaximumScale()

    override fun setMaximumScale(maximumScale: Float) {
        attacher.setMaximumScale(maximumScale)
    }

    override fun setMediumScale(mediumScale: Float) {
        attacher.setMediumScale(mediumScale)
    }

    override fun setMinimumScale(minimumScale: Float) {
        attacher.setMinimumScale(minimumScale)
    }

    override fun getScale(): Float = attacher.getScale()

    override fun setScale(scale: Float) {
        attacher.setScale(scale)
    }

    override fun setScale(scale: Float, animate: Boolean) {
        attacher.setScale(scale, animate)
    }

    override fun setScale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        attacher.setScale(scale, focalX, focalY, animate)
    }

    override fun setOrientation(@Attacher.OrientationMode orientation: Int) {
        attacher.setOrientation(orientation)
    }

    override fun setZoomTransitionDuration(duration: Long) {
        attacher.setZoomTransitionDuration(duration)
    }

    override fun setAllowParentInterceptOnEdge(allow: Boolean) {
        attacher.setAllowParentInterceptOnEdge(allow)
    }

    override fun setOnDoubleTapListener(listener: GestureDetector.OnDoubleTapListener?) {
        attacher.setOnDoubleTapListener(listener)
    }

    override fun setOnScaleChangeListener(listener: OnScaleChangeListener) {
        attacher.setOnScaleChangeListener(listener)
    }

    override fun setOnLongClickListenerr(listener: OnLongClickListener) {
        attacher.setOnLongClickListenerr(listener)
    }

    override fun setOnPhotoTapListener(listener: OnPhotoTapListener) {
        attacher.setOnPhotoTapListener(listener)
    }

    override fun setOnViewTapListener(listener: OnViewTapListener) {
        attacher.setOnViewTapListener(listener)
    }

    override fun getOnPhotoTapListener(): OnPhotoTapListener {
        return attacher.getOnPhotoTapListener()
    }

    override fun getOnViewTapListener(): OnViewTapListener {
        return attacher.getOnViewTapListener()
    }

    override fun update(imageInfoWidth: Int, imageInfoHeight: Int) {
        attacher.update(imageInfoWidth, imageInfoHeight)
    }

    fun setPhotoUri(uri: Uri?) {
        setPhotoUri(uri, null)
    }

    fun setPhotoUri(uri: Uri?, context: Context?) {
        isEnableDraweeMatrix = false
        val controller: DraweeController = Fresco.newDraweeControllerBuilder()
            .setCallerContext(context)
            .setUri(uri)
            .setOldController(controller)
            .setControllerListener(object : BaseControllerListener<ImageInfo?>() {
                override fun onFailure(id: String, throwable: Throwable) {
                    super.onFailure(id, throwable)
                    isEnableDraweeMatrix = false
                }

                override fun onFinalImageSet(
                    id: String, imageInfo: ImageInfo?,
                    animatable: Animatable?
                ) {
                    super.onFinalImageSet(id, imageInfo, animatable)
                    isEnableDraweeMatrix = true
                    if (imageInfo != null) {
                        update(imageInfo.width, imageInfo.height)
                    }
                }

                override fun onIntermediateImageFailed(id: String, throwable: Throwable) {
                    super.onIntermediateImageFailed(id, throwable)
                    isEnableDraweeMatrix = false
                }

                override fun onIntermediateImageSet(id: String, imageInfo: ImageInfo?) {
                    super.onIntermediateImageSet(id, imageInfo)
                    isEnableDraweeMatrix = true
                    if (imageInfo != null) {
                        update(imageInfo.width, imageInfo.height)
                    }
                }
            })
            .build()
        setController(controller)
    }
}
