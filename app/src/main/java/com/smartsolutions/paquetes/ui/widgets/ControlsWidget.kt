package com.smartsolutions.paquetes.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.BubbleServiceHelper
import com.smartsolutions.paquetes.helpers.FirewallHelper
import com.smartsolutions.paquetes.helpers.UIHelper
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ControlsWidget : AppWidgetProvider() {

    @Inject
    lateinit var bubbleHelper: Lazy<BubbleServiceHelper>

    @Inject
    lateinit var firewallHelper: Lazy<FirewallHelper>

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, getRemotesViews(context))
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}


    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        var isAction = true

        when (intent?.action) {
            START_BUBBLE -> {
                runBlocking {
                    bubbleHelper.get().startBubble(true)
                }
            }
            STOP_BUBBLE -> {
                runBlocking {
                    bubbleHelper.get().stopBubble(true)
                }
            }
            START_FIREWALL -> {
                runBlocking {
                    firewallHelper.get().startFirewall(true)
                }
            }
            STOP_FIREWALL -> {
                runBlocking {
                    firewallHelper.get().stopFirewall(true)
                }
            }
            else -> {
                isAction = false
            }
        }

        if (isAction) {
            context?.let { ctx ->
                AppWidgetManager.getInstance(ctx)
                    .updateAppWidget(
                        ComponentName(
                            ctx,
                            ControlsWidget::class.java
                        ),
                        getRemotesViews(ctx)
                    )
            }
        }

    }

    private fun getRemotesViews(context: Context): RemoteViews {
        val uiHelper = UIHelper(context)

        val views = if (uiHelper.isUIDarkTheme()) {
            RemoteViews(context.packageName, R.layout.widget_controls_dark)
        } else {
            RemoteViews(context.packageName, R.layout.widget_controls_light)
        }

        val isBubble = runBlocking {
            bubbleHelper.get().bubbleEnabled()
        }

        val isFirewall = runBlocking {
            firewallHelper.get().firewallEnabled()
        }

        views.setImageViewResource(
            R.id.icon_firewall,
            if (isFirewall) {
                R.drawable.firewall_on
            } else {
                R.drawable.firewall_off
            }
        )

        views.setImageViewResource(
            R.id.icon_bubble,
            if (isBubble) {
                R.drawable.bubble_on
            } else {
                R.drawable.bubble_off
            }
        )

        views.setOnClickPendingIntent(
            R.id.lin_firewall,
            getPendingSelfIntent(
                context,
                if (isFirewall) {
                    STOP_FIREWALL
                } else {
                    START_FIREWALL
                }
            )
        )

        views.setOnClickPendingIntent(
            R.id.lin_bubble,
            getPendingSelfIntent(
                context,
                if (isBubble) {
                    STOP_BUBBLE
                } else {
                    START_BUBBLE
                }
            )
        )

        return views
    }


    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(
            context, 0, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    companion object {
        const val START_BUBBLE = "start_bubble"
        const val STOP_BUBBLE = "stop_bubble"
        const val START_FIREWALL = "start_firewall"
        const val STOP_FIREWALL = "stop_firewall"
    }
}