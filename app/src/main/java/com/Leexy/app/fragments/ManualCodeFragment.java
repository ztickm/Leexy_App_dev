package com.Leexy.app.fragments;


import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.Leexy.app.R;
import com.Leexy.app.dialogs.AdsDialog;

import java.util.regex.Pattern;

public class ManualCodeFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manual_code, container, false);

        final TextInputLayout inputCode = (TextInputLayout) view.findViewById(R.id.text_layout_input_code);
        final EditText editCode = (EditText) view.findViewById(R.id.edit_code);

        editCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {

                    String regex = "^[0-9]{1,14}$";
                    boolean validCode = Pattern.compile(regex).matcher(s.toString()).find();

                    Log.w("ManualCode","valid code ? "+validCode);

                    if (s.toString().length() > 14 || !validCode)
                        throw new NumberFormatException("invalid code");

                    inputCode.setErrorEnabled(false);

                    if (s.toString().length() == 14){
                        // todo : start open dialog ...
                        AdsDialog dialog = new AdsDialog(getContext());
                        dialog.run();
                        dialog.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                editCode.setText("");
                                Toast.makeText(getContext(),"Success",Toast.LENGTH_LONG).show();
                                /*
                                TODO :
                                if you can know if the request was succeeded or not :
                                if successful answer -->
                                    Show Toast Message
                                    Back to home with : "getFragmentManager().popBackStack();"
                                else
                                    Show Toast Message
                                    ask user to try again with an other code
                                 */
                            }
                        });
                    }

                }catch (NumberFormatException e){
                    inputCode.setErrorEnabled(true);
                    inputCode.setError("Veuillez entrer 14 chiffres SVP");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

}
