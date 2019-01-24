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
import java.util.*
import kotlin.math.abs
import android.view.WindowManager
import android.graphics.Point
import org.jetbrains.anko.dip
import androidx.constraintlayout.widget.ConstraintSet




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
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.bubble_layout, this, true)
        background = findViewById(R.id.background)
        bubble = findViewById(R.id.bubble)
        cancel = findViewById(R.id.cancel)
        cardView = findViewById(R.id.cardView)
        arrow = findViewById(R.id.arrow)
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
        springBubble(dip(64).toDouble(),dip(56).toDouble())
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bubbleXSpring.removeListener(MoveSpringListener())
        bubbleYSpring.removeListener(MoveSpringListener())
        cancelSpring.removeListener(CancelSpringListener())
        cancelLayoutSpring.removeListener(CancelLayoutSpringListener())
        cardSpring.removeListener(CardSpringListener())
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
            springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(64).toDouble())
        } else {
            springBubble(((this.width - bubble.width) - dip(16)).toDouble(), dip(64).toDouble())
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
    }
}