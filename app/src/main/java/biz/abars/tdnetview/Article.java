package biz.abars.tdnetview;

import java.util.ArrayList;
import java.util.List;

public class Article{
	public ArrayList<String> data_list;
	public ArrayList<String> url_list;
	public String adapter_id;
	
	Article(){
		data_list = new ArrayList<String>();
		url_list = new ArrayList<String>();
		adapter_id = "";
	}

	public Article clone_article() {
		Article article = new Article();
		article.data_list=(ArrayList<String>)this.data_list.clone();
		article.url_list=(ArrayList<String>)this.url_list.clone();
		article.adapter_id=new String(this.adapter_id);
		return article;
	}
	
	static final int COMPANY_ID=1;
	static final int COMPANY_NAME=2;
	static final int TITLE=3;
	static final int FULL_TEXT=4;

	public List<String> getItemInfo(int position){
		String item=data_list.get(position);
		item=item.replace("\n"," ");
		item=item.replace("<br/>"," ");
		String item_list[]=item.split(" ");		
		List<String> item_list2=new ArrayList<String>();
		for(int i=0;i<item_list.length;i++){
			if(item_list[i].equals("")){
				continue;
			}
			item_list2.add(item_list[i]);
		}
		return item_list2;
	}
	
	private String getOneInfo(int position,int id){
		List<String> item_list=getItemInfo(position);
		if(item_list.size()<=id){
			return "";
		}
		return item_list.get(id);
	}

	public String getCompany(int position){
		return getOneInfo(position,COMPANY_NAME);	
	}
	
	public String getTitle(int position){
		return getOneInfo(position,TITLE);	
	}
	
	public String getUrl(int position){
		return url_list.get(position);
	}
	
	public String getTweetFormat(int position){
		List<String> item_list=getItemInfo(position);
		String item="";
		for(int i=COMPANY_NAME;i<item_list.size();i++){
			if(i>=FULL_TEXT){
				continue;
			}
			if(i!=COMPANY_NAME){
				item+=" ";
			}
			item+=item_list.get(i);
		}
		return item;
	}

}