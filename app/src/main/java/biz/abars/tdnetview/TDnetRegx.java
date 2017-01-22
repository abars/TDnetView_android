package biz.abars.tdnetview;

import org.json.JSONObject;

public class TDnetRegx{
	public static int VERSION=0;
	
	public static String APPENGINE_BASE_URL="http://tdnet-search.appspot.com/";
	
	public static String TDNET_TOP_URL="https://www.release.tdnet.info/inbs/I_main_00.html";
	public static String TDNET_BASE_URL="https://www.release.tdnet.info/inbs/";
	public static String TDNET_DAY_PAGE_PATTERN="frame src=\"(.*)\" name=\"frame_l\"";
	public static String TDNET_NEXT_PAGE_PATTERN="location=\'(.*)?\'\" type=\"button\" value=\"次画面\"";
	public static String TDNET_TR_PATTERN="<tr>(.*?)</tr>";
	public static String TDNET_TD_PATTERN="<td.*?>(.*?)</td>";
	public static String TDNET_CONTENT_PATTERN="<a href=\"(.*?)\" target=.*>(.*?)</a>";
	
	public static int TDNET_ID_N=4;
	public static int TDNET_DATE_ID=0;
	public static int TDNET_COMPANY_CODE_ID=1;
	public static int TDNET_COMPANY_ID=2;
	public static int TDNET_DATA_ID=3;
	
	public static synchronized Boolean Update(String content){
		try{
			JSONObject json=new JSONObject(content);
			VERSION=json.getInt("version");
			TDNET_ID_N=json.getInt("id_n");
			TDNET_DATE_ID=json.getInt("date_id");
			TDNET_COMPANY_ID=json.getInt("company_id");
			TDNET_COMPANY_CODE_ID=TDNET_COMPANY_ID-1;
			TDNET_DATA_ID=json.getInt("data_id");
			TDNET_TOP_URL=json.getString("top_url");
			TDNET_BASE_URL=json.getString("base_url");
			TDNET_DAY_PAGE_PATTERN=json.getString("day_page_pattern");
			TDNET_NEXT_PAGE_PATTERN=json.getString("next_page_pattern");
			TDNET_TR_PATTERN=json.getString("tr_pattern");
			TDNET_TD_PATTERN=json.getString("td_pattern");
			TDNET_CONTENT_PATTERN=json.getString("content_pattern");		
			if(VERSION!=1){
				return false;
			}
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
