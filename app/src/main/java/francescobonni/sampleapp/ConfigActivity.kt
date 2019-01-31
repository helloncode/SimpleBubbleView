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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        val view = layoutInflater.inflate(R.layout.popup_bubble_layout, null) as BubbleView
        view.setAttachedViewRoot(true)
        val vg = window.decorView.rootView as ViewGroup
        vg.addView(view)
        view.show()
    }
}
