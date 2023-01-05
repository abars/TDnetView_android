package biz.abars.tdnetview;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Debug;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.util.Log;
import android.text.Html;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.os.SystemClock;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import biz.abars.tdnetview.R;
import android.widget.EditText;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.*;
//import com.google.android.gms.analytics.Logger;
//import com.google.android.gms.analytics.Tracker;
//import com.google.android.gms.analytics.GoogleAnalytics;
//import com.google.android.gms.analytics.HitBuilders;

public class MainActivity extends Activity implements
NavigationDrawerFragment.NavigationDrawerCallbacks {

	private final int MODE_TODAY = 0;
	private final int MODE_YESTERDAY = 1;
	private final int MODE_INCENTIVE = 2;
	private final int MODE_MARK = 3;
	private final int MODE_SEARCH = 4;
	private final int MODE_SEARCH_LIST = 5;
	private final int MODE_SEARCH_FROM = 2;
	
	private final boolean DEBUG_NOTIFICATION = false;
	
	private final String PREV_PAGE_ID="Prev page";
	private final String NEXT_PAGE_ID="Next page";

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	public NavigationDrawerFragment mNavigationDrawerFragment;
	private DownloadPdf mDownloadPdf;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private EditText text;
	private SaveLoad m_save_load = new SaveLoad();

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	private int mModeTab = MODE_TODAY;
	private int mMode = MODE_TODAY;
	private ArrayAdapter<CharSequence> adapter;
	
	/* data list */
	private Article article = new Article(); /* loadした記事 */
	private Article article_show = new Article(); /* articleをマスキングした記事 */

	/* marking */
	private ArrayList<String> mark_list; /* 会社名リスト */
	
	/* search */
	private ArrayList<String> search_list; /* 検索履歴 */
	private String RECENT_SEARCH_QUERY="最近の開示";
	private final int SEARCH_LIST_N=64;
	
	private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
		@Override
		public void onRefresh() {
			reload("","",0);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		text=(EditText) findViewById(R.id.editText1);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mDownloadPdf = new DownloadPdf();
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
		mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);

		SharedPreferences pref_mark =getSharedPreferences("mark",MODE_PRIVATE);
		mark_list=m_save_load.load_mark(pref_mark);

		SharedPreferences pref_search =getSharedPreferences("search",MODE_PRIVATE);
		search_list=m_save_load.load_search(pref_search);
		if(search_list.size()==0){
			add_search(RECENT_SEARCH_QUERY);
		}

		reload_when_mode_change();

		// Set up admob
		AdView adView = (AdView)this.findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		adView.loadAd(adRequest);
		
		// update state
		updateModeState(false);

		// Get extra url from notification
		treat_intent_parameter(getIntent());
    }
	
	@Override
	protected void onNewIntent(Intent intent){
		Log.i("","onNewIntent");
		treat_intent_parameter(intent);
	}
	
	private void treat_intent_parameter(Intent i){
		String url = i.getStringExtra("url");
		if(url!=null && url.length()>=1){
			i.putExtra("url","");
			String ext_dir=getExternalCacheDir().getAbsolutePath();
			mDownloadPdf.downloadAndOpenPDF(url,ext_dir,this);		        				
		}		
	}
	
	private void reload_when_mode_change(){
		SharedPreferences pref =getSharedPreferences("cache",MODE_PRIVATE);
		if(mMode==MODE_TODAY){
			Article new_article = new Article();			
			if(m_save_load.load(pref,new_article,true)){
				reload_finish(new_article);
			}else{
				reload(getString(R.string.fething),"",0);
			}
		}else{
			//resume
			reload(getString(R.string.fething),"",0);
		}		
	}

	private ListView lv = null;
	
	public String getMarkQuery(){
		String query="";
		if(mark_list!=null && mark_list.size()>=1){
			for(int i=0;i<mark_list.size();i++){
				if(i!=0){
					query+=" OR ";
				}
				query+=mark_list.get(i);
			}
		}
		return "company:"+query;
	}
	
	HttpGetTask now_task=null;
	String m_before_query = "";
	int m_before_page = 0;

	public void reload(String message,String query,int page){
		if(mMode==MODE_SEARCH && query.equals("")){
			mSwipeRefreshLayout.setRefreshing(false);
			return;
		}
		m_before_query = query;
		m_before_page = page;
		if(message!=""){
			mNavigationDrawerFragment.onMessage(message);
		}
		if(mMode==MODE_MARK){
			query=getMarkQuery();
		}
		if(mMode==MODE_INCENTIVE){
			query="title:株主優待";
		}
		if(query.equals(RECENT_SEARCH_QUERY)){
			query="recent";
		}
		HttpGetTask task=new HttpGetTask(mMode,query,this,null,isFullSearchDisable(),page);
		cancel_task();
		now_task=task;
		task.setCache(article);
		Log.i("",""+query);
		task.execute(); // AsyncTaskを使って定義したタスクを呼び出す
	}
	
	private void cancel_task(){
		if(now_task!=null){
			now_task.cancel(true);
			now_task=null;
		}
	}

	public void reload_empty(){
		mNavigationDrawerFragment.onMessage(getString(R.string.no_data_found));
	}

	public void reload_failed(int status_code){
		String message=getString(R.string.network_error);
		if(status_code!=0){
			message=message+" "+status_code;
		}
		mNavigationDrawerFragment.onMessage(message);		
		mSwipeRefreshLayout.setRefreshing(false);
	}

	private HashMap<String,Boolean> new_flag;
	private ArrayList<String> new_flag_url_list;
	
	public void invalidate_list(){
		ArrayAdapter<CharSequence> new_adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_list_item_1);
		lv = (ListView) findViewById(R.id.listView1);		
		lv.setAdapter(new_adapter);
	}
	
	public void show_search_list(){
		Article new_article=new Article();
		new_article.data_list=new ArrayList<String>();
		new_article.url_list=new ArrayList<String>();
		for(int i=0;i<search_list.size();i++){
			String query=search_list.get(i);
			query=query.replace(">","&gt;");
			query=query.replace("<","&lt;");
			new_article.data_list.add(query);
			new_article.url_list.add("search");
		}
		new_article.adapter_id="";
		invalidate_list();
		reload_finish_core(new_article);
	}

	public void reload_finish(Article new_article){
		if(mMode==MODE_SEARCH_LIST){
			return;
		}
		reload_finish_core(new_article);
	}
	
	public void reload_finish_core(Article new_article){
		ArrayAdapter<CharSequence> new_adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_list_item_1){
			@Override
			public View getView(int position, View convertView,ViewGroup parent) {	
				String item=article_show.data_list.get(position);
				View view = super.getView(position, convertView, parent);
				String MARK_COLOR="#80CBC4";
				Boolean is_marked=mark_list!=null && mark_list.size()>=1 && mark_list.contains(article_show.getCompany(position));
				if(mMode!=MODE_MARK && is_marked){
					view.setBackgroundColor(Color.parseColor(MARK_COLOR));					
				}else{
					if((new_flag!=null && new_flag.get(item)) || (mMode==MODE_MARK && !is_marked && item!=NEXT_PAGE_ID && item!=PREV_PAGE_ID)){
						view.setBackgroundColor(Color.DKGRAY);
					}else{
						view.setBackgroundColor(Color.TRANSPARENT);
					}
				}
				return view;
			}
		};
		
		Article new_article_show = new Article();
		HashMap<String,Boolean> new_new_flag=new HashMap<String,Boolean>();

		if(mMode>=MODE_SEARCH_FROM && m_before_page>=1){
			add_button(new_adapter,new_article_show,new_new_flag,PREV_PAGE_ID);
		}		

		for(int i=0;i<new_article.data_list.size();i++){
			if(isDailyInfoDisable() && new_article.data_list.get(i).indexOf("日々の")!=-1){
				continue;
			}
			if(mMode==MODE_MARK && mark_list!=null && mark_list.size()>=1 && !mark_list.contains(new_article.getCompany(i))){
				continue;
			}
			new_adapter.add(Html.fromHtml(new_article.data_list.get(i)));
			new_article_show.data_list.add(new_article.data_list.get(i));
			new_article_show.url_list.add(new_article.url_list.get(i));
			if(mMode==MODE_TODAY && new_flag_url_list!=null && !new_flag_url_list.contains(new_article.getUrl(i))){
				new_new_flag.put(new_article.data_list.get(i),true);
			}else{
				new_new_flag.put(new_article.data_list.get(i),false);
			}
			
			//Debug notification
			if(i<=4 && DEBUG_NOTIFICATION){
				NotificationSender notif=new NotificationSender();
				notif.send_notification(this, null, new_article.getCompany(i), new_article.getTitle(i), new_article.getUrl(i));
			}
		}
		
		if(mMode>=MODE_SEARCH_FROM && new_article.data_list.size()>=HttpGetTask.PAGE_UNIT){
			add_button(new_adapter,new_article_show,new_new_flag,NEXT_PAGE_ID);
		}

		new_flag=new_new_flag;
		adapter=new_adapter;
		article=new_article;
		article_show=new_article_show;
		
		if(mMode==MODE_TODAY){
			new_flag_url_list=new_article_show.url_list;
		}

		lv = (ListView) findViewById(R.id.listView1);
		lv.setAdapter(adapter);
		
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				clickItem(position);
			}
		});

		registerForContextMenu(lv);

		if(mMode==0){
			SharedPreferences pref =getSharedPreferences("cache",MODE_PRIVATE);
			m_save_load.save(pref,new_article);
		}

		mSwipeRefreshLayout.setRefreshing(false);
	}
	
	private void add_button(ArrayAdapter<CharSequence> new_adapter,Article new_article_show,HashMap<String,Boolean> new_new_flag,String id){
		new_adapter.add(id);
		new_article_show.data_list.add(id);
		new_article_show.url_list.add(id);
		new_new_flag.put(id,false);		
	}

	private void search_one(String query,Boolean update_search_list){
		text.setVisibility(View.GONE);
		invalidate_list();
		mMode=MODE_SEARCH;
		close_keyboard();
		String show_query=query.replace("company:","");
		show_query=show_query.replace("title:","");
		reload(getString(R.string.searching)+" "+show_query,query,0);
		if(update_search_list){
			add_search(query);
		}
	}
	
	private void clickItem(int position){
		if(mMode==MODE_SEARCH_LIST){
			search_one(search_list.get(position),true);
			return;
		}
		String url=article_show.getUrl(position);
		if(url.equals(NEXT_PAGE_ID) || url.equals(PREV_PAGE_ID)){
			invalidate_list();
			mSwipeRefreshLayout.setRefreshing(true);
			if(url.equals(NEXT_PAGE_ID)){
				reload("",m_before_query,m_before_page+1);
			}else{
				reload("",m_before_query,m_before_page-1);			
			}
			return;
		}
		String ext_dir=getExternalCacheDir().getAbsolutePath();
		mDownloadPdf.downloadAndOpenPDF(url,ext_dir,this);		        	
	}
	
	private void tweet(int position){
		try {
			String url=TDnetRegx.TDNET_BASE_URL+article_show.getUrl(position);
			String item=article_show.getTweetFormat(position);
			String text = "http://twitter.com/share?text="+item+" "+url;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			setError("Tweet application is not installed in your device");
		}
	}
	
	private void mark_unmark(int position){
		String company=article_show.getCompany(position);
		if(mark_list==null){
			mark_list=new ArrayList<String>();
		}
		if(mark_list.contains(company)){
			mark_list.remove(company);
		}else{
			mark_list.add(company);
		}
		lv.invalidateViews();

		SharedPreferences pref_mark =getSharedPreferences("mark",MODE_PRIVATE);
		m_save_load.save_mark(pref_mark,mark_list);
		
		if(mMode==MODE_MARK){
			reload("Unmarking","",0);
		}
	}

	private void add_search(String query){
		search_list.remove(query);
		search_list.add(0,query);
		while(search_list.size()>=SEARCH_LIST_N){
			search_list.remove(SEARCH_LIST_N-1);			
		}
		if(!search_list.contains(RECENT_SEARCH_QUERY)){
			search_list.add(RECENT_SEARCH_QUERY);
		}
		save_search();
	}

	private void del_search(String query){
		search_list.remove(query);
		save_search();
		show_search_list();
	}

	private void save_search(){
		SharedPreferences pref_search =getSharedPreferences("search",MODE_PRIVATE);
		m_save_load.save_search(pref_search,search_list);
	}

	private void setError(String text){
		mNavigationDrawerFragment.onMessage(text);
	}

	private void yahoo(int position){
		try {
			String company=article_show.getCompany(position);
			company=company.replace("Ｍ－","");
			company=company.replace("Ｊ－","");
			String text = "http://m.finance.yahoo.co.jp/search?q="+company;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(text));
			startActivity(intent);		
		} catch (ActivityNotFoundException e) {
			setError("Browser application is not installed in your device");
		}
	}

	static final int CONTEXT_MENU_ID_TWEET = 0;
	static final int CONTEXT_MENU_ID_YAHOO = 1;
	static final int CONTEXT_MENU_ID_MARK = 2;
	static final int CONTEXT_MENU_ID_REMOVE = 3;
	static final int CONTEXT_MENU_ID_SEARCH = 4;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

    	AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
    	ListView listView = (ListView) v;
    	String url=article_show.getUrl(adapterInfo.position);
		if(url.equals(NEXT_PAGE_ID) || url.equals(PREV_PAGE_ID)){
			return;
		}

		//コンテキストメニューの設定
		menu.setHeaderTitle("Menu");
		//Menu.add(int groupId, int itemId, int order, CharSequence title)
		if(mMode==MODE_SEARCH_LIST){
			menu.add(0, CONTEXT_MENU_ID_REMOVE, 0, "Remove");
		}else{
			menu.add(0, CONTEXT_MENU_ID_TWEET, 0, "Tweet");
			menu.add(0, CONTEXT_MENU_ID_YAHOO, 0, "Yahoo");
			menu.add(0, CONTEXT_MENU_ID_SEARCH, 0, "Search");
			menu.add(0, CONTEXT_MENU_ID_MARK, 0, "Mark/Unmark");
		}
	}

	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		int position=info.position;
		
		String url=article_show.getUrl(position);
		if(url.equals(NEXT_PAGE_ID) || url.equals(PREV_PAGE_ID)){
			return false;
		}

		switch (item.getItemId()) {
		case CONTEXT_MENU_ID_TWEET:
			tweet(position);
			return true;
		case CONTEXT_MENU_ID_YAHOO:
			yahoo(position);
			return true;
		case CONTEXT_MENU_ID_MARK:
			mark_unmark(position);
			return true;
		case CONTEXT_MENU_ID_SEARCH:
			String query="company:"+article_show.getCompany(position);
			onSectionAttachedCore(MODE_SEARCH+1);
			restoreActionBar();
			search_one(query,false);
			return true;
		case CONTEXT_MENU_ID_REMOVE:
			del_search(search_list.get(position));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
		.beginTransaction()
		.replace(R.id.container,
				PlaceholderFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		onSectionAttachedCore(number);
		if(mMode==MODE_SEARCH_LIST){
			mModeTab=MODE_SEARCH;
		}else{
			mModeTab=mMode;
		}
	}
	
	private void onSectionAttachedCore(int number){
		int mode=mMode;

		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			mMode = MODE_TODAY;
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			mMode = MODE_YESTERDAY;
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			mMode = MODE_INCENTIVE;
			break;
		case 4:
			mTitle = getString(R.string.title_section4);
			mMode = MODE_MARK;
			break;
		case 5:
			mTitle = getString(R.string.title_section5);
			mMode = MODE_SEARCH;
			break;
		}
		
		if(mode!=mMode){
			cancel_task();
		}

		updateModeState(mode!=mMode);
	}
	
	private void updateModeState(Boolean mode_changed){
		//画面を回転させた場合にLayoutがnullの状態で
		//onSectionAttachedが呼ばれることがある
		//その場合はonCreateでupdateModeStateを呼び直す
		
		if(mSwipeRefreshLayout==null || text==null){
			return;
		}	
			
		if(mMode!=MODE_TODAY){//==MODE_SEARCH || mMode==MODE_YESTERDAY){
			mSwipeRefreshLayout.setEnabled(false);
		}else{
			mSwipeRefreshLayout.setEnabled(true);
		}

		if(mMode==MODE_SEARCH){
			mMode=MODE_SEARCH_LIST;
			invalidate_list();
			show_search_list();
			text.setVisibility(View.VISIBLE);
			text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					search_one(getQuery(),true);
					return false;
				}
			});
		}else{
			text.setText("");
			close_keyboard();
			text.setVisibility(View.GONE);
			if(mode_changed){
				invalidate_list();
				if(mMode==MODE_MARK || mMode==MODE_INCENTIVE){
					reload(getString(R.string.searching),"",0);						
				}else{
					reload_when_mode_change();
				}
			}
		}
	}

	private void close_keyboard(){
		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(text.getWindowToken(), 0);		
		findViewById(R.id.container).requestFocus();
	}

	private String getQuery(){
		Editable query=text.getText();
		return query.toString();
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	private int PREFERENCE_REQUEST_CODE=1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(this,PreferenceWindow.class);
			startActivityForResult(intent,PREFERENCE_REQUEST_CODE);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode==PREFERENCE_REQUEST_CODE){
			AlarmReceiver.start_timer(this);
			reload(getString(R.string.updating),"",0);
		}
	}

	private Boolean isDailyInfoDisable(){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Boolean preferenceValue = preferences.getBoolean("DAILY_INFO_DISABLE", false);
		return preferenceValue;
	}

	private Boolean isFullSearchDisable(){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Boolean preferenceValue = preferences.getBoolean("FULL_SEARCH_DISABLE", false);
		return preferenceValue;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(
					ARG_SECTION_NUMBER));
		}
	}

	//Analytics
    // The following line should be changed to include the correct property id.
	/*
    private static final String PROPERTY_ID = "UA-8633292-10";
    public static int GENERAL_TRACKER = 0;
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }
    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PROPERTY_ID)
                    : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(
                            R.xml.global_tracker)
                            : analytics.newTracker(R.xml.ecommerce_tracker);
            t.enableAdvertisingIdCollection(true);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }
    */

	@Override
	protected void onStart() {
	    super.onStart();
	    //Tracker t = getTracker(TrackerName.APP_TRACKER);
		//t.setScreenName("Main");
		//t.send(new HitBuilders.AppViewBuilder().build());
	}
	 
	@Override
	protected void onRestart() {
	    super.onStart();
	    //Tracker t = getTracker(TrackerName.APP_TRACKER);
		//t.setScreenName("Main");
		//t.send(new HitBuilders.AppViewBuilder().build());
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {	
		if(keyCode != KeyEvent.KEYCODE_BACK || mMode!=MODE_SEARCH){
			return super.onKeyDown(keyCode, event);
		}else{
			//Return to tab top
			return_to_tab_top();
			return false;
		}
	}
	
	private void return_to_tab_top(){
		cancel_task();
		onSectionAttachedCore(mModeTab+1);
		restoreActionBar();	
	}
}
