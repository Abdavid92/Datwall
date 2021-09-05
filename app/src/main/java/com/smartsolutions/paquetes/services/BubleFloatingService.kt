package com.smartsolutions.paquetes.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
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
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.databinding.BubbleCloseFloatingLayoutBinding
import com.smartsolutions.paquetes.databinding.BubbleFloatingLayoutBinding
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
import kotlinx.coroutines.flow.collect
import java.lang.RuntimeException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class BubbleFloatingService : Service(), CoroutineScope {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var iconManager: IIconManager

    @Inject
    lateinit var networkUsageManager: NetworkUsageManager

    @Inject
    lateinit var networkUsageUtils: NetworkUsageUtils

    lateinit var uiHelper: UIHelper

    private lateinit var bubbleBinding: BubbleFloatingLayoutBinding
    private lateinit var closeBinding: BubbleCloseFloatingLayoutBinding

    private lateinit var windowManager: WindowManager
    private val params = getParams(WindowManager.LayoutParams.WRAP_CONTENT)
    private val paramsClose = getParams(WindowManager.LayoutParams.MATCH_PARENT)

    private lateinit var currentMenu: View

    private val receiverChangeAppWatcher = ReceiverChangeAppWatcher()
    private val receiverTickWatcher = ReceiverTickWatcher()


    private var app: App? = null
    private var bitmapIcon: Bitmap? = null
    private var traffic: Traffic = Traffic()
    private var isShowMenu = false
    private var VPN_ENABLED = false

    private var delayTransparency = 0
    private var ticks = 4
    private var lastX = 0
    private var lastY = 0
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0F
    private var initialTouchY: Float = 0F
    private var xMinClose = 0
    private var xMaxClose = 0
    private var yMinClose = 0
    private var yMaxClose = 0
    private var moving = 0


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

        uiHelper = UIHelper(this)

        bubbleBinding = BubbleFloatingLayoutBinding.inflate(layoutInflater)
        closeBinding = BubbleCloseFloatingLayoutBinding.inflate(layoutInflater)

        setOnTouch()
        setViews()

        addView(bubbleBinding.root, params)

        runBlocking(Dispatchers.IO) {
            app = appRepository.get(applicationContext.packageName)
        }
        registerBroadcasts()

        launch {
            this@BubbleFloatingService.dataStore.data.collect {
                VPN_ENABLED = it[PreferencesKeys.ENABLED_FIREWALL] ?: false
            }
        }
    }

    private fun setViews() {
      closeBinding.root.setOnClickListener {
          hideClose()
      }
        closeBinding.imageClose.setImageResource(R.drawable.ic_close_red)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouch() {
        bubbleBinding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    lastX = initialX
                    lastY = initialY
                    setTransparency(false)
                    showClose()
                }

                MotionEvent.ACTION_UP -> {
                    if (isDrawOverClose()){
                       hideBubble()
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
                    windowManager.updateViewLayout(bubbleBinding.root, params)
                }
            }
            delayTransparency = 0
            true
        }

    }

    private fun setTransparency(transparent: Boolean) {
        val duration = 800L
        if (transparent) {
            if (bubbleBinding.root.alpha == 1.0f) {
                bubbleBinding.root.animate().alpha(0.3f).duration = duration
            }
        } else {
            if (bubbleBinding.root.alpha < 1.0f) {
                bubbleBinding.root.animate().alpha(1f).duration = duration
            }
        }
    }

    private fun setTheme() {
        bubbleBinding.linBackgroundBubble.setBackgroundResource(getBackgroundResource())

        val color = uiHelper.getTextColorByTheme()

        bubbleBinding.appValue.setTextColor(color)
        bubbleBinding.unitApp.setTextColor(color)
    }

    private fun getBackgroundResource(): Int{
        val isDark = uiHelper.isUIDarkTheme()
        return if (VPN_ENABLED){
            if (app?.access == true) {
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
        }else {
            if (isDark){
                R.drawable.background_card_dark
            }else {
                R.drawable.background_card_light
            }
        }
    }

    private fun setTraffic(app: App) {
        val period = networkUsageUtils.getTimePeriod(NetworkUsageUtils.PERIOD_TODAY)
        runBlocking {
            traffic = networkUsageManager.getAppUsage(app.uid, period.first, period.second)
        }

        bubbleBinding.appValue.text = "${traffic.totalBytes.getValue().value.toInt()}"
        bubbleBinding.unitApp.text = traffic.totalBytes.getValue().dataUnit.name
    }


    private fun showClose(){
        try {
            closeBinding.imageClose.alpha = 0f
            addView(closeBinding.root, paramsClose)
            closeBinding.imageClose.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    closeBinding.imageClose.animate().setListener(null)
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
            closeBinding.imageClose.alpha = 1f
            closeBinding.imageClose.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    closeBinding.imageClose.animate().setListener(null)
                    try {
                        windowManager.removeView(closeBinding.root)
                    }catch (e: Exception){

                    }
                }
            })
        }catch (e: Exception){

        }
    }

    private fun isDrawOverClose(): Boolean {
        val posBubble = IntArray(2)
        bubbleBinding.root.getLocationOnScreen(posBubble)

        return posBubble[0] in (xMinClose + 1) until xMaxClose && posBubble[1] in (yMinClose + 1) until yMaxClose
    }

    private fun showBubble() {
        params.x = lastX
        params.y = lastY
        updateView(bubbleBinding.root, params)
        bubbleBinding.root.visibility = View.VISIBLE
        bubbleBinding.root.animate().alpha(1f)
    }

    private fun hideBubble() {
        bubbleBinding.root.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                bubbleBinding.root.animate().setListener(null)
                bubbleBinding.root.visibility = View.GONE
            }
        })
    }


    private fun fillLocationClose(){
        val posClose = IntArray(2)
        closeBinding.imageClose.getLocationOnScreen(posClose)
        val width = closeBinding.imageClose.width
        val height = closeBinding.imageClose.height
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
            bubbleBinding.appValue.visibility = View.GONE
            bubbleBinding.unitApp.visibility = View.GONE
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
        }else {
            bubbleBinding.appValue.visibility = View.VISIBLE
            bubbleBinding.unitApp.visibility = View.VISIBLE
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35f, resources.displayMetrics).toInt()
        }

        bubbleBinding.appIcon.layoutParams.height = radius
        bubbleBinding.appIcon.layoutParams.width = radius
    }


    private fun setValuesMenu(menu: View) {
        val iconAppMenu: ImageView = menu.findViewById(R.id.image_app_icon)
        val switchAccess: Switch = menu.findViewById(R.id.switch_access)
        val downloadValue: TextView = menu.findViewById(R.id.value_download_app)
        val uploadValue: TextView = menu.findViewById(R.id.value_upload_app)
        val imageDownload: ImageView = menu.findViewById(R.id.image_download)
        val imageUpload: ImageView = menu.findViewById(R.id.image_upload)

        iconAppMenu.setImageBitmap(bitmapIcon)

        imageDownload.setImageDrawable( uiHelper.getImageResourceByTheme("ic_download"))

        imageUpload.setImageDrawable(uiHelper.getImageResourceByTheme("ic_upload"))

        val color = uiHelper.getTextColorByTheme()

        downloadValue.setTextColor(color)
        uploadValue.setTextColor(color)
        switchAccess.setTextColor(color)
        switchAccess.isChecked = app?.access == true || app?.tempAccess == true
        switchAccess.isEnabled = VPN_ENABLED && app?.access == false

        switchAccess.setOnCheckedChangeListener { _, isChecked ->
            launch {
                app?.let {
                    it.tempAccess = isChecked
                    appRepository.update(it)
                }
            }
        }

        uploadValue.text =
            "${traffic.txBytes.getValue().value} ${traffic.txBytes.getValue().dataUnit.name}"
        downloadValue.text =
            "${traffic.rxBytes.getValue().value} ${traffic.rxBytes.getValue().dataUnit.name}"
    }

    private fun getViewSideMenu(): View {
        val width = getScreenWidth()/2
        val pos = IntArray(2)
        bubbleBinding.root.getLocationOnScreen(pos)
        val x = pos[0]
        return if (x >= width){
            bubbleBinding.menuLeft.root
        }else {
            bubbleBinding.menuRight.root
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
                    showBubble()

                    app = appIntent

                    setTheme()

                    bitmapIcon = iconManager.get(
                        appIntent.packageName,
                        appIntent.version
                    )

                    bubbleBinding.appIcon.setImageBitmap(bitmapIcon)

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
        }
    }

    private fun updateView(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(bubbleBinding.root, params)
        }catch (e: Exception){

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcasts()
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

}