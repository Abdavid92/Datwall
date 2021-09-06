package com.smartsolutions.paquetes.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
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
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.databinding.BubbleCloseFloatingLayoutBinding
import com.smartsolutions.paquetes.databinding.BubbleFloatingLayoutBinding
import com.smartsolutions.paquetes.databinding.BubbleMenuFloatingLayoutBinding
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.uiDataStore
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.lang.RuntimeException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class BubbleFloatingService : Service(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

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

    @Inject
    lateinit var watcher: RxWatcher

    lateinit var uiHelper: UIHelper

    private lateinit var bubbleBinding: BubbleFloatingLayoutBinding
    private lateinit var closeBinding: BubbleCloseFloatingLayoutBinding
    private lateinit var currentMenu: BubbleMenuFloatingLayoutBinding

    private lateinit var windowManager: WindowManager
    private val params = getParams(WindowManager.LayoutParams.WRAP_CONTENT)
    private val paramsClose = getParams(WindowManager.LayoutParams.MATCH_PARENT)

    private var app: App? = null
    private var bitmapIcon: Bitmap? = null
    private var traffic: Traffic = Traffic()
    private var isShowBubble = true
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

    private var SIZE = BubbleSize.MEDIUM
    private var TRANSPARENCY = BubbleTransparency.MEDIUM
    private var ALWAYS_SHOW = true


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
        registerFlows()

        launch {
            this@BubbleFloatingService.dataStore.data.collect {
                VPN_ENABLED = it[PreferencesKeys.ENABLED_FIREWALL] ?: false
            }
        }
        launch {
            this@BubbleFloatingService.uiDataStore.data.collect {
                SIZE = BubbleSize.valueOf(
                    it[PreferencesKeys.BUBBLE_SIZE] ?: BubbleSize.SMALL.name
                )
                TRANSPARENCY = BubbleTransparency.valueOf(
                    it[PreferencesKeys.BUBBLE_TRANSPARENCY] ?: BubbleTransparency.LOW.name
                )
                ALWAYS_SHOW = it[PreferencesKeys.BUBBLE_ALWAYS_SHOW] ?: false
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
                    }else {
                        lastX = params.x
                        lastY = params.y
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


    private fun showBubble() {
        isShowBubble = true
        params.x = lastX
        params.y = lastY
        updateView(bubbleBinding.root, params)
        bubbleBinding.root.visibility = View.VISIBLE
        bubbleBinding.root.alpha = 1f
        setTransparency(true)
    }

    private fun hideBubble() {
        isShowBubble = false
        bubbleBinding.root.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                bubbleBinding.root.animate().setListener(null)
                bubbleBinding.root.visibility = View.GONE
            }
        })
    }


    private fun updateBubble(){
        if (isShowBubble) {
            setThemeBubble()
            if (!isShowMenu) {
                setSizeBubble()
            }
            app?.let {
                setTraffic(it)
            }
            if (isShowMenu) {
                setValuesMenu(currentMenu)
            }
        }
    }


    private fun setTransparency(transparent: Boolean) {
        val duration = 800L

        if (transparent) {
            val value = when (TRANSPARENCY){
                BubbleTransparency.LOW -> 0.3f
                BubbleTransparency.MEDIUM -> 0.5f
                BubbleTransparency.HIGH -> 0.8f
            }

            if (bubbleBinding.root.alpha == 1f){
                bubbleBinding.root.animate().alpha(value).duration = duration
                bubbleBinding.root.animate().start()
            }
        } else {
            if (bubbleBinding.root.alpha < 1f){
                bubbleBinding.root.animate().alpha(1f).duration = duration
                bubbleBinding.root.animate().start()
            }
        }
    }

    private fun setSizeBubble() {
        when (SIZE) {
            BubbleSize.SMALL -> setSizes(30, 10, 8)
            BubbleSize.MEDIUM -> setSizes(40, 11, 9)
            BubbleSize.LARGE -> setSizes(50, 12, 10)
        }
    }

    private fun setThemeBubble() {
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
        runBlocking(Dispatchers.IO) {
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
        currentMenu.root.scaleX = 0f
        currentMenu.root.scaleY = 0f
        currentMenu.root.visibility = View.VISIBLE

        setValuesMenu(currentMenu)
        currentMenu.root.animate().scaleX(1f).scaleY(1f)
        animationMenu(true)
    }

    private fun hideMenu(menu: BubbleMenuFloatingLayoutBinding){
        isShowMenu = false
        menu.root.animate().scaleX(0f).scaleY(0f).setListener(object : AnimatorListenerAdapter(){
            override fun onAnimationEnd(animation: Animator?) {
                menu.root.animate().setListener(null)
                menu.root.visibility = View.GONE
                animationMenu(false)
            }
        })
    }


    private fun animationMenu(show: Boolean){
        if (show) {
            bubbleBinding.appValue.visibility = View.GONE
            bubbleBinding.unitApp.visibility = View.GONE
            val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
            bubbleBinding.appIcon.layoutParams.height = radius
            bubbleBinding.appIcon.layoutParams.width = radius
        }else {
            bubbleBinding.appValue.visibility = View.VISIBLE
            bubbleBinding.unitApp.visibility = View.VISIBLE
            setSizeBubble()
        }
    }

    private fun setValuesMenu(menu: BubbleMenuFloatingLayoutBinding) {

        menu.imageAppIcon.setImageBitmap(bitmapIcon)

        menu.imageDownload.setImageDrawable( uiHelper.getImageResourceByTheme("ic_download"))

        menu.imageUpload.setImageDrawable(uiHelper.getImageResourceByTheme("ic_upload"))

        val color = uiHelper.getTextColorByTheme()

        menu.valueDownloadApp.apply {
            setTextColor(color)
            text =
                "${traffic.rxBytes.getValue().value} ${traffic.rxBytes.getValue().dataUnit.name}"
        }
        menu.valueUploadApp.apply {
            setTextColor(color)
            text =
                "${traffic.txBytes.getValue().value} ${traffic.txBytes.getValue().dataUnit.name}"
        }

        menu.switchAccess.apply {
            setTextColor(color)
            isChecked = app?.access == true || app?.tempAccess == true
            isEnabled = VPN_ENABLED && app?.access == false
            setOnCheckedChangeListener { _, isChecked ->
                launch {
                    app?.let {
                        it.tempAccess = isChecked
                        appRepository.update(it)
                    }
                }
            }
        }
    }

    private fun getViewSideMenu(): BubbleMenuFloatingLayoutBinding {
        val width = getScreenWidth()/2
        val pos = IntArray(2)
        bubbleBinding.root.getLocationOnScreen(pos)
        val x = pos[0]
        return if (x >= width){
            bubbleBinding.menuLeft
        }else {
            bubbleBinding.menuRight
        }
    }

    private fun registerFlows() {
        launch {
            watcher.currentAppFlow.collect {

                withContext(Dispatchers.Main) {
                    val appCurrent = it.first

                    app = appCurrent
                    setTraffic(appCurrent)

                    setThemeBubble()

                    bitmapIcon = iconManager.get(
                        appCurrent.packageName,
                        appCurrent.version
                    )
                    bubbleBinding.appIcon.setImageBitmap(bitmapIcon)

                    if (!ALWAYS_SHOW && traffic.totalBytes.bytes <= 0) {
                        hideBubble()
                    } else if (!isShowBubble) {
                        showBubble()
                    }
                }
            }
        }

        launch {
            watcher.bandWithFlow.collect {
                withContext(Dispatchers.Main) {
                    if (delayTransparency < 3) {
                        delayTransparency++
                    } else if (delayTransparency == 3 && !isShowMenu && isShowBubble) {
                        setTransparency(true)
                        delayTransparency++
                    }

                    updateBubble()
                }
            }
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

    private fun setSizes(iconSize: Int, textSize: Int, subTextSize: Int) {
        val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconSize.toFloat(), resources.displayMetrics).toInt()
        bubbleBinding.appIcon.layoutParams.height = radius
        bubbleBinding.appIcon.layoutParams.width = radius

        bubbleBinding.appValue.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize.toFloat())
        bubbleBinding.unitApp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, subTextSize.toFloat())
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
        job.cancel()
        super.onDestroy()
    }

    enum class BubbleSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    enum class BubbleTransparency {
        LOW,
        MEDIUM,
        HIGH
    }

}