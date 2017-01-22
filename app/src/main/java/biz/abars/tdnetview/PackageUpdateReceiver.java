package biz.abars.tdnetview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PackageUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i("","Package Update Receiver called");
        if(Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) 
        {
        	if(intent.getData().getSchemeSpecificPart().equals(context.getPackageName()))
            {
        		AlarmReceiver.start_timer(context);
            }
        }
    }
}

