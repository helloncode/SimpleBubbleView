package francescobonni.sampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import francescobonni.simplebubbleview.BubbleView
import kotlinx.android.synthetic.main.activity_config.*

class ConfigActivity : AppCompatActivity() {
    private lateinit var bubbleView: BubbleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        bubbleView = BubbleView(this)
        bubbleView.setLayoutPopup(R.layout.popup_bubble_layout)
                .setAttachedViewRoot(true)
        val vg = window.decorView.rootView as ViewGroup
        vg.addView(bubbleView)
    }

    fun showHide(view: View) {
        if(bubbleView.isBubbleShown()) {
            bubbleView.hide()
        } else {
            bubbleView.show()
        }
    }
}
