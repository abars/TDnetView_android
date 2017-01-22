package biz.abars.tdnetview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i("","Startup Receiver called");
    	AlarmReceiver.start_timer(context);
    }
}
