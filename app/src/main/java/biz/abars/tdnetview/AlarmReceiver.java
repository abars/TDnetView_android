package biz.abars.tdnetview;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class AlarmReceiver extends BroadcastReceiver {
	private SaveLoad m_save_load = new SaveLoad();
	private Context m_context;
	private Intent m_intent;
	
	private Article current_article=new Article();
	
	private Boolean DEBUG_NOTIFY=false;
	
    public static void start_timer(Context context) {
    	Intent intent = new Intent(context, AlarmReceiver.class);  
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);  

    	if(!isNotificationEnable(context)){
			alarmManager.cancel(sender);
	        return;
		}		

    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    	int interval = 3600000;
		try{
			interval=Integer.parseInt(preferences.getString("NOTIFICATION_INTERVAL","3600000"));
		}catch(Exception e){
		}

    	long firstTime = SystemClock.elapsedRealtime();
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,firstTime, interval, sender);
    }

	private static Boolean isNotificationEnable(Context context){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Boolean preferenceValue = preferences.getBoolean("NOTIFICATION_ENABLE", false);
		return preferenceValue;
	}

	@Override  
    public void onReceive(Context context, Intent intent) {  
        Log.d("AlarmReceiver", "Alarm Received! : " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));  

        int mode=0;
        String query="";
        boolean full=false;

		m_context=context;
		m_intent=intent;

		SharedPreferences pref =context.getSharedPreferences("alarm",context.MODE_PRIVATE);
		if(!m_save_load.load(pref,current_article,false) || DEBUG_NOTIFY){
			current_article.data_list=new ArrayList<String>();
			current_article.url_list=new ArrayList<String>();
			current_article.adapter_id="alarm";
		}

        HttpGetTask task=new HttpGetTask(mode,query,null,this,full,0);
		task.setCache(current_article);
		task.execute();
    }  
    
    public void reload_finish(Article new_article){
		SharedPreferences pref_mark =m_context.getSharedPreferences("mark",m_context.MODE_PRIVATE);
		ArrayList<String> mark_list;
		mark_list=m_save_load.load_mark(pref_mark);

    	for(int i=0;i<new_article.data_list.size();i++){
    		if(current_article.url_list.contains(new_article.url_list.get(i))){
    			continue;
    		}
			Boolean is_marked=mark_list!=null && mark_list.size()>=1 && mark_list.contains(new_article.getCompany(i));
			if(is_marked){
				String company=new_article.getCompany(i);
				String title=new_article.getTitle(i);
				String url=new_article.getUrl(i);
				NotificationSender notif=new NotificationSender();
				notif.send_notification(m_context,m_intent,company,title,url);
			}
    	}
    	
    	//古い記事を削除していく
    	int MAX_N=5000;	//決算集中時期は1000件程度まで膨らむ
    	while(new_article.data_list.size()>=MAX_N){
	    	new_article.data_list.remove(MAX_N-1);
	    	new_article.url_list.remove(MAX_N-1);
    	}

		SharedPreferences pref =m_context.getSharedPreferences("alarm",m_context.MODE_PRIVATE);
		m_save_load.save(pref,new_article);
    }   
}
