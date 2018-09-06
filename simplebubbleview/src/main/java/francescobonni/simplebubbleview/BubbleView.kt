package francescobonni.simplebubbleview

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.util.AttributeSet
import android.support.v7.widget.CardView
import android.view.View
import android.view.ViewManager
import android.widget.ImageView
import org.jetbrains.anko.*
import org.jetbrains.anko.cardview.v7.cardView
import org.jetbrains.anko.constraint.layout.constraintLayout


class BubbleView : ConstraintLayout {

    private lateinit var background: ImageView
    private lateinit var bubble: ImageView
    private lateinit var cancel: ImageView
    private lateinit var cardView: CardView


    constructor(context: Context?) : super(context) {
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs){
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
       bubble()
        
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
                    visibility = View.GONE
                }.lparams(width = dip(48), height = dip(48)) {
                    topToTop = Ids.background
                    endToEnd = Ids.background
                    startToStart = Ids.background
                    bottomToBottom = Ids.background
                }

                cardView {
                    id = Ids.card_view
                    visibility = View.GONE
                }.lparams(width = wrapContent, height = wrapContent) {
                    topToTop = ConstraintSet.PARENT_ID
                    startToStart = ConstraintSet.PARENT_ID
                    endToEnd = ConstraintSet.PARENT_ID
                    bottomToBottom = ConstraintSet.PARENT_ID
                }
            }

    private object Ids {
        val background = View.generateViewId()
        val bubble = View.generateViewId()
        val cancel = View.generateViewId()
        val card_view = View.generateViewId()
    }
}