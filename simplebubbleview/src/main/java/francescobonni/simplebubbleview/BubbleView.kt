package francescobonni.simplebubbleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.support.v7.widget.CardView
import android.text.Layout
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
        background = find(Ids.background)
        bubble = find(Ids.bubble)
        cancel = find(Ids.cancel)
        cardView = find(Ids.cardview)
        gestureDetector = GestureDetector(context, SingleTapConfirm())
        bubbleXSpring = springSystem.createSpring().setSpringConfig(SpringConfig(70.toDouble(), 12.toDouble()))
        bubbleYSpring = springSystem.createSpring().setSpringConfig(SpringConfig(70.toDouble(), 12.toDouble()))
        bubble.setOnTouchListener(draggableListener)
    }

    private fun ViewManager.bubble() =
            constraintLayout {
                isClickable = false
                isFocusable = false
                padding = dip(16)

                imageView {
                    id = Ids.background
                    imageResource = R.drawable.gradient_bubble
                    visibility = View.GONE
                }.lparams(width = dip(0), height = dip(0)) {
                    bottomToBottom = ConstraintSet.PARENT_ID
                    endToEnd = ConstraintSet.PARENT_ID
                    startToStart = ConstraintSet.PARENT_ID
                }

                imageView {
                    id = Ids.bubble
                    imageResource = R.drawable.bubble_background
                }.lparams(width = dip(48), height = dip(48))

                imageView {
                    id = Ids.cancel
                    imageResource = R.drawable.icon_bubble
                    visibility = View.VISIBLE
                }.lparams(width = dip(48), height = dip(48)) {
                    topToTop = Ids.background
                    endToEnd = Ids.background
                    startToStart = Ids.background
                    bottomToBottom = Ids.background
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

    private inner class MoveSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            bubble.x = bubbleXSpring.currentValue.toFloat()
            bubble.y = bubbleYSpring.currentValue.toFloat()
        }
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
                showCancelLayout()
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
                hideCardView()
                val finalXBubble = (motionEvent.rawX + dX).toDouble()
                val finalYBubble = (motionEvent.rawY + dY).toDouble()
                springBubble(finalXBubble, finalYBubble)
                magnetToCancel(finalXBubble, finalYBubble)
            }
            else -> return@OnTouchListener false
        }
        return@OnTouchListener true
    }

    private fun bubbleClicked() : Boolean {
        toggleCancelLayout()
        springBubble(0.toDouble(), 0.toDouble())
        toggleCardView()
        return true
    }

    private fun toggleCancelLayout() {
        when(cancel.visibility) {
            View.VISIBLE -> hideCancelLayout()
            else -> showCancelLayout()
        }
    }

    private fun showCancelLayout() {
        cancel.visibility = View.VISIBLE
        background.visibility = View.VISIBLE
    }

    private fun hideCancelLayout() {
        cancel.visibility = View.GONE
        background.visibility = View.GONE
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
        val nearestXWall = (if (bubble.x >= middle) this.width - bubble.width else 0).toFloat()
        springBubble(nearestXWall.toDouble(), bubble.y.toDouble())
    }

    private fun magnetToCancel(finalXBubble: Double, finalYBubble: Double) {
        val differenceX = abs(finalXBubble - cancel.x)
        val differenceY = abs(finalYBubble - cancel.y)
        if(differenceX + differenceY < dip(32)) {
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

}