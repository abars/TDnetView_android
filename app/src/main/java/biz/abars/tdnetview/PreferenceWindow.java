package biz.abars.tdnetview;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import biz.abars.tdnetview.R;
public class PreferenceWindow extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
    }
}

