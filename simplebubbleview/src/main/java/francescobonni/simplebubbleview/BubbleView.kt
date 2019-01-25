package francescobonni.simplebubbleview

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.bubble_layout.view.*
import org.jetbrains.anko.dip
import kotlin.math.abs
import kotlin.random.Random

internal const val STIFNESS_BUBBLE_ANIMATION = 150f
class BubbleView : ConstraintLayout {

    private var gestureDetector: GestureDetector = GestureDetector(context, SingleTapConfirm())
    private var showPopup : Boolean = false
    private var dX: Float = 0.toFloat()
    private var dY: Float = 0.toFloat()
    private val xAnimForce = SpringForce().apply {
        dampingRatio =  SpringForce.DAMPING_RATIO_LOW_BOUNCY
        stiffness = SpringForce.STIFFNESS_HIGH
    }
    private val yAnimForce = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        stiffness = STIFNESS_BUBBLE_ANIMATION
    }
    private val alphaAnimForce = SpringForce().apply {
        dampingRatio = 1f
        stiffness = 40f
    }

    private val alphaAnimForceCancel = SpringForce().apply {
        dampingRatio = 0.45f
        stiffness = 40f
    }

    private val openAnimForce = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_LOW
    }
    private lateinit var xAnimBubble: SpringAnimation
    private lateinit var yAnimBubble: SpringAnimation
    private lateinit var alphaAnimBubbleCard: SpringAnimation
    private lateinit var slideYAnimBubbleCard: SpringAnimation
    private var alphaAnimCancel: SpringAnimation
    var click = false

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
               hideCancelLayout()
            }

            MotionEvent.ACTION_MOVE -> {
                if(alphaAnimBubbleCard.spring.finalPosition == 1f) hideCardView()
                val finalXBubble = (motionEvent.rawX + dX)
                val finalYBubble = (motionEvent.rawY + dY)
                xAnimBubble.animateToFinalPosition(finalXBubble)
                yAnimBubble.animateToFinalPosition(finalYBubble)
                Log.e("position", finalYBubble.toString())
                if(finalYBubble > 2*(this.height/3)) {
                    showCancelLayout()
                } else {
                    hideCancelLayout()
                }
                magnetToCancel(finalXBubble, finalYBubble)
            }
            else -> return@OnTouchListener false
        }
        return@OnTouchListener true
    }

    constructor(context: Context?) : super(context) {
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }


    init {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.bubble_layout, this, true)
        gestureDetector = GestureDetector(context, SingleTapConfirm())
        bubble.setOnTouchListener(draggableListener)
        alphaAnimBubbleCard = SpringAnimation(cardView, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForce.setFinalPosition(0f)
        }
        alphaAnimCancel = SpringAnimation(cancel, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForceCancel.setFinalPosition(0f)
        }
        alphaAnimCancel.animateToFinalPosition(1f)
        viewTreeObserver.addOnGlobalLayoutListener {

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val size = Point()
            display.getSize(size)
            yAnimBubble = SpringAnimation(bubble, DynamicAnimation.Y).apply {
                spring = yAnimForce
            }
            yAnimBubble.addUpdateListener { _, value, _ ->
                Log.e("value", value.toString())
                if(click && value <= dip(90).toFloat()) {
                    toogleCardView()
                }
            }
            xAnimBubble = SpringAnimation(bubble, DynamicAnimation.X).apply {
                spring = xAnimForce
            }
            val maxX = width.minus(bubble.width).minus(dip(16))
            val maxY = height.minus(bubble.height).minus(dip(64))
            slideYAnimBubbleCard = SpringAnimation(cardView, DynamicAnimation.Y).apply {
                spring = openAnimForce.setFinalPosition(height.toFloat())
            }
            slideYAnimBubbleCard.start()
            slideYAnimBubbleCard.skipToEnd()
            val randPositionX = Random.nextInt(bubble.width.plus(dip(16)), maxX)
            val randPositionY = Random.nextInt(dip(35), maxY)
            xAnimBubble.animateToFinalPosition(randPositionX.toFloat())
            yAnimBubble.animateToFinalPosition(randPositionY.toFloat())
        }
    }


    private fun bubbleClicked() : Boolean {
        if(cancel.alpha == 1f) {
            hideCancelLayout()
        }
        xAnimBubble.animateToFinalPosition(width.minus(bubble.width).minus(dip(16)).toFloat())
        yAnimBubble.animateToFinalPosition(dip(35).toFloat())
        click = true
        return true
    }

    private fun showCancelLayout() {
        alphaAnimCancel.animateToFinalPosition(1f)
    }

    private fun hideCancelLayout() {
        alphaAnimCancel.animateToFinalPosition(0f)
    }

    private fun toogleCardView() {
        click = false
        if(cardView.alpha == 0f) {
            showCardView()
        } else {
            hideCardView()
        }
    }

    private fun showCardView() {
        alphaAnimBubbleCard.animateToFinalPosition(1f)
        slideYAnimBubbleCard.animateToFinalPosition(dip(35).plus(bubble.height).toFloat())
    }

    private fun hideCardView() {
        alphaAnimBubbleCard.animateToFinalPosition(0f)
        slideYAnimBubbleCard.animateToFinalPosition(height.toFloat())
    }

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
        xAnimBubble.animateToFinalPosition(nearestXWall)
        yAnimBubble.animateToFinalPosition(bubble.y)
    }

    private fun magnetToCancel(finalXBubble: Float, finalYBubble: Float) {
        if(cancel.visibility == View.GONE) return
        val differenceX = abs(finalXBubble - cancel.x)
        val differenceY = abs(finalYBubble - cancel.y)
        if(differenceX + differenceY < dip(72)) {
            val widthDifference = bubble.width - cancel.width
            val heightDifference = bubble.height - cancel.height
            yAnimBubble.animateToFinalPosition(cancel.y - (heightDifference/2))
            xAnimBubble.animateToFinalPosition(cancel.x - (widthDifference/2))
        } else {
            /* cancelSpring.endValue = 1.0 */
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
        cardView.addView(layoutView)
        showPopup = true
        return this
    }

    private inner class SingleTapConfirm : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            return true
        }
    }
}