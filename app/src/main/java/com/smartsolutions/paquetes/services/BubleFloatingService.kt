package com.smartsolutions.paquetes.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.NetworkUtils
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@AndroidEntryPoint
class BubbleFloatingService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var iconManager: IIconManager

    @Inject
    lateinit var networkUsageManager: NetworkUsageManager

    @Inject
    lateinit var uiHelper: UIHelper

    private lateinit var windowManager: WindowManager
    private val params = getParams(WindowManager.LayoutParams.WRAP_CONTENT)
    private val paramsClose = getParams(WindowManager.LayoutParams.MATCH_PARENT)
    private val paramsMenu = getParams(WindowManager.LayoutParams.MATCH_PARENT)

    private lateinit var cardBubble: View
    private lateinit var closeView: View
    private lateinit var menuBubble: View

    private val receiverChangeAppWatcher = ReceiverChangeAppWatcher()
    private val receiverTickWatcher = ReceiverTickWatcher()

    private lateinit var iconApp: ImageView
    private lateinit var valueApp: TextView
    private lateinit var unitApp: TextView
    private lateinit var background: LinearLayout
    private lateinit var closeImage: ImageView
    private lateinit var iconAppMenu: ImageView
    private lateinit var switchAccess: Switch
    private lateinit var downloadValue: TextView
    private lateinit var uploadValue: TextView
    private lateinit var backgroundMenu: LinearLayout
    private lateinit var imageUpload: ImageView
    private lateinit var imageDownload: ImageView


    private var app: App? = null

    var delayTransparency = 0
    var ticks = 4
    var initialX: Int = 0
    var initialY: Int = 0
    var initialTouchX: Float = 0F
    var initialTouchY: Float = 0F
    var xMinClose = 0
    var xMaxClose = 0
    var yMinClose = 0
    var yMaxClose = 0
    var moving = 0


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.MAIN_CHANNEL_ID
            ).apply {
                setSmallIcon(R.drawable.ic_bubble_notification)
                setContentTitle("Burbuja Flotante")
            }.build()
        )

        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()

        val layoutInflater = LayoutInflater.from(this)

        windowManager = ContextCompat.getSystemService(this, WindowManager::class.java)
            ?: throw NullPointerException()

        cardBubble = layoutInflater.inflate(R.layout.buble_floating_layout, null, false)
        closeView = layoutInflater.inflate(R.layout.bubble_close_floating_layout, null, false)
        menuBubble = layoutInflater.inflate(R.layout.bubble_menu_floating_layout, null, false)

        setOnTouch()
        initializeViews()

        windowManager.addView(cardBubble, params)

        runBlocking(Dispatchers.IO) {
            app = appRepository.get(applicationContext.packageName)
        }
        registerBroadcasts()
    }


    private fun initializeViews() {
        iconApp = cardBubble.findViewById(R.id.app_icon)
        valueApp = cardBubble.findViewById(R.id.app_value)
        unitApp = cardBubble.findViewById(R.id.unit_app)
        background = cardBubble.findViewById(R.id.lin_background)
        closeImage = closeView.findViewById(R.id.image_close)
        closeImage.setImageResource(R.drawable.ic_close_red)
        closeView.findViewById<LinearLayout>(R.id.lin_background).setOnClickListener {
            hideClose()
        }
        iconAppMenu = menuBubble.findViewById(R.id.image_app_icon)
        switchAccess = menuBubble.findViewById(R.id.switch_access)
        downloadValue = menuBubble.findViewById(R.id.value_download_app)
        uploadValue = menuBubble.findViewById(R.id.value_upload_app)
        backgroundMenu = menuBubble.findViewById(R.id.lin_background)
        menuBubble.findViewById<LinearLayout>(R.id.lin_back).setOnClickListener {
            hideMenu()
        }
        imageDownload = menuBubble.findViewById(R.id.image_download)
        imageUpload = menuBubble.findViewById(R.id.image_upload)
    }


    private fun setOnTouch() {
        cardBubble.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    setTransparency(false)
                    showClose()
                }

                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    if (isDrawOverClose()){
                       Toast.makeText(this, "Close", Toast.LENGTH_SHORT).show()
                    }
                    hideClose()
                    if (moving < 10) {
                        showMenu()
                    }
                    moving = 0
                }

                MotionEvent.ACTION_MOVE -> {
                    moving++
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(cardBubble, params)
                }
            }
            delayTransparency = 0
            true
        }

    }

    private fun setTransparency(transparent: Boolean) {
        val duration = 800L
        if (transparent) {
            if (cardBubble.alpha == 1.0f) {
                cardBubble.animate().alpha(0.3f).duration = duration
            }
        } else {
            if (cardBubble.alpha < 1.0f) {
                cardBubble.animate().alpha(1f).duration = duration
            }
        }
    }

    private fun setTheme(access: Boolean) {
        val isDark = uiHelper.isUIDarkTheme()

        if (!isDark){
            imageUpload.setImageResource(R.drawable.ic_upload_dark)
            imageDownload.setImageResource(R.drawable.ic_download_dark)
        }else {
            imageUpload.setImageResource(R.drawable.ic_upload_light)
            imageDownload.setImageResource(R.drawable.ic_download_light)
        }

        val resource = if (access) {
            if (!isDark) {
                R.drawable.background_green_light_bordeless_card
            } else {
                R.drawable.background_green_dark_bordeless_card
            }
        } else {
            if (!isDark) {
                R.drawable.background_red_light_bordeless_card
            } else {
                R.drawable.background_red_dark_bordeless_card
            }
        }

        background.setBackgroundResource(resource)
        backgroundMenu.setBackgroundResource(resource)

        val color = if (isDark) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        valueApp.setTextColor(color)
        unitApp.setTextColor(color)
        downloadValue.setTextColor(color)
        uploadValue.setTextColor(color)
        switchAccess.setTextColor(color)
    }

    private fun setTraffic(app: App) {
        val period = NetworkUtils.getTimePeriod(NetworkUtils.PERIOD_TODAY)
        val value: Traffic
        runBlocking {
            value = networkUsageManager.getAppUsage(app.uid, period.first, period.second)
        }

        valueApp.text = "${value.totalBytes.getValue().value.toInt()}"
        unitApp.text = value.totalBytes.getValue().dataUnit.name

        uploadValue.text = "${Math.round(value.txBytes.getValue().value * 100.0)/100.0} ${value.txBytes.getValue().dataUnit.name}"
        downloadValue.text = "${Math.round(value.rxBytes.getValue().value * 100.0)/100.0} ${value.rxBytes.getValue().dataUnit.name}"
    }


    private fun showClose(){
        try {
            closeImage.alpha = 0f
            windowManager.addView(closeView, paramsClose)
            closeImage.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    closeImage.animate().setListener(null)
                    if (xMinClose == 0 && yMinClose == 0) {
                        fillLocationClose()
                    }
                }
            })
        }catch (e: Exception){
            hideClose()
        }
    }

    private fun hideClose(){
        try {
            closeImage.alpha = 1f
            closeImage.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    closeImage.animate().setListener(null)
                    try {
                        windowManager.removeView(closeView)
                    }catch (e: Exception){

                    }
                }
            })
        }catch (e: Exception){

        }
    }

    private fun isDrawOverClose(): Boolean {
        val posBubble = IntArray(2)
        cardBubble.getLocationOnScreen(posBubble)

        return posBubble[0] in (xMinClose + 1) until xMaxClose && posBubble[1] in (yMinClose + 1) until yMaxClose
    }


    private fun fillLocationClose(){
        val posClose = IntArray(2)
        closeImage.getLocationOnScreen(posClose)
        val width = closeImage.width
        val height = closeImage.height
        xMinClose = posClose[0] - width
        xMaxClose = posClose[0] + width

        yMinClose = posClose[1] - height
        yMaxClose = posClose[1] + height
    }


    private fun showMenu(){
        try {
            menuBubble.visibility = View.VISIBLE
            menuBubble.scaleX = 0f
            menuBubble.scaleY = 0f
            windowManager.addView(menuBubble, paramsMenu)
            menuBubble.animate().scaleX(1f).scaleY(1f)
        }catch (e: Exception){
            hideMenu()
        }
    }

    private fun hideMenu(){
        try {
            menuBubble.animate().scaleY(0f).scaleX(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        menuBubble.visibility = View.GONE
                        menuBubble.animate().setListener(null)
                        try {
                            windowManager.removeView(menuBubble)
                        }catch (e: Exception){

                        }
                    }
                })
        }catch (e: Exception){

        }
    }


    private fun registerBroadcasts() {

        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiverChangeAppWatcher,
            IntentFilter(Watcher.ACTION_CHANGE_APP_FOREGROUND)
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiverTickWatcher,
            IntentFilter(Watcher.ACTION_TICKTOCK)
        )

    }

    private fun unregisterBroadcasts() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverChangeAppWatcher)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverTickWatcher)
    }

    inner class ReceiverChangeAppWatcher : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                it.getParcelableExtra<App>(Watcher.EXTRA_FOREGROUND_APP)?.let { appIntent ->

                    app = appIntent

                    setTheme(appIntent.access)

                    switchAccess.isChecked = appIntent.access

                    val bitmap = iconManager.get(
                        appIntent.packageName,
                        appIntent.version
                    )

                    iconApp.setImageBitmap(bitmap)
                    iconAppMenu.setImageBitmap(bitmap)

                    setTraffic(appIntent)
                }

            }
        }

    }

    inner class ReceiverTickWatcher : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (delayTransparency < 3) {
                delayTransparency++
            } else if (delayTransparency == 3) {
                setTransparency(true)
                delayTransparency++
            }

            if (ticks >= 5) {
                ticks = -1
                app?.let {
                    setTheme(it.access)
                    setTraffic(it)
                }
            }
            ticks++
        }

    }


    private fun getParams(hw: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            hw,
            hw,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.getCurrentWindowMetrics()
            windowMetrics.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.getDefaultDisplay().getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }
    }

    private fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.getCurrentWindowMetrics()
            windowMetrics.bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.getDefaultDisplay().getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcasts()
    }

}