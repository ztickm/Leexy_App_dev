package com.Leexy.app.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.Leexy.app.R;

public class AdsDialog {

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Context context;
    private View view;

    public AdsDialog(Context context){
        this.context = context;
    }

    public void run(){
        this.dialogBuilder = new AlertDialog.Builder(context);
        this.view = LayoutInflater.from(context).inflate(R.layout.ads_dialog,null);

        dialogBuilder.setView(view);
        dialog = dialogBuilder.create();
        dialog.setCancelable(false);
        dialog.show();

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        dialog.dismiss();
                    }
                },
                5000);
    }

    public AlertDialog getDialog() {
        return dialog;
    }
}
