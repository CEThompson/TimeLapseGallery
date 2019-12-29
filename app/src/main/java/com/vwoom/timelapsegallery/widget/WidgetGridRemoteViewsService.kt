package com.vwoom.timelapsegallery.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetGridRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetGridRemoteViewsFactory(this.applicationContext, intent)
    }
}