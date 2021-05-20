package com.hemanth.imagedraweeview

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.annotation.IntDef
import androidx.core.view.GestureDetectorCompat
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.view.DraweeView
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.ref.WeakReference

class Attacher(draweeView: DraweeView<GenericDraweeHierarchy>) : IAttacher, OnTouchListener,
    OnScaleDragGestureListener {
    @IntDef(HORIZONTAL, VERTICAL)
    @Retention(RetentionPolicy.SOURCE)
    annotation class OrientationMode

    private var mOrientation = HORIZONTAL
    private val mMatrixValues = FloatArray(9)
    private val mDisplayRect = RectF()
    private val mZoomInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    private var mMinScale = IAttacher.DEFAULT_MIN_SCALE
    private var mMidScale = IAttacher.DEFAULT_MID_SCALE
    private var mMaxScale = IAttacher.DEFAULT_MAX_SCALE
    private var mZoomDuration = IAttacher.ZOOM_DURATION
    private val mScaleDragDetector: ScaleDragDetector
    private val mGestureDetector: GestureDetectorCompat
    private var mBlockParentIntercept = false
    private var mAllowParentInterceptOnEdge = true
    private var mScrollEdgeX = EDGE_BOTH
    private var mScrollEdgeY = EDGE_BOTH
    val drawMatrix = Matrix()
    private var mImageInfoHeight = -1
    private var mImageInfoWidth = -1
    private var mCurrentFlingRunnable: FlingRunnable? = null
    private val mDraweeView: WeakReference<DraweeView<GenericDraweeHierarchy>>
    private var mPhotoTapListener: OnPhotoTapListener? = null
    private var mViewTapListener: OnViewTapListener? = null
    private var mLongClickListener: OnLongClickListener? = null
    private var mScaleChangeListener: OnScaleChangeListener? = null
    override fun setOnDoubleTapListener(newOnDoubleTapListener: GestureDetector.OnDoubleTapListener?) {
        if (newOnDoubleTapListener != null) {
            mGestureDetector.setOnDoubleTapListener(newOnDoubleTapListener)
        } else {
            mGestureDetector.setOnDoubleTapListener(DefaultOnDoubleTapListener(this))
        }
    }

    val draweeView: DraweeView<GenericDraweeHierarchy>?
        get() = mDraweeView.get()

    override fun getMinimumScale(): Float {
        return mMinScale
    }

    override fun getMediumScale(): Float {
        return mMidScale
    }

    override fun getMaximumScale(): Float {
        return mMaxScale
    }

    override fun setMaximumScale(maximumScale: Float) {
        checkZoomLevels(mMinScale, mMidScale, maximumScale)
        mMaxScale = maximumScale
    }

    override fun setMediumScale(mediumScale: Float) {
        checkZoomLevels(mMinScale, mediumScale, mMaxScale)
        mMidScale = mediumScale
    }

    override fun setMinimumScale(minimumScale: Float) {
        checkZoomLevels(minimumScale, mMidScale, mMaxScale)
        mMinScale = minimumScale
    }

    override fun getScale(): Float {
        return Math.sqrt(
            (
                    Math.pow(
                        getMatrixValue(drawMatrix, Matrix.MSCALE_X).toDouble(),
                        2.0
                    ).toFloat() + Math.pow(
                        getMatrixValue(drawMatrix, Matrix.MSKEW_Y).toDouble(), 2.0
                    ).toFloat()).toDouble()
        ).toFloat()
    }

    override fun setScale(scale: Float) {
        setScale(scale, false)
    }

    override fun setScale(scale: Float, animate: Boolean) {
        val draweeView = draweeView
        if (draweeView != null) {
            setScale(
                scale,
                (draweeView.right / 2).toFloat(),
                (draweeView.bottom / 2).toFloat(),
                animate
            )
        }
    }

    override fun setScale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        val draweeView = draweeView
        if (draweeView == null || scale < mMinScale || scale > mMaxScale) {
            return
        }
        if (animate) {
            draweeView.post(AnimatedZoomRunnable(getScale(), scale, focalX, focalY))
        } else {
            drawMatrix.setScale(scale, scale, focalX, focalY)
            checkMatrixAndInvalidate()
        }
    }

    override fun setOrientation(@OrientationMode orientation: Int) {
        mOrientation = orientation
    }

    override fun setZoomTransitionDuration(duration: Long) {
        var duration = duration
        duration = if (duration < 0) IAttacher.ZOOM_DURATION else duration
        mZoomDuration = duration
    }

    override fun setAllowParentInterceptOnEdge(allow: Boolean) {
        mAllowParentInterceptOnEdge = allow
    }

    override fun setOnScaleChangeListener(listener: OnScaleChangeListener) {
        mScaleChangeListener = listener
    }

    override fun setOnLongClickListenerr(listener: OnLongClickListener) {
        mLongClickListener = listener
    }

    override fun setOnPhotoTapListener(listener: OnPhotoTapListener) {
        mPhotoTapListener = listener
    }

    override fun setOnViewTapListener(listener: OnViewTapListener) {
        mViewTapListener = listener
    }

    override fun getOnPhotoTapListener(): OnPhotoTapListener {
        return mPhotoTapListener!!
    }

    override fun getOnViewTapListener(): OnViewTapListener {
        return mViewTapListener!!
    }

    override fun update(imageInfoWidth: Int, imageInfoHeight: Int) {
        mImageInfoWidth = imageInfoWidth
        mImageInfoHeight = imageInfoHeight
        updateBaseMatrix()
    }

    private val viewWidth: Int
        private get() {
            val draweeView = draweeView
            return if (draweeView != null) {
                (draweeView.width
                        - draweeView.paddingLeft
                        - draweeView.paddingRight)
            } else 0
        }
    private val viewHeight: Int
        private get() {
            val draweeView = draweeView
            return if (draweeView != null) {
                (draweeView.height
                        - draweeView.paddingTop
                        - draweeView.paddingBottom)
            } else 0
        }

    private fun getMatrixValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    val displayRect: RectF?
        get() {
            checkMatrixBounds()
            return getDisplayRect(drawMatrix)
        }

    fun checkMatrixAndInvalidate() {
        val draweeView = draweeView ?: return
        if (checkMatrixBounds()) {
            draweeView.invalidate()
        }
    }

    fun checkMatrixBounds(): Boolean {
        val rect = getDisplayRect(drawMatrix) ?: return false
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0.0f
        var deltaY = 0.0f
        val viewHeight = viewHeight
        if (height <= viewHeight.toFloat()) {
            deltaY = (viewHeight - height) / 2 - rect.top
            mScrollEdgeY = EDGE_BOTH
        } else if (rect.top > 0.0f) {
            deltaY = -rect.top
            mScrollEdgeY = EDGE_TOP
        } else if (rect.bottom < viewHeight.toFloat()) {
            deltaY = viewHeight - rect.bottom
            mScrollEdgeY = EDGE_BOTTOM
        } else {
            mScrollEdgeY = EDGE_NONE
        }
        val viewWidth = viewWidth
        if (width <= viewWidth) {
            deltaX = (viewWidth - width) / 2 - rect.left
            mScrollEdgeX = EDGE_BOTH
        } else if (rect.left > 0) {
            deltaX = -rect.left
            mScrollEdgeX = EDGE_LEFT
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
            mScrollEdgeX = EDGE_RIGHT
        } else {
            mScrollEdgeX = EDGE_NONE
        }
        drawMatrix.postTranslate(deltaX, deltaY)
        return true
    }

     fun getDisplayRect(matrix: Matrix): RectF? {
        val draweeView = draweeView
        if (draweeView == null || mImageInfoWidth == -1 && mImageInfoHeight == -1) {
            return null
        }
        mDisplayRect[0.0f, 0.0f, mImageInfoWidth.toFloat()] = mImageInfoHeight.toFloat()
        draweeView.hierarchy.getActualImageBounds(mDisplayRect)
        matrix.mapRect(mDisplayRect)
        return mDisplayRect
    }

    private fun updateBaseMatrix() {
        if (mImageInfoWidth == -1 && mImageInfoHeight == -1) {
            return
        }
        resetMatrix()
    }

    private fun resetMatrix() {
        drawMatrix.reset()
        checkMatrixBounds()
        val draweeView = draweeView
        draweeView?.invalidate()
    }

    private fun checkMinScale() {
        val draweeView = draweeView ?: return
        if (getScale() < mMinScale) {
            val rect = displayRect
            if (null != rect) {
                draweeView.post(
                    AnimatedZoomRunnable(
                        getScale(), mMinScale, rect.centerX(),
                        rect.centerY()
                    )
                )
            }
        }
    }

    override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
        if (getScale() < mMaxScale || scaleFactor < 1.0f) {
            if (mScaleChangeListener != null) {
                mScaleChangeListener!!.onScaleChange(scaleFactor, focusX, focusY)
            }
            drawMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            checkMatrixAndInvalidate()
        }
    }

    override fun onScaleEnd() {
        checkMinScale()
    }

    override fun onDrag(dx: Float, dy: Float) {
        val draweeView = draweeView
        if (draweeView != null && !mScaleDragDetector.isScaling) {
            drawMatrix.postTranslate(dx, dy)
            checkMatrixAndInvalidate()
            val parent = draweeView.parent ?: return
            if (mAllowParentInterceptOnEdge
                && !mScaleDragDetector.isScaling
                && !mBlockParentIntercept
            ) {
                if (mOrientation == HORIZONTAL && (mScrollEdgeX == EDGE_BOTH || (mScrollEdgeX
                            == EDGE_LEFT && dx >= 1f) || mScrollEdgeX == EDGE_RIGHT && dx <= -1f)
                ) {
                    parent.requestDisallowInterceptTouchEvent(false)
                } else if (mOrientation == VERTICAL && (mScrollEdgeY == EDGE_BOTH || (mScrollEdgeY
                            == EDGE_TOP && dy >= 1f) || mScrollEdgeY == EDGE_BOTTOM && dy <= -1f)
                ) {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            } else {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
    }

    override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        val draweeView = draweeView ?: return
        mCurrentFlingRunnable = FlingRunnable(draweeView.context)
        mCurrentFlingRunnable!!.fling(
            viewWidth, viewHeight, velocityX.toInt(),
            velocityY.toInt()
        )
        draweeView.post(mCurrentFlingRunnable)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val parent = v.parent
                parent?.requestDisallowInterceptTouchEvent(true)
                cancelFling()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val parent = v.parent
                parent?.requestDisallowInterceptTouchEvent(false)
            }
            else -> {
            }
        }
        val wasScaling = mScaleDragDetector.isScaling
        val wasDragging = mScaleDragDetector.isDragging
        var handled = mScaleDragDetector.onTouchEvent(event)
        val noScale = !wasScaling && !mScaleDragDetector.isScaling
        val noDrag = !wasDragging && !mScaleDragDetector.isDragging
        mBlockParentIntercept = noScale && noDrag
        if (mGestureDetector.onTouchEvent(event)) {
            handled = true
        }
        return handled
    }

    private inner class AnimatedZoomRunnable(
        currentZoom: Float, targetZoom: Float,
        private val mFocalX: Float, private val mFocalY: Float
    ) : Runnable {
        private val mStartTime: Long
        private val mZoomStart: Float
        private val mZoomEnd: Float
        override fun run() {
            val draweeView = draweeView ?: return
            val t = interpolate()
            val scale = mZoomStart + t * (mZoomEnd - mZoomStart)
            val deltaScale = scale / getScale()
            onScale(deltaScale, mFocalX, mFocalY)
            if (t < 1f) {
                postOnAnimation(draweeView, this)
            }
        }

        private fun interpolate(): Float {
            var t = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration
            t = Math.min(1f, t)
            t = mZoomInterpolator.getInterpolation(t)
            return t
        }

        init {
            mStartTime = System.currentTimeMillis()
            mZoomStart = currentZoom
            mZoomEnd = targetZoom
        }
    }

    private inner class FlingRunnable(context: Context?) : Runnable {
        private val mScroller: OverScroller
        private var mCurrentX = 0
        private var mCurrentY = 0
        fun cancelFling() {
            mScroller.abortAnimation()
        }

        fun fling(viewWidth: Int, viewHeight: Int, velocityX: Int, velocityY: Int) {
            val rect = displayRect ?: return
            val startX = Math.round(-rect.left)
            val minX: Int
            val maxX: Int
            val minY: Int
            val maxY: Int
            if (viewWidth < rect.width()) {
                minX = 0
                maxX = Math.round(rect.width() - viewWidth)
            } else {
                maxX = startX
                minX = maxX
            }
            val startY = Math.round(-rect.top)
            if (viewHeight < rect.height()) {
                minY = 0
                maxY = Math.round(rect.height() - viewHeight)
            } else {
                maxY = startY
                minY = maxY
            }
            mCurrentX = startX
            mCurrentY = startY
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0)
            }
        }

        override fun run() {
            if (mScroller.isFinished) {
                return
            }
            val draweeView = draweeView
            if (draweeView != null && mScroller.computeScrollOffset()) {
                val newX = mScroller.currX
                val newY = mScroller.currY
                drawMatrix.postTranslate((mCurrentX - newX).toFloat(), (mCurrentY - newY).toFloat())
                draweeView.invalidate()
                mCurrentX = newX
                mCurrentY = newY
                postOnAnimation(draweeView, this)
            }
        }

        init {
            mScroller = OverScroller(context)
        }
    }

    private fun cancelFling() {
        if (mCurrentFlingRunnable != null) {
            mCurrentFlingRunnable!!.cancelFling()
            mCurrentFlingRunnable = null
        }
    }

    private fun postOnAnimation(view: View, runnable: Runnable) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postOnAnimation(runnable)
        } else {
            view.postDelayed(runnable, 16L)
        }
    }

    public fun onDetachedFromWindow() {
        cancelFling()
    }

    companion object {
        private const val EDGE_NONE = -1
        private const val EDGE_LEFT = 0
        private const val EDGE_TOP = 0
        private const val EDGE_RIGHT = 1
        private const val EDGE_BOTTOM = 1
        private const val EDGE_BOTH = 2
        const val HORIZONTAL = 0
        const val VERTICAL = 1
        private fun checkZoomLevels(minZoom: Float, midZoom: Float, maxZoom: Float) {
            require(minZoom < midZoom) { "MinZoom has to be less than MidZoom" }
            require(midZoom < maxZoom) { "MidZoom has to be less than MaxZoom" }
        }
    }

    init {
        mDraweeView = WeakReference(draweeView)
        draweeView.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FIT_CENTER
        draweeView.setOnTouchListener(this)
        mScaleDragDetector = ScaleDragDetector(draweeView.context, this)
        mGestureDetector = GestureDetectorCompat(draweeView.context,
            object : SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    super.onLongPress(e)
                    if (null != mLongClickListener) {
                        mLongClickListener!!.onLongClick(draweeView)
                    }
                }
            })
        mGestureDetector.setOnDoubleTapListener(DefaultOnDoubleTapListener(this))
    }
}
