package com.Leexy.app.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.Leexy.app.OcrCaptureActivity;
import com.Leexy.app.R;

public class HomeFragment extends Fragment {



    private LinearLayout openScannerReader, openManuelCode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        openManuelCode = view.findViewById(R.id.layout_open_manuel_code);
        openScannerReader = view.findViewById(R.id.layout_open_scanner);

        openScannerReader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), OcrCaptureActivity.class));
            }
        });

        openManuelCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new ManualCodeFragment();
                getActivity().getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, fragment,ManualCodeFragment.class.getSimpleName())
                        .addToBackStack(ManualCodeFragment.class.getSimpleName())
                        .commit();
            }
        });

        return view;
    }

}
