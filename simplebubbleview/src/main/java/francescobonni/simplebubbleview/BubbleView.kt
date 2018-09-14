package francescobonni.simplebubbleview

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.util.AttributeSet
import android.support.v7.widget.CardView
import android.view.*
import android.widget.ImageView
import com.bumptech.glide.Glide
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
import android.view.WindowManager
import android.graphics.Color
import android.graphics.Point
import android.util.Log
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo


class BubbleView : ConstraintLayout {

    private lateinit var background: ImageView
    private lateinit var bubble: ImageView
    private lateinit var cancel: ImageView
    private lateinit var arrow: ImageView
    private lateinit var cardView: CardView
    private lateinit var gestureDetector: GestureDetector
    private var showPopup : Boolean = false
    private val springSystem = SpringSystem.create()
    private lateinit var bubbleXSpring: Spring
    private lateinit var bubbleYSpring: Spring
    private lateinit var cancelSpring: Spring
    private lateinit var cancelLayoutSpring: Spring
    private lateinit var cardSpring: Spring
    private var attachedToRootView = false
    private var showCardViewLayoutEffect: YoYo.YoYoString? = null
    private var hideCardViewLayoutEffect: YoYo.YoYoString? = null

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
        arrow = find(Ids.arrow)
        gestureDetector = GestureDetector(context, SingleTapConfirm())
        bubbleXSpring = springSystem.createSpring().setSpringConfig(SpringConfig(120.toDouble(), 12.toDouble()))
        bubbleYSpring = springSystem.createSpring().setSpringConfig(SpringConfig(120.toDouble(), 12.toDouble()))
        cancelSpring = springSystem.createSpring().setSpringConfig(SpringConfig(75.toDouble(), 8.toDouble()))
        cancelLayoutSpring = springSystem.createSpring().setSpringConfig(SpringConfig(60.toDouble(), 15.toDouble()))
        cardSpring = springSystem.createSpring().setSpringConfig(SpringConfig(120.toDouble(), 16.toDouble()))
        cancelLayoutSpring.setEndValue(0.0).setAtRest()
        cancelSpring.currentValue = 1.0
        bubble.setOnTouchListener(draggableListener)
        val lt = LayoutTransition()
        lt.disableTransitionType(LayoutTransition.DISAPPEARING)
        this.layoutTransition = lt
    }

    private fun ViewManager.bubble() =
            constraintLayout {
                isClickable = false
                isFocusable = false

                imageView {
                    id = Ids.background
                    imageResource = R.drawable.gradient_bubble
                    scaleY = 0f
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
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    scaleY = 0f
                }.lparams(width = dip(64), height = dip(64)) {
                    topToTop = Ids.background
                    endToEnd = Ids.background
                    startToStart = Ids.background
                    bottomToBottom = Ids.background
                    verticalBias = 0.40f
                }

                cardView {
                    id = Ids.cardview
                    scaleY = 0f
                    backgroundColor = Color.GRAY
                }.lparams(width = matchConstraint, height = wrapContent) {
                    topToTop = ConstraintSet.PARENT_ID
                    startToStart = ConstraintSet.PARENT_ID
                    endToEnd = ConstraintSet.PARENT_ID
                    if(!attachedToRootView) {
                        setMargins(0, dip(80), 0, 0)
                    } else {
                        setMargins(0, dip(110), 0, 0)
                    }
                }

                imageView {
                    id = Ids.arrow
                    scaleY = 0f
                    imageResource = R.drawable.triangle_drawable
                }.lparams(width = dip(36), height = dip(36)) {
                    topToTop = Ids.cardview
                    endToEnd = ConstraintSet.PARENT_ID
                    if(!attachedToRootView) {
                        setMargins(0, 0, dip(22), 0)
                    } else {
                        setMargins(0, 0, dip(22), 0)
                    }
                }
            }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bubbleXSpring.addListener(MoveSpringListener())
        bubbleYSpring.addListener(MoveSpringListener())
        cancelSpring.addListener(CancelSpringListener())
        cancelLayoutSpring.addListener(CancelLayoutSpringListener())
        cardSpring.addListener(CardSpringListener())

        val boolean = Random().nextInt(1) == 1
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        springBubble(
                if(boolean)
                    dip(16).toDouble()
                else
                    ((width.toDouble() - bubble.width) - dip(64)),
                Random().nextInt(height/2).toDouble() + dip(16).toDouble())
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bubbleXSpring.removeListener(MoveSpringListener())
        bubbleYSpring.removeListener(MoveSpringListener())
        cancelSpring.removeListener(CancelSpringListener())
        cancelLayoutSpring.removeListener(CancelLayoutSpringListener())
        cardSpring.removeListener(CardSpringListener())
    }

    private object Ids {
        val background = View.generateViewId()
        val bubble = View.generateViewId()
        val cancel = View.generateViewId()
        val cardview = View.generateViewId()
        val arrow = View.generateViewId()
    }

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
                if(cardView.alpha == 1f) hideCardView()
                val finalXBubble = (motionEvent.rawX + dX).toDouble()
                val finalYBubble = (motionEvent.rawY + dY).toDouble()
                springBubble(finalXBubble, finalYBubble)
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
        if(cancel.alpha == 1f) {
            hideCancelLayout()
        }
        if(!attachedToRootView) {
            springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(16).toDouble())
        } else {
            springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(46).toDouble())
        }
        toogleCardView()
        return true
    }

    private fun showCancelLayout() {
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
            val widthDifference = bubble.width - cancel.width
            val heightDifference = bubble.height - cancel.height
            cancelSpring.endValue = 1.5
            springBubble(cancel.x.toDouble() - (widthDifference/2), cancel.y.toDouble() - (heightDifference/2))

        } else {
            cancelSpring.endValue = 1.0
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
            background.scaleY = cancelLayoutSpring.currentValue.toFloat()
            background.translationY = (-1000 * (cancelLayoutSpring.currentValue.toFloat() - 1))
        }
    }

    private inner class CardSpringListener : SimpleSpringListener() {
        override fun onSpringUpdate(spring: Spring?) {
            cardView.scaleY = cardSpring.currentValue.toFloat()
            cardView.scaleX = cardSpring.currentValue.toFloat()
            cardView.alpha = cardSpring.currentValue.toFloat()
            //cardView.translationY = ((-(cardView.height.toFloat()/2)) * (1 - cardSpring.currentValue.toFloat()))
        }
    }
}