package com.smartsolutions.paquetes.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.lang.RuntimeException
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

    @Inject
    lateinit var networkUsageUtils: NetworkUsageUtils

    private lateinit var windowManager: WindowManager
    private val params = getParams(WindowManager.LayoutParams.WRAP_CONTENT)
    private val paramsClose = getParams(WindowManager.LayoutParams.MATCH_PARENT)

    private lateinit var cardBubble: View
    private lateinit var closeView: View
    private lateinit var menuLeft: View
    private lateinit var menuRight: View
    private lateinit var currentMenu: View

    private val receiverChangeAppWatcher = ReceiverChangeAppWatcher()
    private val receiverTickWatcher = ReceiverTickWatcher()

    private lateinit var iconApp: ImageView
    private lateinit var valueApp: TextView
    private lateinit var unitApp: TextView
    private lateinit var background: LinearLayout
    private lateinit var closeImage: ImageView


    private var app: App? = null
    private var bitmapIcon: Bitmap? = null
    private var traffic: Traffic = Traffic()
    private var isShowMenu = false

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

        setOnTouch()
        initializeViews()

        addView(cardBubble, params)

        runBlocking(Dispatchers.IO) {
            app = appRepository.get(applicationContext.packageName)
        }
        registerBroadcasts()
    }


    private fun initializeViews() {
        iconApp = cardBubble.findViewById(R.id.app_icon)
        valueApp = cardBubble.findViewById(R.id.app_value)
        unitApp = cardBubble.findViewById(R.id.unit_app)
        background = cardBubble.findViewById(R.id.lin_background_bubble)
        closeImage = closeView.findViewById(R.id.image_close)
        closeImage.setImageResource(R.drawable.ic_close_red)
        closeView.findViewById<LinearLayout>(R.id.lin_background).setOnClickListener {
            hideClose()
        }
        menuLeft = cardBubble.findViewById(R.id.menu_left)
        menuRight = cardBubble.findViewById(R.id.menu_right)
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
                        if (!isShowMenu){
                            showMenu()
                        }else {
                            hideMenu(currentMenu)
                        }

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

    private fun setTheme() {
        background.setBackgroundResource(getBackgroundResource())

        val color = uiHelper.getTextColorByTheme()

        valueApp.setTextColor(color)
        unitApp.setTextColor(color)
    }

    private fun getBackgroundResource(): Int{
        val isDark = uiHelper.isUIDarkTheme()
        return if (app?.access == true) {
           if (isDark){
               R.drawable.background_green_borderless_card_dark
           }else {
               R.drawable.background_green_borderless_card_light
           }
        } else {
            if (isDark){
                R.drawable.background_red_borderless_card_dark
            }else {
                R.drawable.background_red_borderless_card_light
            }
        }
    }

    private fun setTraffic(app: App) {
        val period = networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_TODAY)
        runBlocking {
            traffic = networkUsageManager.getAppUsage(app.uid, period.first, period.second)
        }

        valueApp.text = "${traffic.totalBytes.getValue().value.toInt()}"
        unitApp.text = traffic.totalBytes.getValue().dataUnit.name
    }


    private fun showClose(){
        try {
            closeImage.alpha = 0f
            addView(closeView, paramsClose)
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
        isShowMenu = true
        currentMenu = getViewSideMenu()
        currentMenu.scaleX = 0f
        currentMenu.scaleY = 0f
        currentMenu.visibility = View.VISIBLE

        setValuesMenu(currentMenu)
        currentMenu.animate().scaleX(1f).scaleY(1f)
        animationMenu(true)
    }


    private fun animationMenu(show: Boolean){
        val radius = if (show) {
            valueApp.visibility = View.GONE
            unitApp.visibility = View.GONE
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
        }else {
            valueApp.visibility = View.VISIBLE
            unitApp.visibility = View.VISIBLE
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35f, resources.displayMetrics).toInt()
        }

        iconApp.layoutParams.height = radius
        iconApp.layoutParams.width = radius
    }


    private fun setValuesMenu(menu: View) {
        val iconAppMenu: ImageView = menu.findViewById(R.id.image_app_icon)
        val switchAccess: Switch = menu.findViewById(R.id.switch_access)
        val downloadValue: TextView = menu.findViewById(R.id.value_download_app)
        val uploadValue: TextView = menu.findViewById(R.id.value_upload_app)
        val backgroundMenu: LinearLayout = menu.findViewById(R.id.lin_background)
        val imageDownload: ImageView = menu.findViewById(R.id.image_download)
        val imageUpload: ImageView = menu.findViewById(R.id.image_upload)

        iconAppMenu.setImageBitmap(bitmapIcon)

        imageDownload.setImageDrawable( uiHelper.getImageResourceByTheme("ic_download"))

        imageUpload.setImageDrawable(uiHelper.getImageResourceByTheme("ic_upload"))

        //backgroundMenu.setBackgroundResource(getBackgroundResource())

        val color = uiHelper.getTextColorByTheme()

        downloadValue.setTextColor(color)
        uploadValue.setTextColor(color)
        switchAccess.setTextColor(color)

        uploadValue.text =
            "${Math.round(traffic.txBytes.getValue().value * 100.0) / 100.0} ${traffic.txBytes.getValue().dataUnit.name}"
        downloadValue.text =
            "${Math.round(traffic.rxBytes.getValue().value * 100.0) / 100.0} ${traffic.rxBytes.getValue().dataUnit.name}"
    }

    private fun getViewSideMenu(): View {
        val width = getScreenWidth()/2
        val pos = IntArray(2)
        cardBubble.getLocationOnScreen(pos)
        val x = pos[0]
        return if (x >= width){
            menuLeft
        }else {
            menuRight
        }
    }

    private fun hideMenu(view: View){
        isShowMenu = false
        view.animate().scaleX(0f).scaleY(0f).setListener(object : AnimatorListenerAdapter(){
            override fun onAnimationEnd(animation: Animator?) {
                view.animate().setListener(null)
                view.visibility = View.GONE
                animationMenu(false)
            }
        })
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

                    setTheme()

                    bitmapIcon = iconManager.get(
                        appIntent.packageName,
                        appIntent.version
                    )

                    iconApp.setImageBitmap(bitmapIcon)

                    setTraffic(appIntent)
                }

            }
        }

    }

    inner class ReceiverTickWatcher : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (delayTransparency < 3) {
                delayTransparency++
            } else if (delayTransparency == 3 && !isShowMenu) {
                setTransparency(true)
                delayTransparency++
            }

            if (ticks >= 5) {
                ticks = -1
                app?.let {
                    setTheme()
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


    private fun addView(view: View, params: WindowManager.LayoutParams){
        try {
            windowManager.addView(view, params)
        }catch (e: Exception){

            if (e is RuntimeException && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                throw MissingPermissionException(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            }

            throw e
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcasts()
    }

}