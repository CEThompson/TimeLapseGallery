package com.vwoom.timelapsegallery.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.vwoom.timelapsegallery.utils.InjectorUtils

class WidgetGridRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return InjectorUtils.provideWidgetGridRemotViewsFactory(this.applicationContext, intent)
    }
}