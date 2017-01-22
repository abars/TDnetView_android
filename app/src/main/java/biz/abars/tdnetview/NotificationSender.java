package biz.abars.tdnetview;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class NotificationSender {
    public void send_notification(Context context, Intent intent,String company,String title,String url){    
    	Intent intent2 = new Intent(context, MainActivity.class);
    	intent2.putExtra("url", url);
    	
    	int req_code=(int)SystemClock.elapsedRealtime();	//require unique for put extra
    	if(req_code==0){
    		req_code=1;
    	}
    	PendingIntent pendingIntent = PendingIntent.getActivity(context, req_code, intent2, 0);
     
    	NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    	 
    	int notification_mode=getNotificationMode(context);
    	
    	Notification notification = new NotificationCompat.Builder(context)
    	    .setSmallIcon(R.drawable.notification_144)
    	    .setTicker(title)
    	    .setWhen(System.currentTimeMillis())
    	    .setContentTitle(company)
    	    .setContentText(title)
    	    .setDefaults(notification_mode)		// 音、バイブレート、LEDで通知
    	    .setContentIntent(pendingIntent)	
    	    .setAutoCancel(true)
    	    .build();
     
    	// 古い通知を削除
    	//notificationManager.cancelAll();
    	
    	// 通知
    	int uniq_id=req_code;
    	notificationManager.notify(uniq_id, notification);
    }

	private static int getNotificationMode(Context context){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		int notification_mode = 0;
		try{
			notification_mode=Integer.parseInt(preferences.getString("NOTIFICATION_ACTION","0"));
		}catch(Exception e){
		}
		if(notification_mode==7){
			return Notification.DEFAULT_ALL;
		}
		if(notification_mode==4){
			return Notification.DEFAULT_LIGHTS;
		}
		if(notification_mode==2){
			return Notification.DEFAULT_VIBRATE;
		}
		if(notification_mode==1){
			return Notification.DEFAULT_SOUND;
		}
		return 0;//Notification.DEFAULT_LIGHTS;
	}

}
