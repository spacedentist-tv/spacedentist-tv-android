package tv.spacedentist.android

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import tv.spacedentist.android.chromecast.SDCastManager
import tv.spacedentist.android.util.SDLogger
import tv.spacedentist.android.view.SDButton
import tv.spacedentist.android.view.SDButtonClickSender

/**
 * The activity class that does everything. Luckily there's not that much to do, but would be nice
 * to refactor and make more testable.
 */
class SDMainActivity : AppCompatActivity(), CastStateListener {

    private val notificationManager: SDNotificationManager
        get() = SDApplication.component.notificationManager

    private val castManager: SDCastManager
        get() = SDApplication.component.castManager

    private val logger: SDLogger
        get() = SDApplication.component.logger

    private lateinit var mDisconnectedScene: Scene
    private lateinit var mChromecastScene: Scene
    private lateinit var mConnectingScene: Scene
    private lateinit var mConnectedScene: Scene

    private var mCurrentScene: Scene? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        logger.d(TAG, "onCreate()")

        castManager.addCastStateListener(this)

        supportActionBar?.let {
            it.setDisplayShowCustomEnabled(true)
            it.setDisplayShowTitleEnabled(false)
            it.setCustomView(R.layout.action_bar_title)
        }

        val sceneRoot = findViewById<ViewGroup>(R.id.scene_root)

        // add the button click listeners
        val buttonClickSender = SDButtonClickSender(castManager)
        sceneRoot.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                // if we're adding the keypad attach the key listeners
                if (child.id == R.id.connected) {
                    SDButton.values().forEach {
                        val buttonView = findViewById<View>(it.resId)
                        buttonView.setOnClickListener(buttonClickSender)
                    }
                }
            }

            override fun onChildViewRemoved(parent: View, child: View) {}
        })

        mDisconnectedScene = Scene.getSceneForLayout(sceneRoot, R.layout.scene_no_chromecast, this)
        mChromecastScene = Scene.getSceneForLayout(sceneRoot, R.layout.scene_chromecast, this)
        mConnectingScene = Scene.getSceneForLayout(sceneRoot, R.layout.scene_connecting, this)
        mConnectedScene = Scene.getSceneForLayout(sceneRoot, R.layout.scene_connected, this)
    }

    override fun onDestroy() {
        super.onDestroy()

        logger.d(TAG, "onDestroy()")

        castManager.removeCastStateListener(this)
    }

    public override fun onResume() {
        super.onResume()
        logger.d(TAG, "onResume()")
        notificationManager.setActivityOpen(true)
        showCorrectView(castManager.currentCastState)
        castManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        logger.d(TAG, "onPause()")
        notificationManager.setActivityOpen(false)
        castManager.onPause()
    }

    private fun setMenuItem(menu: Menu, @IdRes itemId: Int, title: String, visible: Boolean) {
        val menuItem = menu.findItem(itemId)
        menuItem.title = title
        menuItem.isVisible = visible
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)

        setMenuItem(menu, R.id.app_id, BuildConfig.APPLICATION_ID, BuildConfig.DEBUG)
        setMenuItem(menu, R.id.app_flavor, BuildConfig.FLAVOR, BuildConfig.DEBUG)
        setMenuItem(menu, R.id.app_build_type, BuildConfig.BUILD_TYPE, BuildConfig.DEBUG)
        setMenuItem(menu, R.id.app_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", true)

        return true
    }

    override fun onCastStateChanged(state: Int) {
        showCorrectView(state)
    }

    private fun showCorrectView(state: Int) {
        val newScene = when {
            state == CastState.CONNECTING -> mConnectingScene
            state == CastState.CONNECTED -> mConnectedScene
            state != CastState.NO_DEVICES_AVAILABLE -> mChromecastScene
            else -> mDisconnectedScene
        }

        if (newScene != mCurrentScene) {
            mCurrentScene = newScene
            TransitionManager.go(mCurrentScene!!)
        }
    }

    companion object {
        private const val TAG = "SDMainActivity"
    }
}
