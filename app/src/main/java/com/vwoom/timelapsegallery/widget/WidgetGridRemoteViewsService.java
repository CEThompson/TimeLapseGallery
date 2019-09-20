package com.vwoom.timelapsegallery.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WidgetGridRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetGridRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
