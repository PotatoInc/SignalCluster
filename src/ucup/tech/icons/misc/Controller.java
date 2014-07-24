package ucup.tech.icons.misc;

import android.content.Context;

public class Controller {
	private Context mContext;
	public Controller(Context context){
		mContext = context;
	}
	public int getDrawableId(String s){
		return mContext.getResources().getIdentifier(s, "drawable", mContext.getPackageName());
	}
	public int getLayoutId(String s){
		return mContext.getResources().getIdentifier(s, "layout", mContext.getPackageName());
	}
	public int getId(String s){
		return mContext.getResources().getIdentifier(s, "id", mContext.getPackageName());
	}
}
