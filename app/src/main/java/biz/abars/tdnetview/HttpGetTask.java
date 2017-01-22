package biz.abars.tdnetview;

import android.app.Activity;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLEncoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.net.http.AndroidHttpClient;

import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.util.Log;

// AsyncTaskのサブクラスとして、バックグラウンド処理用のタスクを記述
class HttpGetTask extends AsyncTask<HttpUriRequest, Void, String> {
	private int m_mode;
	private String m_query;
	private Boolean m_full_search_disable;
	private Activity m_activity;
	private AlarmReceiver m_alarm;
	private int m_page;

	private int statusCode;

	private String SEARCH_URL="http://tdnet-search.appspot.com/?query=";
	private String REGX_URL="http://tdnet-search.appspot.com/?mode=regx";

	private int MODE_SEARCH_FROM=2;
	private int MODE_SEARCH=4;
	
	public static final int PAGE_UNIT=100;

	private Article article = new Article();
	
	public HttpGetTask(int mode,String query,Activity activity,AlarmReceiver alarm,Boolean full_search_disable,int page) {

		// 呼び出し元のアクティビティ
		this.m_mode = mode;
		this.m_query = query;
		this.m_activity = activity;
		this.m_alarm = alarm;
		this.m_full_search_disable = full_search_disable;
		this.m_page = page;
	}
	
	public void setCache(Article new_article){
		article=new_article.clone_article();
	}

	protected String download_one_page(String result){
		String url=result;
		String message="";
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Demo AndroidHttpClient");
		try{
			HttpGet request = new HttpGet(url);
			HttpResponse response = httpClient.execute(request);

			if(response.getStatusLine().getStatusCode()==HttpStatus.SC_OK){
				InputStream content = response.getEntity().getContent();
				message = IOUtils.toString(content);
			}else{
				statusCode=response.getStatusLine().getStatusCode();
			}
			
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		return message;
	}
	
	protected Boolean updateRegx(){
		HttpGet request = new HttpGet(REGX_URL);
		
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Demo AndroidHttpClient");

		try {
			HttpResponse response = httpClient.execute(request);
			
			if(response.getStatusLine().getStatusCode()!=HttpStatus.SC_OK){
				statusCode=response.getStatusLine().getStatusCode();
				httpClient.close();
				return false;
			}

			InputStream content = response.getEntity().getContent();
			String  json = IOUtils.toString(content);
			httpClient.close();
			
			if(!TDnetRegx.Update(json)){
				statusCode=-1;
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return true;
	}

	// doInBackground() に、バックグラウンド処理の内容を記述する。
	// ここではAndroidHttpClientによるHTTP GET実行
	protected String doInBackground(HttpUriRequest... request_list) {
		statusCode=0;
		
		//キャンセルされた
		if(isCancelled()){
			Log.i("","cancel in background");
			return "";
		}
		
		//検索の場合
		if(m_mode>=MODE_SEARCH_FROM){
			String encoded_query="";
			try{
				encoded_query=URLEncoder.encode(m_query,"utf-8");
			}catch(Exception e){				
			}
			String full="";
			if(m_mode==MODE_SEARCH && !m_full_search_disable){
				full="&mode=full";
			}
			if(m_page!=0){
				full+="&page="+(m_page+1);
			}
			full+="&page_unit="+PAGE_UNIT;
			return RequestMain(SEARCH_URL+encoded_query+full);
		}
		
		//正規表現をアップデート
		if(TDnetRegx.VERSION==0){
			if(!updateRegx()){
				return "";
			}
		}

		//TDnetの場合
		HttpGet request = new HttpGet(TDnetRegx.TDNET_TOP_URL);

		AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Demo AndroidHttpClient");

		try {
			HttpResponse response = httpClient.execute(request);
			
			if(response.getStatusLine().getStatusCode()!=HttpStatus.SC_OK){
				statusCode=response.getStatusLine().getStatusCode();
				httpClient.close();
				return "";
			}
			
			if(isCancelled()){
				httpClient.close();
				Log.i("","cancel in background tdnet access");
				return "";
			}
			
			String result="";	
			InputStream content = response.getEntity().getContent();
			String  message = IOUtils.toString(content); // Commons IOを用いてInputStream→String変換
			httpClient.close();

			Pattern p = Pattern.compile(TDnetRegx.TDNET_DAY_PAGE_PATTERN);
			Matcher m = p.matcher(message);

			if(m.find()){
				String today=m.group(1);
				String yesterday=GetDayOffsetUrl(today,1);
				if(m_alarm!=null){
					//深夜のデータの取得漏れを防ぐために今日と昨日の記事を取得
					RequestMain(TDnetRegx.TDNET_BASE_URL+yesterday);
					return RequestMain(TDnetRegx.TDNET_BASE_URL+today);
				}
				if(m_mode==1){
					return RequestMain(TDnetRegx.TDNET_BASE_URL+yesterday);
				}
				return RequestMain(TDnetRegx.TDNET_BASE_URL+today);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}

		return "";
	}
	
	private String GetDayOffsetUrl(String result,int offset){
		Calendar cal = Calendar.getInstance();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String from=dateFormat.format(cal.getTime());                        
		cal.add(Calendar.DATE, -offset);
		String to=dateFormat.format(cal.getTime());
		result=result.replace(from,to);
		return result;
	}

	protected String RequestMain(String result){
		String adapter_id=result;
		if(m_alarm!=null){
			adapter_id="alarm";
		}
		
		if(!article.adapter_id.equals(adapter_id) || m_mode>=MODE_SEARCH_FROM){
			Log.i("","invalidate");
			article = new Article();
			article.adapter_id=new String(adapter_id);
		}

		String message = "";

		ArrayList<String> new_adapter= new ArrayList<String>();
		ArrayList<String> new_url_list= new ArrayList<String>();
		
		Boolean cache_hit=false;

		while(true){
			String page=download_one_page(result);
			Pattern p2 = Pattern.compile(TDnetRegx.TDNET_NEXT_PAGE_PATTERN);
			Matcher m2 = p2.matcher(page);
			message+=page;

			if(parsePage(page,new_adapter,new_url_list)){
				cache_hit=true;
				break;
			}

			if(m2.find()){
				result=m2.group(1);
				if(result.equals("")){
					break;
				}
				result=TDnetRegx.TDNET_BASE_URL+result;
			}else{
				break;
			}
		}
		
		article.data_list.addAll(0,new_adapter);
		article.url_list.addAll(0,new_url_list);
		
		if(cache_hit){
			Log.i("","cache_hit");
		}else{
			Log.i("","cache_miss");
		}

		return message;
	}
	
	private Boolean parsePage(String message,ArrayList<String> new_adapter,ArrayList<String> new_url_list){
		Pattern p = Pattern.compile(TDnetRegx.TDNET_TR_PATTERN,Pattern.DOTALL);
		Pattern p2 = Pattern.compile(TDnetRegx.TDNET_TD_PATTERN,Pattern.DOTALL);
		Pattern p3 = Pattern.compile(TDnetRegx.TDNET_CONTENT_PATTERN,Pattern.DOTALL);

		Matcher m = p.matcher(message);

		while(m.find()){
			String row=m.group();
			Matcher m2=p2.matcher(row);

			if(row.indexOf("div align")!=-1){
				continue;
			}

			List<String> list=new ArrayList<String>();        	
			while(m2.find()){
				String col=m2.group(1);
				col=col.replaceAll("\r", "");
				col=col.replaceAll("\n", "");
				list.add(col);
			}

			if(list.size()>=TDnetRegx.TDNET_ID_N){
				String date=list.get(TDnetRegx.TDNET_DATE_ID);
				String code=""+list.get(TDnetRegx.TDNET_COMPANY_CODE_ID);
				if(code==""){
					code="-";
				}else{
					if(code.length()>=4){
						code=code.substring(0, 4);
					}
				}
				String company=list.get(TDnetRegx.TDNET_COMPANY_ID);
				String data=list.get(TDnetRegx.TDNET_DATA_ID);
				String url="";

				date=date.replaceAll(" ", "");
				company=company.replaceAll(" ", "");

				Matcher m3=p3.matcher(data);
				while(m3.find()){
					url=m3.group(1);
					data=m3.group(2);
				}
				
				String br="<br/>";//"\n"
				
				data=data.replaceAll(" ", "");
				data=data.replaceAll("&amp;", " ");
				
				if(m_mode==MODE_SEARCH && !m_full_search_disable){
					String main_text=list.get(TDnetRegx.TDNET_DATA_ID+1);
					data+=br+br+main_text;
				}

				String mes=date+"  "+code+"  "+company+br+data+br;
				
				if(url==""){
					continue;
				}				
				if(article.url_list.contains(url)){
					return true;
				}
				
				new_adapter.add(mes);
				new_url_list.add(url);
			}
		}	
		
		return false;
	}

	// onPostExecute() に、バックグラウンド処理完了時の処理を記述する。
	// ここでは、HTTPレスポンスボディとして取得した文字列のTextViewへの貼り付け
	protected void onPostExecute(String message) {
		if(isCancelled()){
			return;
		}
		
		if(m_alarm!=null){
			m_alarm.reload_finish(article);
		}
		
		if(m_activity==null){
			return;
		}
		
		if(message.equals("")){
			((MainActivity)m_activity).reload_failed(statusCode);
			return;
		}
		
		((MainActivity)m_activity).reload_finish(article);

		if(article.data_list.size()==0){
			((MainActivity)m_activity).reload_empty();
		}
	}
}
