package com.hemanth.imagedraweeview

/**
 *  Interface definition for callback to be invoked when attached ImageView scale changes
 *
 *  @author hemanth
 *  @since 18 may 2021
 */
interface OnScaleChangeListener {

    /**
     * Callback for when the scale changes
     *
     * @param scaleFactor the scale factor
     * @param focusX focal point X position
     * @param focusY focal point Y position
     */
    fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float)
}
