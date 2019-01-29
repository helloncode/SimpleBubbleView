package francescobonni.simplebubbleview

import android.content.Context
import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.WindowManager
import android.content.ContextWrapper
import android.app.Activity
import android.view.Display
import android.os.Build
import java.lang.reflect.InvocationTargetException


internal object Utils {

    fun getActivity(context: Context?): Activity? {
        if (context == null) return null
        if (context is Activity) return context
        return if (context is ContextWrapper) getActivity(context.baseContext) else null
    }

    fun getNavigationBarSize(context: Context): Int {
        val appUsableSize = getAppUsableScreenSize(context)
        val realScreenSize = getRealScreenSize(context)
        return if (appUsableSize.y < realScreenSize.y) {
            realScreenSize.y - appUsableSize.y
        } else 0
    }

    fun getAppUsableScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return size
    }
}