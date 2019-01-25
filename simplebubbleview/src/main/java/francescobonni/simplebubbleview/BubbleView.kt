package francescobonni.simplebubbleview

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import android.view.*
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem
import kotlin.math.abs
import android.view.WindowManager
import android.graphics.Point
import android.util.Log
import android.util.TypedValue
import org.jetbrains.anko.dip
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.bubble_layout.view.*
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.random.Random


class BubbleView : ConstraintLayout {

    private lateinit var gestureDetector: GestureDetector
    private var showPopup : Boolean = false
    private var dX: Float = 0.toFloat()
    private var dY: Float = 0.toFloat()
    private lateinit var xAnimBubble: SpringAnimation
    private lateinit var yAnimBubble: SpringAnimation

    private var draggableListener = View.OnTouchListener { view, motionEvent ->
        if(showPopup) {
            if(gestureDetector.onTouchEvent(motionEvent)) {
                bubbleClicked()
                return@OnTouchListener true
            }
        }

        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = view.x - motionEvent.rawX
                dY = view.y - motionEvent.rawY
            }

            MotionEvent.ACTION_UP -> {
                if(isBubbleOnCancel()) {
                    hideBubble()
                } else {
                    stickBubbleToWall()
                }
                //hideCancelLayout()
            }

            MotionEvent.ACTION_MOVE -> {
                //if(cardView.alpha == 1f) hideCardView()
                val finalXBubble = (motionEvent.rawX + dX).toDouble()
                val finalYBubble = (motionEvent.rawY + dY).toDouble()
                //springBubble(finalXBubble, finalYBubble)
                if(finalYBubble > this.height/2) {
                    //showCancelLayout()
                } else {
                    //hideCancelLayout()
                }
                magnetToCancel(finalXBubble, finalYBubble)
            }
            else -> return@OnTouchListener false
        }
        return@OnTouchListener true
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.bubble_layout, this, true)
        gestureDetector = GestureDetector(context, SingleTapConfirm())
        bubble.setOnTouchListener(draggableListener)
        viewTreeObserver.addOnGlobalLayoutListener {

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val size = Point()
            display.getSize(size)
            val force = SpringForce()
            force.dampingRatio = 0.45f
            force.stiffness = 150f
            val force2 = SpringForce()
            force2.dampingRatio = 0.45f
            force2.stiffness = 150f
            yAnimBubble = SpringAnimation(bubble, DynamicAnimation.Y)
            yAnimBubble.spring = force2
            xAnimBubble = SpringAnimation(bubble, DynamicAnimation.X)
            xAnimBubble.spring = force
            val maxX = width.minus(bubble.width).minus(dip(16))
            val maxY = height.minus(bubble.height).minus(dip(64))
            val randPositionX = Random.nextInt(bubble.width.plus(dip(16)), maxX)
            val randPositionY = Random.nextInt(bubble.height.plus(dip(35)), maxY)
            xAnimBubble.animateToFinalPosition(randPositionX.toFloat())
            yAnimBubble.animateToFinalPosition(randPositionY.toFloat())
        }
    }


    private fun bubbleClicked() : Boolean {
        if(cancel.alpha == 1f) {
            //hideCancelLayout()
        }
        //springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(64).toDouble())
        //toogleCardView()
        return true
    }

    /*private fun showCancelLayout() {
        if(cancelLayoutSpring.currentValue == 1.0) return
        cancelLayoutSpring.endValue = 1.0
    }

    private fun hideCancelLayout() {
        if(cancelLayoutSpring.currentValue == 0.0) return
        cancelLayoutSpring.endValue = 0.0
    }

    private fun springBubble(x: Double, y: Double) {
        bubbleXSpring.endValue = x
        bubbleYSpring.endValue = y
    }

    private fun toogleCardView() {
        if(cardSpring.endValue == 0.toDouble()) {
            showCardView()
        } else {
            hideCardView()
        }
    }

    private fun showCardView() {
        cardSpring.endValue = 1.toDouble()
    }

    private fun hideCardView() {
        cardSpring.endValue = 0.toDouble()
    }*/

    private fun isBubbleOnCancel() : Boolean {
        val isOnCancelX = bubble.x >= cancel.x && bubble.x < cancel.x + cancel.width
        val isOnCancelY = bubble.y >= cancel.y && bubble.y < cancel.y + cancel.height
        return isOnCancelX && isOnCancelY
    }

    private fun hideBubble() {
        bubble.visibility = View.GONE
    }

    private fun stickBubbleToWall() {
        val middle = this.width / 2
        val nearestXWall = (if (bubble.x >= middle) (this.width - bubble.width) - dip(16) else dip(16)).toFloat()
        //springBubble(nearestXWall.toDouble(), bubble.y.toDouble())
    }

    private fun magnetToCancel(finalXBubble: Double, finalYBubble: Double) {
        if(cancel.visibility == View.GONE) return
        val differenceX = abs(finalXBubble - cancel.x)
        val differenceY = abs(finalYBubble - cancel.y)
        if(differenceX + differenceY < dip(72)) {
            val widthDifference = bubble.width - cancel.width
            val heightDifference = bubble.height - cancel.height
            //cancelSpring.endValue = 1.5
            //springBubble(cancel.x.toDouble() - (widthDifference/2), cancel.y.toDouble() - (heightDifference/2))
        } else {
            //cancelSpring.endValue = 1.0
        }
    }

    fun setCancelImage(resourceId: Int) : BubbleView {
        Glide.with(context).load(resourceId).into(cancel)
        return this
    }

    fun setCancelImage(drawable: Drawable) : BubbleView {
        Glide.with(context).load(drawable).into(cancel)
        return this
    }

    fun setCancelImage(url: String) : BubbleView {
        Glide.with(context).load(url).into(cancel)
        return this
    }

    fun setBubbleImage(resourceId: Int) : BubbleView {
        Glide.with(context).load(resourceId).into(bubble)
        return this
    }

    fun setBubbleImage(drawable: Drawable) : BubbleView {
        Glide.with(context).load(drawable).into(bubble)
        return this
    }

    fun setBubbleImage(url: String) : BubbleView {
        Glide.with(context).load(url).into(bubble)
        return this
    }

    fun setLayoutPopup(layoutId: Int) : BubbleView {
        return setLayoutPopup(View.inflate(context, layoutId, null))
    }

    fun setLayoutPopup(layoutView: View) : BubbleView {
        //cardView.addView(layoutView)
        showPopup = true
        return this
    }

    private inner class SingleTapConfirm : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            return true
        }
    }

    /*private inner class MoveSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            bubble.x = bubbleXSpring.currentValue.toFloat()
            bubble.y = bubbleYSpring.currentValue.toFloat()
        }
    }

    private inner class CancelSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            cancel.scaleX = cancelSpring.currentValue.toFloat()
            cancel.scaleY = cancelSpring.currentValue.toFloat()
        }
    }

    private inner class CancelLayoutSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            cancel.scaleY = cancelLayoutSpring.currentValue.toFloat()
            cancel.translationY = (-1000 * (cancelLayoutSpring.currentValue.toFloat() - 1))
            cancel.imageAlpha = (255 * cancelLayoutSpring.currentValue.toFloat()).toInt()
            background.imageAlpha = (255 * cancelLayoutSpring.currentValue.toFloat()).toInt()
        }
    }

    private inner class CardSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            cardView.scaleY = cardSpring.currentValue.toFloat()
            cardView.scaleX = cardSpring.currentValue.toFloat()
            cardView.alpha = cardSpring.currentValue.toFloat()
        }
    }*/
}