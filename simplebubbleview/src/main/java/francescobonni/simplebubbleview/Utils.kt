package francescobonni.simplebubbleview

import android.content.Context
import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.content.ContextWrapper
import android.app.Activity



internal object Utils {
    val singleTapConfirm = object : GestureDetector.SimpleOnGestureListener(){
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return super.onSingleTapUp(e)
        }
    }

    fun getActivity(context: Context?): Activity? {
        if (context == null) return null
        if (context is Activity) return context
        return if (context is ContextWrapper) getActivity(context.baseContext) else null
    }
}