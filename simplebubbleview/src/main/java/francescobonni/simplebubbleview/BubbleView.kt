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
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.bubble_layout.view.*
import org.jetbrains.anko.dip
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.random.Random


internal const val STIFNESS_BUBBLE_ANIMATION = 175f
internal const val STIFNESS_BUBBLE_ANIMATION_ON_DRAG = 12_000f
internal const val STIFNESS_ALPHA_ANIMATION = 40f
class BubbleView : ConstraintLayout {
    private val DIP_16:Int by lazy {
        dip(16)
    }
    private val xAnimForce by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
            stiffness = STIFNESS_BUBBLE_ANIMATION
        }
    }

    private val yAnimForce by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
            stiffness = STIFNESS_BUBBLE_ANIMATION
        }
    }

    private val alphaAnimForceCancelLayout by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    private val slideYAnimForceCancel by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    private val zoomAnimForceCancel by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_MEDIUM
        }
    }

    private val backgroundWindowAnimForce by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    private val alphaAnimForceCustomLayout by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = STIFNESS_ALPHA_ANIMATION
        }
    }

    private val slideYAnimForceCustomLayout by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }
    }

    private val alphaAnimForceArrow by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = STIFNESS_ALPHA_ANIMATION
        }
    }

    private val slideYAnimForceArrow by lazy {
        SpringForce().apply {
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
            stiffness = SpringForce.STIFFNESS_LOW
        }
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

    private val navigationBarHeight: Int by lazy {
        if (Utils.getNavigationBarSize(context) > 0) {
            val resources = context.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        } else {
            Utils.getNavigationBarSize(context)
        }
    }

    private val bubbleXOnClick by lazy {
        customLayout.x.plus(customLayout.width.minus(bubble.width).minus(dip(16)))
    }

    private val bubbleYOnClick by lazy {
        var value = height.minus(customLayout.height.plus(bubble.height).plus(arrow.height)).toFloat()
        if (attachedToRoot) value = value.minus(navigationBarHeight)
        return@lazy if (value > statusBarHeight.toFloat() && customLayoutPosition != 1) {
            when (customLayoutPosition) {
                -1 -> value
                else -> value/2
            }
        } else {
            if (attachedToRoot) {
                statusBarHeight.plus(DIP_16).toFloat()
            } else {
                DIP_16.toFloat()
            }
        }
    }

    private val minX by lazy {
        bubble.width.plus(DIP_16)
    }

    private val maxX by lazy {
        width.minus(bubble.width).minus(DIP_16)
    }

    private val minY by lazy {
        statusBarHeight.plus(DIP_16)
    }

    private val maxY by lazy {
        height.minus(bubble.height).minus(dip(64)).div(2)
    }

    private lateinit var xAnimBubble: SpringAnimation
    private lateinit var yAnimBubble: SpringAnimation
    private lateinit var alphaAnimBubbleCustomLayout: SpringAnimation
    private lateinit var slideYAnimBubbleCustomLayout: SpringAnimation
    private lateinit var alphaAnimBubbleArrow: SpringAnimation
    private lateinit var slideYAnimBubbleArrow: SpringAnimation
    private lateinit var slideAnimCancel: SpringAnimation
    private lateinit var alphaAnimCancel: SpringAnimation
    private lateinit var alphaAnimCancelLayout: SpringAnimation
    private lateinit var zoomXAnimCancel: SpringAnimation
    private lateinit var zoomYAnimCancel: SpringAnimation
    private lateinit var alphaAnimWindowBackground: SpringAnimation
    private var stifnessOnDragBubbleAnimation = STIFNESS_BUBBLE_ANIMATION_ON_DRAG

    private val gestureDetector: GestureDetector = GestureDetector(context, MyGestureDetector())
    private var bubbleIconId: Int? = null
    private var bubbleFallbackIconId: Int? = null
    private var cancelIconId: Int? = null
    private var cardChildLayoutId: Int? = null
    private var cardstatus = VisibilityStatus.INVISIBLE
    private var cancelstatus = VisibilityStatus.INVISIBLE
    private var stickToWall = true
    private var show = false

    private var dX: Float = 0f
    private var dY: Float = 0f
    private var savedBubbleXOnclick: Float = 0f
    private var savedBubbleYOnclick: Float = 0f
    private var cancelYPosition = 0f
    private var counterMoveCalled = 0
    private var attachedToRoot = false
    private var arrayAttr: TypedArray? = null
    private lateinit var customLayout: View
    private var customLayoutPosition: Int? = 0

    private val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        initCancelPosition()
        shrinkCardLayoutIfNeed()
        removeOnGlobalLayoutListener()
        customLayout.y = height.toFloat()
        arrow.y = height.toFloat()
        if(show) {
            showBubble()
        }
    }

    private var bubbleOnTouchListener = View.OnTouchListener { view, motionEvent ->
        if (!gestureDetector.onTouchEvent(motionEvent)) {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDownBubble(view, motionEvent)
                }

                MotionEvent.ACTION_UP -> {
                    if (counterMoveCalled == 10) {
                        xAnimBubble.spring.stiffness = STIFNESS_BUBBLE_ANIMATION
                        yAnimBubble.spring.stiffness = STIFNESS_BUBBLE_ANIMATION
                        onActionUpBubble()
                    }
                    counterMoveCalled = 0
                }

                MotionEvent.ACTION_MOVE -> {
                    if (counterMoveCalled < 10) counterMoveCalled++
                    if (counterMoveCalled == 10) {
                        onActionMoveBubble(motionEvent)
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
        if (isBubbleOnCancel()) {
            hideBubble()
        } else {
            stickBubbleToWall()
        }
        hideCancelLayout()
    }

    private fun onActionMoveBubble(motionEvent: MotionEvent) {
        if (cardstatus == VisibilityStatus.VISIBLE) {
            hideCardView()
        }
        xAnimBubble.spring.stiffness = stifnessOnDragBubbleAnimation
        yAnimBubble.spring.stiffness = stifnessOnDragBubbleAnimation
        val finalXBubble = (motionEvent.rawX + dX)
        val finalYBubble = (motionEvent.rawY + dY)
        if (finalYBubble > this.height / 2 && cancelstatus == VisibilityStatus.INVISIBLE) {
            showCancelLayout()
        } else if (finalYBubble < this.height / 2 && cancelstatus == VisibilityStatus.VISIBLE) {
            hideCancelLayout()
        }
        if(!magnetToCancel(finalXBubble, finalYBubble)) {
            xAnimBubble.animateToFinalPosition(finalXBubble)
            yAnimBubble.animateToFinalPosition(finalYBubble)
        }

    }

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initLayout()
        arrayAttr = context?.theme?.obtainStyledAttributes(attrs, R.styleable.BubbleView, defStyleAttr, 0)
    }

    private fun shrinkCardLayoutIfNeed() {
        val params = customLayout.layoutParams
        var value = height.minus(customLayout.height.plus(bubble.height).plus(DIP_16))
        if(attachedToRoot) value = value.plus(navigationBarHeight)

        if ((attachedToRoot && value < statusBarHeight) || value < 0) {
            params.height = customLayout.height.minus(value.absoluteValue)
        }

        customLayout.layoutParams = params
    }

    private fun initLayout() {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.bubble_layout, this, true)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initAttributesLayout()
        customLayout = findViewById(cardChildLayoutId!!)
        initAnimation()
        initAttributesAnimation()
        bubble.setOnTouchListener(bubbleOnTouchListener)
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    private fun initAnimation() {
        initBubbleAnimation()
        initAnimationCard()
        initAnimationCancel()
        initBackgroundWindowCard()
    }

    private fun initAttributesLayout() {
        arrayAttr?.let { array ->
            if (array.hasValue(R.styleable.BubbleView_bubble_icon)) {
                bubbleIconId = array.getResourceId(R.styleable.BubbleView_bubble_icon, R.drawable.bubble_background)
            }
            if (array.hasValue(R.styleable.BubbleView_bubble_icon)) {
                bubbleFallbackIconId = array.getResourceId(R.styleable.BubbleView_bubble_fallback_icon, R.drawable.bubble_background)
            }
            if (array.hasValue(R.styleable.BubbleView_bubble_icon)) {
                cancelIconId = array.getResourceId(R.styleable.BubbleView_cancel_icon, R.drawable.cancel_icon_bubble)
                Glide.with(context).load(cancelIconId).into(cancel)
            }
            stickToWall = array.getBoolean(R.styleable.BubbleView_cancel_icon, true)
            show = array.getBoolean(R.styleable.BubbleView_show, false)
            if (array.hasValue(R.styleable.BubbleView_child_layout)) {
                cardChildLayoutId = array.getResourceId(R.styleable.BubbleView_child_layout, 0)
            }
        }
    }

    private fun initAttributesAnimation() {
        arrayAttr?.let { array ->
            if (array.hasValue(R.styleable.BubbleView_bubble_bouncy)) {
                val bouncy = array.getFloat(R.styleable.BubbleView_bubble_bouncy, 0f).div(100)
                xAnimBubble.spring.dampingRatio = bouncy
                yAnimBubble.spring.dampingRatio = bouncy
            }
            if (array.hasValue(R.styleable.BubbleView_bubble_speed)) {
                val speed = array.getFloat(R.styleable.BubbleView_bubble_speed, 0f)
                stifnessOnDragBubbleAnimation = speed
            }
            if (array.hasValue(R.styleable.BubbleView_cancel_animation_speed)) {
                slideAnimCancel.spring.stiffness = array.getFloat(R.styleable.BubbleView_cancel_animation_speed, 0f)
            }
            if (array.hasValue(R.styleable.BubbleView_child_layout_animation_speed)) {
                val speed = array.getFloat(R.styleable.BubbleView_child_layout_animation_speed, 0f)
                slideYAnimBubbleCustomLayout.spring.stiffness = speed
                alphaAnimBubbleCustomLayout.spring.stiffness = speed
                slideYAnimBubbleArrow.spring.stiffness = speed
                slideYAnimBubbleArrow.spring.stiffness = speed
            }
            customLayoutPosition = array.getInt(R.styleable.BubbleView_child_layout_anchor, 0)
        }
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
        alphaAnimBubbleCustomLayout = SpringAnimation(customLayout, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForceCustomLayout
        }

        slideYAnimBubbleCustomLayout = SpringAnimation(customLayout, DynamicAnimation.Y).apply {
            spring = slideYAnimForceCustomLayout
        }

        alphaAnimBubbleArrow= SpringAnimation(arrow, DynamicAnimation.ALPHA).apply {
            spring = alphaAnimForceArrow
        }

        slideYAnimBubbleArrow = SpringAnimation(arrow, DynamicAnimation.Y).apply {
            spring = slideYAnimForceArrow
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

        alphaAnimCancel = SpringAnimation(cancel, DynamicAnimation.ALPHA).apply {
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

    private fun removeOnGlobalLayoutListener() {
        viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    private fun loadBubble(requestListener: RequestListener<Drawable>) {
        bubbleIconId?.let {
            Glide.with(context).load(it).apply(RequestOptions().error(bubbleFallbackIconId
                    ?: R.drawable.bubble_background))
                    .apply(RequestOptions.circleCropTransform()).addListener(requestListener).into(bubble)
        } ?: showBubbleAnimation()
    }

    private fun showBubbleAnimation() {
        bubble.alpha = 1f
        val randPositionX = Random.nextInt(minX, maxX)
        val randPositionY = Random.nextInt(minY, maxY)
        xAnimBubble.animateToFinalPosition(randPositionX.toFloat())
        yAnimBubble.animateToFinalPosition(randPositionY.toFloat())
    }

    private fun bubbleClicked(): Boolean {
        xAnimBubble.spring.stiffness = STIFNESS_BUBBLE_ANIMATION
        yAnimBubble.spring.stiffness = STIFNESS_BUBBLE_ANIMATION
        toogleCardView()
        return true
    }

    private fun toogleCardView() {
        if (cardstatus == VisibilityStatus.INVISIBLE) {
            savedBubbleXOnclick = bubble.x
            savedBubbleYOnclick = bubble.y
            xAnimBubble.animateToFinalPosition(bubbleXOnClick)
            yAnimBubble.animateToFinalPosition(bubbleYOnClick)
            showCardView()
        } else {
            xAnimBubble.animateToFinalPosition(savedBubbleXOnclick)
            yAnimBubble.animateToFinalPosition(savedBubbleYOnclick)
            hideCardView()
        }
    }

    private fun showCardView() {
        cardstatus = VisibilityStatus.VISIBLE
        arrow.x = bubbleXOnClick
        customLayout.visibility = View.VISIBLE
        alphaAnimBubbleCustomLayout.animateToFinalPosition(1f)
        alphaAnimBubbleArrow.animateToFinalPosition(1f)
        slideYAnimBubbleCustomLayout.animateToFinalPosition(bubbleYOnClick.plus(bubble.height).plus(arrow.height))
        slideYAnimBubbleArrow.animateToFinalPosition(bubbleYOnClick.plus(bubble.height))
        alphaAnimWindowBackground.animateToFinalPosition(1f)
    }

    private fun hideCardView() {
        cardstatus = VisibilityStatus.INVISIBLE
        alphaAnimBubbleCustomLayout.animateToFinalPosition(0f)
        alphaAnimBubbleCustomLayout.addEndListener { animation, canceled, value, velocity ->
            customLayout.visibility = View.INVISIBLE
        }
        alphaAnimBubbleArrow.animateToFinalPosition(0f)
        slideYAnimBubbleCustomLayout.animateToFinalPosition(height.toFloat())
        slideYAnimBubbleArrow.animateToFinalPosition(height.toFloat())
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

    private fun isBubbleOnCancel(): Boolean {
        val isOnCancelX = bubble.x >= cancel.x && bubble.x < cancel.x + cancel.width
        val isOnCancelY = bubble.y >= cancel.y && bubble.y < cancel.y + cancel.height
        return isOnCancelX && isOnCancelY
    }

    private fun hideBubble() {
        bubble.x = 0f
        bubble.y = 0f
        bubble.visibility = View.INVISIBLE
    }

    private fun stickBubbleToWall() {
        val middle = this.width / 2
        val nearestXWall = (if (bubble.x >= middle) (this.width - bubble.width) - DIP_16 else DIP_16).toFloat()
        xAnimBubble.animateToFinalPosition(nearestXWall)
        yAnimBubble.animateToFinalPosition(bubble.y)
    }

    private fun magnetToCancel(finalXBubble: Float, finalYBubble: Float) : Boolean {
        if (cancelstatus == VisibilityStatus.VISIBLE) {
            val differenceX = abs(finalXBubble - cancel.x)
            val differenceY = abs(finalYBubble - cancel.y)
            return if (differenceX + differenceY < dip(72)) {
                val widthDifference = bubble.width - cancel.width
                val heightDifference = bubble.height - cancel.height
                yAnimBubble.animateToFinalPosition(cancel.y - (heightDifference / 2))
                xAnimBubble.animateToFinalPosition(cancel.x - (widthDifference / 2))
                zoomXAnimCancel.animateToFinalPosition(1.5f)
                zoomYAnimCancel.animateToFinalPosition(1.5f)
                true
            } else {
                zoomXAnimCancel.animateToFinalPosition(1.0f)
                zoomYAnimCancel.animateToFinalPosition(1.0f)
                false
            }
        }
        return false
    }

    fun setCancelImage(resourceId: Int): BubbleView {
        Glide.with(context).load(resourceId).into(cancel)
        return this
    }

    fun setCancelImage(drawable: Drawable): BubbleView {
        Glide.with(context).load(drawable).into(cancel)
        return this
    }

    fun setCancelImage(url: String): BubbleView {
        Glide.with(context).load(url).into(cancel)
        return this
    }

    fun setBubbleImage(resourceId: Int): BubbleView {
        Glide.with(context).load(resourceId).into(bubble)
        return this
    }

    fun setBubbleImage(drawable: Drawable): BubbleView {
        Glide.with(context).load(drawable).apply(RequestOptions.circleCropTransform()).into(bubble)
        return this
    }

    fun setBubbleImage(url: String): BubbleView {
        Glide.with(context).load(url).apply(RequestOptions.circleCropTransform()).into(bubble)
        return this
    }

    fun setFallbackBubbleImage(resourceId: Int): BubbleView {
        bubbleFallbackIconId = resourceId
        return this
    }

    fun setBubbleBouncy(bouncy: Float): BubbleView {
        xAnimBubble.spring.dampingRatio = bouncy
        yAnimBubble.spring.dampingRatio = bouncy
        return this
    }

    fun setBubbleSpeed(speed: Float): BubbleView {
        xAnimBubble.spring.stiffness = speed
        yAnimBubble.spring.stiffness = speed
        return this
    }

    fun setCustomChildLayoutAnimationSpeed(speed: Float): BubbleView {
        alphaAnimBubbleCustomLayout.spring.stiffness = speed
        slideYAnimBubbleCustomLayout.spring.stiffness = speed
        return this
    }

    fun setCancelAnimationSpeed(speed: Float): BubbleView {
        alphaAnimCancelLayout.spring.stiffness = speed
        return this
    }

    fun setAttachedViewRoot(boolean: Boolean): BubbleView {
        attachedToRoot = boolean
        return this
    }

    fun show() {
        showBubble()
    }

    fun isBubbleShown() : Boolean = bubble.visibility == View.VISIBLE

    fun hide() {
        hideBubble()
        hideCardView()
        hideCancelLayout()
    }

    enum class VisibilityStatus {
        VISIBLE, INVISIBLE
    }

    private inner class MyGestureDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            bubbleClicked()
            return true
        }
    }
}