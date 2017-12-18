package com.Leexy.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.akexorcist.localizationactivity.ui.LocalizationActivity;

public class SettingsActivity extends LocalizationActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Spanned policy = Html.fromHtml(getString(R.string.privacy_policy));
        TextView termsOfUse = (TextView)findViewById(R.id.privacy_policy_tvlink);
        termsOfUse.setText(policy);
        termsOfUse.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onClickENLang(View view) {
        setLanguage("en");
    }

    public void onClickFRLang(View view) {
        setLanguage("fr");
    }

    public void onClickARLang(View view) {
        setLanguage("ar");
    }
}
