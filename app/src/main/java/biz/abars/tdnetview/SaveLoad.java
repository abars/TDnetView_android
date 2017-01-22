package biz.abars.tdnetview;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Context;

public class SaveLoad{
	private Boolean DEBUG_NEW_DATA=false;
	
	public void save_mark(SharedPreferences pref,ArrayList<String> mark){
		SharedPreferences.Editor e = pref.edit();
		JSONArray data_json = new JSONArray(mark);
		e.putString("company", data_json.toString());
		e.commit();
	}

	public ArrayList<String> load_mark(SharedPreferences pref){
		String data_json = pref.getString("company","none");
		ArrayList<String>  mark = json_to_array_list(data_json);
		if(mark==null){
			return new ArrayList<String>();
		}
		return mark;
	}
	
	public void save_search(SharedPreferences pref,ArrayList<String> search){
		SharedPreferences.Editor e = pref.edit();
		JSONArray data_json = new JSONArray(search);
		e.putString("search", data_json.toString());
		e.commit();
	}

	public ArrayList<String> load_search(SharedPreferences pref){
		String data_json = pref.getString("search","none");
		ArrayList<String>  search = json_to_array_list(data_json);
		if(search==null){
			return new ArrayList<String>();
		}
		return search;
	}

	public void save(SharedPreferences pref,Article new_article){
		SharedPreferences.Editor e = pref.edit();

		JSONArray data_json = new JSONArray(new_article.data_list);
		JSONArray url_json = new JSONArray(new_article.url_list);

		e.putString("data", data_json.toString());
		e.putString("url", url_json.toString());
		e.putString("day", get_today());
		e.putString("id", new_article.adapter_id);
		
		e.commit();
	}

	private String get_today(){
		Calendar cal = Calendar.getInstance();
		return ""+cal.get(Calendar.YEAR)+"/"+cal.get(Calendar.MONTH)+1+"/"+cal.get(Calendar.DATE);		
	}
	
	private ArrayList<String> json_to_array_list(String data_json){
		if(data_json.equals("none")){
			return null;
		}
		try{
			JSONArray data_array =new JSONArray(data_json);
			ArrayList<String>  data = new ArrayList<String>();
			for(int i=0;i< data_array.length();i++){
				data.add(data_array.getString(i));
			}
			return data;
		}catch(Exception e){
			return null;
		}
	}
	
	public Boolean load(SharedPreferences pref,Article new_article,Boolean invalidate_by_date){
		String data_json = pref.getString("data","none");
		String url_json = pref.getString("url", "none");
		String day = pref.getString("day", "none");
		String id = pref.getString("id", "none");

		if(!day.equals(get_today()) && invalidate_by_date){
			return false;
		}

		try{
			new_article.data_list = json_to_array_list(data_json);
			new_article.url_list = json_to_array_list(url_json);
			new_article.adapter_id = id;
					
			if(new_article.data_list==null || new_article.url_list==null){
				return false;
			}

			if(DEBUG_NEW_DATA){
				return false;
			}
		
			return true;
		}catch(Exception e){
			return false;
		}
	}
};
