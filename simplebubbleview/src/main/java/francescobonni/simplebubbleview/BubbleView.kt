package francescobonni.simplebubbleview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.bubble_layout.view.*
import org.jetbrains.anko.dip
import kotlin.math.abs
import kotlin.random.Random
import android.view.MotionEvent
import android.view.GestureDetector


internal const val STIFNESS_BUBBLE_ANIMATION = 175f
internal const val STIFNESS_ALPHA_ANIMATION = 40f
class BubbleView : ConstraintLayout {
    private var counter = 0

    private val xAnimForce = SpringForce().apply {
        dampingRatio =  SpringForce.DAMPING_RATIO_LOW_BOUNCY
        stiffness = STIFNESS_BUBBLE_ANIMATION
    }

    private val yAnimForce = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        stiffness = STIFNESS_BUBBLE_ANIMATION
    }

    private val alphaAnimForceCancelLayout = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_LOW
    }

    private val slideYAnimForceCancel = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_LOW
    }

    private val zoomAnimForceCancel = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_MEDIUM
    }

    private val backgroundWindowAnimForce = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_LOW
    }

    private val alphaAnimForceCard = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = STIFNESS_ALPHA_ANIMATION
    }

    private val slideYAnimForceCard = SpringForce().apply {
        dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
        stiffness = SpringForce.STIFFNESS_LOW
    }

    private val statusBarHeight: Int by lazy {
        val rectangle = Rect()
        val window = Utils.getActivity(context)?.window
        window?.let {
            it.decorView.getWindowVisibleDisplayFrame(rectangle)
            val statusBarHeight = rectangle.top
            val contentViewTop = it.findViewById<View>(Window.ID_ANDROID_CONTENT).top
            contentViewTop - statusBarHeight
        } ?: 0
    }

    private val bubbleXOnClick by lazy {
        width.minus(bubble.width).minus(dip(16)).toFloat()
    }
    private val bubbleYOnClick by lazy {
        statusBarHeight.toFloat()
    }
    private val minX by lazy {
        bubble.width.plus(dip(16))
    }
    private val maxX by lazy {
        width.minus(bubble.width).minus(dip(16))
    }
    private val minY by lazy {
        statusBarHeight.plus(dip(16))
    }
    private val maxY by lazy {
        height.minus(bubble.height).minus(dip(64)).div(2)
    }

    private lateinit var xAnimBubble: SpringAnimation
    private lateinit var yAnimBubble: SpringAnimation
    private lateinit var alphaAnimBubbleCard: SpringAnimation
    private lateinit var slideYAnimBubbleCard: SpringAnimation
    private lateinit var slideAnimCancel: SpringAnimation
    private lateinit var alphaAnimCancel: SpringAnimation
    private lateinit var alphaAnimCancelLayout: SpringAnimation
    private lateinit var zoomXAnimCancel: SpringAnimation
    private lateinit var zoomYAnimCancel: SpringAnimation
    private lateinit var alphaAnimWindowBackground: SpringAnimation

    private val gestureDetector: GestureDetector = GestureDetector(context, MyGestureDetector())
    private var bubbleIconId: Int? = null
    private var bubbleFallbackIconId: Int? = null
    private var cancelIconId: Int? = null
    private var cardLayoutId: Int? = null
    private var cardstatus = VisibilityStatus.INVISIBLE
    private var cancelstatus = VisibilityStatus.INVISIBLE
    private var stickToWall = true

    private var dX: Float = 0.toFloat()
    private var dY: Float = 0.toFloat()
    private var cancelYPosition = 0.toFloat()

    private var bubbleOnTouchListener = View.OnTouchListener { view, motionEvent ->
        if(!gestureDetector.onTouchEvent(motionEvent)) {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDownBubble(view, motionEvent)
                }

                MotionEvent.ACTION_UP -> {
                    counter = 0
                    onActionUpBubble()
                }

                MotionEvent.ACTION_MOVE -> {
                    if(counter < 10) counter++
                    if(counter == 10) {
                        onActionMoveBuble(motionEvent)
                    }
                }
                else -> return@OnTouchListener false
            }
        }
        return@OnTouchListener true
    }

    private fun onActionDownBubble(view: View, motionEvent: MotionEvent) {
        dX = view.x - motionEvent.rawX
        dY = view.y - motionEvent.rawY
    }

    private fun onActionUpBubble() {
        if(isBubbleOnCancel()) {
            hideBubble()
        } else {
            stickBubbleToWall()
        }
        hideCancelLayout()
    }

    private fun onActionMoveBuble(motionEvent: MotionEvent) {
        if(cardstatus == VisibilityStatus.VISIBLE) {
            hideCardView()
        }
        val finalXBubble = (motionEvent.rawX + dX)
        val finalYBubble = (motionEvent.rawY + dY)
        xAnimBubble.animateToFinalPosition(finalXBubble)
        yAnimBubble.animateToFinalPosition(finalYBubble)
        if(finalYBubble > this.height/2 && cancelstatus == VisibilityStatus.INVISIBLE) {
            showCancelLayout()
        } else if(finalYBubble < this.height/2 && cancelstatus == VisibilityStatus.VISIBLE) {
            hideCancelLayout()
        }
        magnetToCancel(finalXBubble, finalYBubble)
    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)  {
        initLayout()
        initAnimation()
        initAttributes(context?.theme?.obtainStyledAttributes(attrs, R.styleable.BubbleView, defStyleAttr, 0))
        bubble.setOnTouchListener(bubbleOnTouchListener)
        viewTreeObserver.addOnGlobalLayoutListener {
            initCancelPosition()
            showBubble()
        }
    }

    private fun initLayout() {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.bubble_layout, this, true)
    }

    private fun initAttributes(arrayAttr : TypedArray?) {
        arrayAttr?.let { array ->
            bubbleIconId = array.getResourceId(R.styleable.BubbleView_bubble_icon, R.drawable.bubble_background)
            bubbleFallbackIconId = array.getResourceId(R.styleable.BubbleView_bubble_fallback_icon, R.drawable.bubble_background)
            cancelIconId = array.getResourceId(R.styleable.BubbleView_cancel_icon, R.drawable.cancel_icon_bubble)
            Glide.with(context).load(cancelIconId).into(cancel)
            stickToWall = array.getBoolean(R.styleable.BubbleView_cancel_icon, true)
            if (array.hasValue(R.styleable.BubbleView_card_layout)) {
                cardLayoutId = array.getResourceId(R.styleable.BubbleView_card_layout, 0)
                cardLayoutId?.let {
                    setLayoutPopup(View.inflate(context, it, null))
                }
            }
            if (array.hasValue(R.styleable.BubbleView_bubble_bouncy)) {
                val bouncy = array.getInt(R.styleable.BubbleView_bubble_bouncy, 0).toFloat().div(100)
                xAnimBubble.spring.dampingRatio = bouncy
                yAnimBubble.spring.dampingRatio = bouncy
            }
            if (array.hasValue(R.styleable.BubbleView_bubble_speed)) {
                val speed = array.getInt(R.styleable.BubbleView_bubble_speed, 0).toFloat()
                xAnimBubble.spring.stiffness = speed
                yAnimBubble.spring.stiffness = speed
            }
            if (array.hasValue(R.styleable.BubbleView_cancel_animation_speed)) {
                slideAnimCancel.spring.stiffness = array.getInt(R.styleable.BubbleView_cancel_animation_speed, 0).toFloat()
            }
            if (array.hasValue(R.styleable.BubbleView_card_animation_speed)) {
                slideYAnimBubbleCard.spring.dampingRatio = array.getInt(R.styleable.BubbleView_card_animation_speed, 0).toFloat()
            }
        }
    }

    private fun initAnimation() {
        initBubbleAnimation()
        initAnimationCard()
        initAnimationCancel()
        initBackgroundWindowCard()
    }

    private fun initBubbleAnimation() {
        yAnimBubble = SpringAnimation(bubble, DynamicAnimation.Y).apply {
            spring = yAnimForce
        }
        xAnimBubble = SpringAnimation(bubble, DynamicAnimation.X).apply {
            spring = xAnimForce
        }
    }

    private fun initAnimationCard() {
        alphaAnimBubbleCard = SpringAnimation(cardView, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForceCard
        }

        slideYAnimBubbleCard = SpringAnimation(cardView, DynamicAnimation.Y).apply {
            spring = slideYAnimForceCard
        }
    }

    private fun initBackgroundWindowCard() {
        alphaAnimWindowBackground = SpringAnimation(window_background, DynamicAnimation.ALPHA).apply {
            spring = backgroundWindowAnimForce
        }
    }

    private fun initAnimationCancel() {
        slideAnimCancel = SpringAnimation(cancel, DynamicAnimation.Y).apply {
            spring = slideYAnimForceCancel
        }

        alphaAnimCancel  = SpringAnimation(cancel, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForceCancelLayout
        }

        alphaAnimCancelLayout = SpringAnimation(cancel_background, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForceCancelLayout
        }

        zoomXAnimCancel = SpringAnimation(cancel, DynamicAnimation.SCALE_X).apply {
            spring = zoomAnimForceCancel
        }

        zoomYAnimCancel = SpringAnimation(cancel, DynamicAnimation.SCALE_Y).apply {
            spring = zoomAnimForceCancel
        }
    }

    private fun initCancelPosition() {
        cancelYPosition = cancel.y
        slideAnimCancel.animateToFinalPosition(height.toFloat())
    }

    private fun showBubble() {
        loadBubble(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                showBubbleAnimation()
                return false
            }
        })
    }

    private fun loadBubble(requestListener: RequestListener<Drawable>) {
        bubbleIconId?.let {
            GlideApp.with(context).load(it).error(bubbleFallbackIconId ?: R.drawable.bubble_background).addListener(requestListener).into(bubble)
        } ?: showBubbleAnimation()
    }

    private fun showBubbleAnimation() {
        val randPositionX = Random.nextInt(minX, maxX)
        val randPositionY = Random.nextInt(minY, maxY)
        xAnimBubble.animateToFinalPosition(randPositionX.toFloat())
        yAnimBubble.animateToFinalPosition(randPositionY.toFloat())
    }

    private fun bubbleClicked() : Boolean {
        xAnimBubble.animateToFinalPosition(bubbleXOnClick)
        yAnimBubble.animateToFinalPosition(bubbleYOnClick)
        toogleCardView()
        return true
    }

    private fun toogleCardView() {
        if(cardstatus == VisibilityStatus.INVISIBLE) {
            showCardView()
        } else {
            hideCardView()
        }
    }

    private fun showCardView() {
        cardstatus = VisibilityStatus.VISIBLE
        alphaAnimBubbleCard.animateToFinalPosition(1f)
        slideYAnimBubbleCard.animateToFinalPosition(dip(35).plus(bubble.height).toFloat())
        alphaAnimWindowBackground.animateToFinalPosition(1f)
    }

    private fun hideCardView() {
        cardstatus = VisibilityStatus.INVISIBLE
        alphaAnimBubbleCard.animateToFinalPosition(0f)
        slideYAnimBubbleCard.animateToFinalPosition(height.toFloat())
        alphaAnimWindowBackground.animateToFinalPosition(0f)
    }

    private fun showCancelLayout() {
        cancelstatus = VisibilityStatus.VISIBLE
        slideAnimCancel.animateToFinalPosition(cancelYPosition)
        alphaAnimCancelLayout.animateToFinalPosition(1f)
        alphaAnimCancel.animateToFinalPosition(1f)
    }

    private fun hideCancelLayout() {
        cancelstatus = VisibilityStatus.INVISIBLE
        slideAnimCancel.animateToFinalPosition(height.toFloat())
        alphaAnimCancelLayout.animateToFinalPosition(0f)
        alphaAnimCancel.animateToFinalPosition(0f)
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
        if(cancelstatus == VisibilityStatus.VISIBLE) {
            val differenceX = abs(finalXBubble - cancel.x)
            val differenceY = abs(finalYBubble - cancel.y)
            if (differenceX + differenceY < dip(72)) {
                val widthDifference = bubble.width - cancel.width
                val heightDifference = bubble.height - cancel.height
                yAnimBubble.animateToFinalPosition(cancel.y - (heightDifference / 2))
                xAnimBubble.animateToFinalPosition(cancel.x - (widthDifference / 2))
                zoomXAnimCancel.animateToFinalPosition(1.5f)
                zoomYAnimCancel.animateToFinalPosition(1.5f)
            } else {
                zoomXAnimCancel.animateToFinalPosition(1.0f)
                zoomYAnimCancel.animateToFinalPosition(1.0f)
            }
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
        return this
    }

    enum class VisibilityStatus{
        VISIBLE, INVISIBLE
    }

    private inner class MyGestureDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            bubbleClicked()
            return true
        }
    }
}