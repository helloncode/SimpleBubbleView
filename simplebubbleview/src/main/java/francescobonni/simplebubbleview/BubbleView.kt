package francescobonni.simplebubbleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.*
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.support.v7.widget.CardView
import android.view.*
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.constraint.layout.matchConstraint
import java.util.*
import kotlin.math.abs


class BubbleView : ConstraintLayout {

    private lateinit var background: ImageView
    private lateinit var bubble: ImageView
    private lateinit var cancel: ImageView
    private lateinit var cardView: CardView
    private lateinit var gestureDetector: GestureDetector
    private var showPopup : Boolean = false
    private val springSystem = SpringSystem.create()
    private lateinit var bubbleXSpring: Spring
    private lateinit var bubbleYSpring: Spring
    private var attachedToRootView = false
    private var dX: Float = 0.toFloat()
    private var dY:Float = 0.toFloat()

    constructor(context: Context?) : super(context) {
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        bubble().layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        background = find(Ids.background)
        bubble = find(Ids.bubble)
        cancel = find(Ids.cancel)
        cardView = find(Ids.cardview)
        gestureDetector = GestureDetector(context, SingleTapConfirm())
        bubbleXSpring = springSystem.createSpring().setSpringConfig(SpringConfig(120.toDouble(), 20.toDouble()))
        bubbleYSpring = springSystem.createSpring().setSpringConfig(SpringConfig(120.toDouble(), 20.toDouble()))
        bubble.setOnTouchListener(draggableListener)
    }

    private fun ViewManager.bubble() =
            constraintLayout {
                isClickable = false
                isFocusable = false

                imageView {
                    id = Ids.background
                    imageResource = R.drawable.gradient_bubble
                    visibility = View.GONE
                }.lparams(width = dip(0), height = dip(0)) {
                    bottomToBottom = ConstraintSet.PARENT_ID
                    endToEnd = ConstraintSet.PARENT_ID
                    startToStart = ConstraintSet.PARENT_ID
                    dimensionRatio = "540:225"
                }

                imageView {
                    id = Ids.bubble
                    imageResource = R.drawable.bubble_background
                    x = dip(16).toFloat()
                    y = dip(16).toFloat()
                }.lparams(width = dip(48), height = dip(48))

                imageView {
                    id = Ids.cancel
                    imageResource = R.drawable.icon_bubble
                    visibility = View.GONE
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }.lparams(width = dip(64), height = dip(64)) {
                    topToTop = Ids.background
                    endToEnd = Ids.background
                    startToStart = Ids.background
                    bottomToBottom = Ids.background
                    verticalBias = 0.40f
                }

                cardView {
                    id = Ids.cardview
                    visibility = View.GONE
                }.lparams(width = wrapContent, height = wrapContent) {
                    topToTop = ConstraintSet.PARENT_ID
                    startToStart = ConstraintSet.PARENT_ID
                    endToEnd = ConstraintSet.PARENT_ID
                    bottomToBottom = ConstraintSet.PARENT_ID
                }
            }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bubbleXSpring.addListener(MoveSpringListener())
        bubbleYSpring.addListener(MoveSpringListener())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bubbleXSpring.removeListener(MoveSpringListener())
        bubbleYSpring.removeListener(MoveSpringListener())
    }

    private object Ids {
        val background = View.generateViewId()
        val bubble = View.generateViewId()
        val cancel = View.generateViewId()
        val cardview = View.generateViewId()
    }

    private var draggableListener = View.OnTouchListener { view, motionEvent ->
        if(showPopup) {
            if (gestureDetector.onTouchEvent(motionEvent)) {
                return@OnTouchListener bubbleClicked()
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
                val finalXBubble = (motionEvent.rawX + dX).toDouble()
                val finalYBubble = (motionEvent.rawY + dY).toDouble()
                springBubble(finalXBubble, finalYBubble)
                hideCardView()
                if(finalYBubble > this.height/2) {
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

    private fun bubbleClicked() : Boolean {
        hideCancelLayout()
        if(attachedToRootView) {
            springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(16).toDouble())
        } else {
            springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(36).toDouble())
        }
        toggleCardView()
        return true
    }

    private fun showCancelLayout() {
        if(cancel.visibility == View.VISIBLE) return
        TransitionManager.beginDelayedTransition(this, getShowCancelTransition())
        cancel.visibility = View.VISIBLE
        background.visibility = View.VISIBLE
    }

    private fun getShowCancelTransition() : TransitionSet {
        val transitionSet = TransitionSet()
        transitionSet.startDelay = 500
        transitionSet.interpolator = FastOutSlowInInterpolator()
        val fade = Fade()
        fade.duration = 1200
        fade.startDelay = 300
        val slide = Slide(Gravity.BOTTOM)
        slide.duration = 700
        transitionSet.addTransition(fade)
        transitionSet.addTransition(slide)
        return transitionSet
    }

    private fun hideCancelLayout() {
        if(cancel.visibility == View.GONE) return
        TransitionManager.beginDelayedTransition(this, getHideCancelTransition())
        cancel.visibility = View.GONE
        background.visibility = View.GONE
    }

    private fun getHideCancelTransition() : TransitionSet {
        val transition = AutoTransition()
        transition.startDelay = 200
        transition.duration = 400
        transition.interpolator = FastOutSlowInInterpolator()
        return transition
    }

    private fun springBubble(x: Double, y: Double) {
        bubbleXSpring.endValue = x
        bubbleYSpring.endValue = y
    }

    private fun toggleCardView() {
        when(cardView.visibility) {
            View.VISIBLE -> hideCardView()
            else -> showCardView()
        }
    }

    private fun showCardView() {
        cardView.visibility = View.VISIBLE
    }

    private fun hideCardView() {
        cardView.visibility = View.GONE
    }

    private fun isBubbleOnCancel() : Boolean {
        val differenceX = abs(bubble.x - cancel.x)
        val differenceY = abs(bubble.y - cancel.y)
        return differenceX + differenceY < dip(16)
    }

    private fun hideBubble() {
        bubble.visibility = View.GONE
    }

    private fun stickBubbleToWall() {
        val middle = this.width / 2
        val nearestXWall = (if (bubble.x >= middle) (this.width - bubble.width) - dip(16) else dip(16)).toFloat()
        springBubble(nearestXWall.toDouble(), bubble.y.toDouble())
    }

    private fun magnetToCancel(finalXBubble: Double, finalYBubble: Double) {
        if(cancel.visibility == View.GONE) return
        val differenceX = abs(finalXBubble - cancel.x)
        val differenceY = abs(finalYBubble - cancel.y)
        if(differenceX + differenceY < dip(72)) {
            springBubble(cancel.x.toDouble(), cancel.y.toDouble())
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
        return setLayoutPopup(context.layoutInflater.inflate(layoutId, cardView, false))
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

    private inner class MoveSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            bubble.x = bubbleXSpring.currentValue.toFloat()
            bubble.y = bubbleYSpring.currentValue.toFloat()
        }
    }
}