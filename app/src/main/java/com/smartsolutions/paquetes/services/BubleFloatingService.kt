package com.smartsolutions.paquetes.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.BubbleCloseFloatingLayoutBinding
import com.smartsolutions.paquetes.databinding.BubbleFloatingLayoutBinding
import com.smartsolutions.paquetes.databinding.BubbleMenuFloatingLayoutBinding
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.uiDataStore
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class BubbleFloatingService : Service(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var iconManager: IIconManager

    @Inject
    lateinit var networkUsageManager: NetworkUsageManager

    @Inject
    lateinit var dateCalendarUtils: DateCalendarUtils

    @Inject
    lateinit var watcher: RxWatcher

    @Inject
    lateinit var firewallHelper: FirewallHelper

    @Inject
    lateinit var bubbleServiceHelper: BubbleServiceHelper

    lateinit var uiHelper: UIHelper

    private var _bubbleBinding: BubbleFloatingLayoutBinding? = null
    private val bubbleBinding
        get() = if (_bubbleBinding == null) {
            _bubbleBinding = BubbleFloatingLayoutBinding.inflate(LayoutInflater.from(this))
            _bubbleBinding!!
        } else {
            _bubbleBinding!!
        }

    private var _closeBinding: BubbleCloseFloatingLayoutBinding? = null
    private val closeBinding
        get() = if (_closeBinding == null) {
            _closeBinding = BubbleCloseFloatingLayoutBinding.inflate(LayoutInflater.from(this))
            _closeBinding!!
        } else {
            _closeBinding!!
        }


    private var currentMenu: BubbleMenuFloatingLayoutBinding? = null

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var paramsClose: WindowManager.LayoutParams

    private var app: App? = null
    private var bitmapIcon: Bitmap? = null
    private var traffic: Traffic = Traffic()
    private var isShowBubble = false
    private var isShowMenu = false
    private var VPN_ENABLED = false

    private var delayTransparency = 0
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
    private var trics = 0


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.MAIN_CHANNEL_ID
            ).apply {
                setSmallIcon(R.drawable.ic_main_notification)
                setContentTitle("Burbuja Flotante")
            }.build()
        )

        windowManager = ContextCompat.getSystemService(this, WindowManager::class.java)
            ?: throw NullPointerException()

        params = getParams(WindowManager.LayoutParams.WRAP_CONTENT, true)
        paramsClose = getParams(android.view.WindowManager.LayoutParams.MATCH_PARENT)

        uiHelper = UIHelper(this)

        setOnTouch()
        setViews()

        addView(bubbleBinding.root, params)

        runBlocking {
            app = appRepository.get(applicationContext.packageName)
        }

        launch {
            firewallHelper.observeFirewallState().collect {
                VPN_ENABLED = it
            }
        }

        launch {
            this@BubbleFloatingService.uiDataStore.data.collect {
                SIZE = BubbleSize.valueOf(
                    it[PreferencesKeys.BUBBLE_SIZE] ?: BubbleSize.SMALL.name
                )
                TRANSPARENCY = it[PreferencesKeys.BUBBLE_TRANSPARENCY] ?: 0.5f
                ALWAYS_SHOW = it[PreferencesKeys.BUBBLE_ALWAYS_SHOW] ?: false
                withContext(Dispatchers.Main) {
                    setTransparency(true, true)
                    updateBubble()
                }
            }
        }

        registerFlows()

        return START_STICKY
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
                    if (isDrawOverClose()) {
                        hideBubble()
                    } else {
                        lastX = params.x
                        lastY = params.y
                        saveLastPosition()
                    }
                    hideClose()
                    if (moving < 10) {
                        if (!isShowMenu) {
                            showMenu()
                        } else {
                            currentMenu?.let {
                                hideMenu(it)
                            }
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
        params.x = lastX
        params.y = lastY
        if (isDontAddView) {
            addView(bubbleBinding.root, params)
        }
        updateView(bubbleBinding.root, params)
        bubbleBinding.root.visibility = View.VISIBLE
        if (!isShowBubble) {
            setTransparency(true, true)
        } else {
            bubbleBinding.root.alpha = 1f
            setTransparency(true)
        }
        isShowBubble = true
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


    private fun updateBubble() {
        if (isShowBubble) {
            setThemeBubble()
            if (!isShowMenu) {
                setSizeBubble(this, bubbleBinding.root, SIZE)
            }
            app?.let {
                setTraffic(it)
            }
            if (isShowMenu) {
                currentMenu?.let {
                    setValuesMenu(it)
                }
            }
        }
    }


    private fun setTransparency(transparent: Boolean, force: Boolean = false) {
        val duration = 800L

        if (transparent) {
            if (bubbleBinding.root.alpha == 1f || force) {
                bubbleBinding.root.animate().alpha(TRANSPARENCY).duration = duration
                bubbleBinding.root.animate().start()
            }
        } else {
            if (bubbleBinding.root.alpha < 1f || force) {
                bubbleBinding.root.animate().alpha(1f).duration = duration
                bubbleBinding.root.animate().start()
            }
        }
    }

    private fun setThemeBubble() {
        bubbleBinding.linBackgroundBubble.setBackgroundResource(getBackgroundResource())

        val color = uiHelper.getTextColorByTheme()

        bubbleBinding.appValue.setTextColor(color)
        bubbleBinding.unitApp.setTextColor(color)
    }

    private fun getBackgroundResource(): Int {
        val isDark = uiHelper.isUIDarkTheme()
        return if (VPN_ENABLED) {
            if (app?.access == true || app?.tempAccess == true) {
                if (isDark) {
                    R.drawable.background_green_borderless_card_dark
                } else {
                    R.drawable.background_green_borderless_card_light
                }
            } else {
                if (isDark) {
                    R.drawable.background_red_borderless_card_dark
                } else {
                    R.drawable.background_red_borderless_card_light
                }
            }
        } else {
            if (isDark) {
                R.drawable.background_card_dark
            } else {
                R.drawable.background_card_light
            }
        }
    }

    private fun setTraffic(app: App) {
        val period = dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_TODAY)
        runBlocking(Dispatchers.Default) {
            traffic = networkUsageManager.getAppUsage(app.uid, period.first, period.second)
        }

        bubbleBinding.appValue.text = "${traffic.totalBytes.getValue().value.toInt()}"
        bubbleBinding.unitApp.text = traffic.totalBytes.getValue().dataUnit.name

        if (!ALWAYS_SHOW && traffic.totalBytes.bytes <= 0) {
            hideBubble()
        } else if (!isShowBubble) {
            showBubble()
        }
    }


    private fun showClose() {
        try {
            closeBinding.imageClose.alpha = 0f
            addView(closeBinding.root, paramsClose)
            closeBinding.imageClose.animate().alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        closeBinding.imageClose.animate().setListener(null)
                        if (xMinClose == 0 && yMinClose == 0) {
                            fillLocationClose()
                        }
                    }
                })
        } catch (e: Exception) {
            hideClose()
        }
    }

    private fun hideClose() {
        try {
            closeBinding.imageClose.alpha = 1f
            closeBinding.imageClose.animate().alpha(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        closeBinding.imageClose.animate().setListener(null)
                        try {
                            windowManager.removeView(closeBinding.root)
                        } catch (e: Exception) {

                        }
                    }
                })
        } catch (e: Exception) {

        }
    }

    private fun isDrawOverClose(): Boolean {
        val posBubble = IntArray(2)
        bubbleBinding.root.getLocationOnScreen(posBubble)

        return posBubble[0] in (xMinClose + 1) until xMaxClose && posBubble[1] in (yMinClose + 1) until yMaxClose
    }


    private fun fillLocationClose() {
        val posClose = IntArray(2)
        closeBinding.imageClose.getLocationOnScreen(posClose)
        val width = closeBinding.imageClose.width
        val height = closeBinding.imageClose.height
        xMinClose = posClose[0] - width
        xMaxClose = posClose[0] + width

        yMinClose = posClose[1] - height
        yMaxClose = posClose[1] + height
    }


    private fun showMenu() {
        isShowMenu = true
        currentMenu = getViewSideMenu().also {
            it.root.scaleX = 0f
            it.root.scaleY = 0f
            it.root.visibility = View.VISIBLE
            setValuesMenu(it)
            it.root.animate().scaleX(1f).scaleY(1f)
        }

        animationMenu(true)
    }

    private fun hideMenu(menu: BubbleMenuFloatingLayoutBinding) {
        isShowMenu = false
        menu.root.animate().scaleX(0f).scaleY(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                menu.root.animate().setListener(null)
                menu.root.visibility = View.GONE
                animationMenu(false)
            }
        })
    }


    private fun animationMenu(show: Boolean) {
        if (show) {
            bubbleBinding.appValue.visibility = View.GONE
            bubbleBinding.unitApp.visibility = View.GONE
            val radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                60f,
                resources.displayMetrics
            ).toInt()
            bubbleBinding.appIcon.layoutParams.height = radius
            bubbleBinding.appIcon.layoutParams.width = radius
        } else {
            bubbleBinding.appValue.visibility = View.VISIBLE
            bubbleBinding.unitApp.visibility = View.VISIBLE
            setSizeBubble(this, bubbleBinding.root, SIZE)
        }
    }

    private fun setValuesMenu(menu: BubbleMenuFloatingLayoutBinding) {

        menu.imageAppIcon.setImageBitmap(bitmapIcon)

        menu.imageDownload.setImageDrawable(uiHelper.getImageResourceByTheme("ic_download"))

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
        val width = getScreenWidth() / 2
        val pos = IntArray(2)
        bubbleBinding.root.getLocationOnScreen(pos)
        val x = pos[0]
        return if (x >= width) {
            bubbleBinding.menuLeft
        } else {
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

                    iconManager.getIcon(
                        appCurrent.packageName,
                        appCurrent.version
                    ) {
                        bitmapIcon = it
                        bubbleBinding.appIcon.setImageBitmap(bitmapIcon)
                    }
                }
            }
        }

        launch {
            appRepository.flow().collect { list ->
                app?.let { app ->
                    this@BubbleFloatingService.app = list.firstOrNull { it == app }
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

                    if (trics > 2) {
                        trics = 0
                        updateBubble()
                    } else {
                        trics++
                    }
                }
            }
        }
    }

    private fun saveLastPosition() {
        launch {
            applicationContext.internalDataStore.edit {
                it[PreferencesKeys.BUBBLE_POSITION_X] = lastX
                it[PreferencesKeys.BUBBLE_POSITION_Y] = lastY
            }
        }
    }


    private fun getParams(hw: Int, withLastPosition: Boolean = false): WindowManager.LayoutParams {
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
        ).apply {
            if (withLastPosition) {
                val position = runBlocking {
                    val x = applicationContext.internalDataStore.data.firstOrNull()
                        ?.get(PreferencesKeys.BUBBLE_POSITION_X)
                    val y = applicationContext.internalDataStore.data.firstOrNull()
                        ?.get(PreferencesKeys.BUBBLE_POSITION_Y)

                    return@runBlocking x to y
                }

                position.first?.let {
                    lastX = it
                    this.x = it
                }
                position.second?.let {
                    lastY = it
                    this.y = it
                }
            }
        }
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

    private fun addView(view: View, params: WindowManager.LayoutParams) {
        isDontAddView = try {
            windowManager.addView(view, params)
            false
        } catch (e: Exception) {
            isShowBubble = false
            true
        }
    }

    private fun updateView(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(bubbleBinding.root, params)
        } catch (e: Exception) {

        }
    }


    override fun onDestroy() {
        job.cancel()
        try {
            windowManager.removeView(bubbleBinding.root)
        } catch (e: Exception) {

        }
        try {
            windowManager.removeView(closeBinding.root)
        } catch (e: Exception) {

        }
        _closeBinding = null
        _bubbleBinding = null
        currentMenu = null

        super.onDestroy()
    }

    enum class BubbleSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    companion object {

        var SIZE = BubbleSize.MEDIUM
        var TRANSPARENCY = 0.5f
        var ALWAYS_SHOW = true

        var isDontAddView = false

        fun setSizeBubble(context: Context, bubble: View, size: BubbleSize) {
            when (size) {
                BubbleSize.SMALL -> setSizes(context, bubble, 30, 10, 8)
                BubbleSize.MEDIUM -> setSizes(context, bubble, 40, 11, 9)
                BubbleSize.LARGE -> setSizes(context, bubble, 50, 12, 10)
            }
        }

        private fun setSizes(
            context: Context,
            bubble: View,
            iconSize: Int,
            textSize: Int,
            subTextSize: Int
        ) {
            val radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                iconSize.toFloat(),
                context.resources.displayMetrics
            ).toInt()

            val appIcon = bubble.findViewById<ImageView>(R.id.app_icon)

            appIcon.layoutParams.height = radius
            appIcon.layoutParams.width = radius

            bubble.findViewById<TextView>(R.id.app_value)
                .setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize.toFloat())

            bubble.findViewById<TextView>(R.id.unit_app)
                .setTextSize(TypedValue.COMPLEX_UNIT_DIP, subTextSize.toFloat())
        }
    }
}