package francescobonni.sampleapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import francescobonni.simplebubbleview.BubbleView
import kotlinx.android.synthetic.main.activity_config.*

class ConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)
        root.addView(BubbleView(this))
    }
}
