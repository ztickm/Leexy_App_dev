/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.Leexy.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.Leexy.app.ui.camera.GraphicOverlay;

import com.akexorcist.localizationactivity.ui.LocalizationActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.Leexy.app.ui.camera.CameraSource;
import com.Leexy.app.ui.camera.CameraSourcePreview;

import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Activity for the Ocr Detecting app.  This app detects text and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and contents of each TextBlock.
 */
public final class OcrCaptureActivity extends LocalizationActivity implements MoPubView.BannerAdListener {
    private static final String TAG = "OcrCaptureActivity";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final int CALL_PERM = 111;
    private static final int CALL_CAMERA_PERMISSIONS = 3;
    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    //Ads
    private MoPubView moPubView;
    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.ocr_capture);


        //Ads
//        moPubView = (MoPubView) findViewById(R.id.adview);
//        moPubView.setAdUnitId("e5bbfb06ad124fa891ae5bbe8427a836");
//        moPubView.loadAd();
//        moPubView.setBannerAdListener(this);


        //Camera
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        // Set good defaults for capturing text.
        boolean autoFocus = true;
        boolean useFlash = false;

        //Permissions
        String[] PERMISSIONS = {Manifest.permission.CALL_PHONE, Manifest.permission.CAMERA};

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, CALL_CAMERA_PERMISSIONS);
        }
        else {
            createCameraSource(autoFocus, useFlash);
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
//        int cameraPermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
//        if (cameraPermissionState == PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "OnCreate: Camera Permission already granted, Creating camera source andChecking for phone permission");
//            createCameraSource(autoFocus, useFlash);
//
//            int phonePermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
//            if (phonePermissionState != PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "OnCreateCameraON: Phone Permission not granted, requesting");
//                requestPhonePermission();
//            }
//
//        } else {
//            Log.d(TAG, "OnCreate: Camera Permission not granted creating CameraSource and checking phone permissions");
//            requestCameraPermission();
//            int phonePermissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
//            if (phonePermissionState != PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "OnCreateCameraOFF: Phone Permission not granted, requesting");
//                requestPhonePermission();
//            }
//        }


        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        final Snackbar snackbar = Snackbar.make(mGraphicOverlay, R.string.Tap_rectangle_to_refill_your_account,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.Dismiss, new View.OnClickListener() {
            @Override
            public void onClick(View v){
                snackbar.dismiss();
            }
        });
        snackbar.show();

    }

    public void openSettingsActivity(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Checks for permissions
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Handles the requesting of the Phone permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestPhonePermission() {
        Log.w(TAG, " Call permission is not granted yet. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CALL_PHONE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CALL_PHONE)) {
            ActivityCompat.requestPermissions(this, permissions, CALL_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        CALL_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.Call_permission_is_necessary,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }


    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */

    //TODO: this method is too big, break it up
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        switch (requestCode) {
            case CALL_PERM: {
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();

                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.permissions_needed_title)
                            .setMessage(R.string.permissions_notgranted)
                            .setPositiveButton(R.string.ok, listener)
                            .show();

                } else {

                    // permissions were granted
                    return;


                }
            }

            case RC_HANDLE_CAMERA_PERM: {

                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted - initialize the camera source");
                    // we have permission, so create the camerasource
                    boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
                    boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
                    createCameraSource(autoFocus, useFlash);
                    return;
                }

                Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.permissions_needed_title)
                        .setMessage(R.string.permissions_notgranted)
                        .setPositiveButton(R.string.ok, listener)
                        .show();

            }
            case CALL_CAMERA_PERMISSIONS:{
                if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted - initialize the camera source");
                    // we have permission, so create the camerasource
                    boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
                    boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
                    createCameraSource(autoFocus, useFlash);
                    return;
                }

                Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.permissions_needed_title)
                        .setMessage(R.string.permissions_notgranted)
                        .setPositiveButton(R.string.ok, listener)
                        .show();
            }
            default: {
                Log.d(TAG, "Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
            }


        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {

        super.onResume();
        Log.d(TAG, "ON RESUME.");
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ON PAUSE.");
        if (mPreview != null) {
            Log.d(TAG, "ON PAUSE mPreview isn't stopped.");
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {

//        moPubView.destroy();
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }



    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap is called to speak the tapped TextBlock, if any, out loud.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the tap was on a TextBlock
     */
    private boolean onTap(float rawX, float rawY) {
        OcrGraphic graphic = mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
        TextBlock text = null;


        if (graphic != null) {
            text = graphic.getTextBlock();
            String textValue = text.getValue();
            textValue= textValue.replaceAll("\\s","");
            if (text != null && textValue != null) {
                boolean textIsValidRechargeCode = Pattern.matches("[0-9]+[0-9 \\t]+", text.getValue());
                if (textIsValidRechargeCode) {
                    showConfirmationDialog(textValue);
                } else {

                    Snackbar.make(mGraphicOverlay, R.string.Detected_text_isntrefillcode,
                            Snackbar.LENGTH_LONG)
                            .show();
                    Log.d(TAG, "REGEX: Detected text isn't a Refill code");
                }
            } else {
                Log.d(TAG, "text data is null");
            }
        } else {
            Log.d(TAG, "no text detected");
        }
        return text != null;
    }

    //TODO: move this to new class
    private void showConfirmationDialog(String refillCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirm_correct_refill_code_dialog);

        // Set up the input
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);
        input.setText(refillCode);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String confirmedCode= input.getText().toString();
                callOperatorService(confirmedCode);
            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    //TODO: move this to new class
    private void callOperatorService(String rawUniqueRefillCode) {


        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String simOperatorName = telephonyManager.getSimOperatorName();
        String fullUssdCallCode;
        switch (simOperatorName) {
            case AppConfig.OPERATOR1: { //mobilis
                fullUssdCallCode = AppConfig.OPERATOR1_USSD_REFILL + rawUniqueRefillCode + "#";
                Log.d(TAG, "OPER: MOBILIS DETECTED");
                commitCallIntent(fullUssdCallCode);
                break;
            }
            case AppConfig.OPERATOR2: { //ooredoo
                fullUssdCallCode = AppConfig.OPERATOR2_USSD_REFILL + rawUniqueRefillCode + "#";
                Log.d(TAG, "OPER: OOREDOO DETECTED");
                commitCallIntent(fullUssdCallCode);
                break;
            }
            case AppConfig.OPERATOR3: {//djezzy
                fullUssdCallCode = AppConfig.OPERATOR3_USSD_REFILL + rawUniqueRefillCode + "#";
                Log.d(TAG, "OPER: DJEZZY DETECTED");
                commitCallIntent(fullUssdCallCode);
                break;
            }
            default: {
                Log.d(TAG, "OPER no valid operator detected" + simOperatorName);
                Snackbar.make(mGraphicOverlay, R.string.No_valid_operator_detected,
                        Snackbar.LENGTH_LONG)
                        .show();
                break;
            }
        }
        Log.d(TAG, "------------operateur : "+ simOperatorName);

    }
//TODO: move this to new class
    private void commitCallIntent(String fullUssdCallCode) {
        Log.d(TAG, "fullUssdCallCode" + fullUssdCallCode);

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + Uri.encode(fullUssdCallCode)));
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Permission not granted on commitCallIntent");

            requestPhonePermission();
            //requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, CALL_PERM);
            // return;

        } else {
            startActivity(callIntent);
        }
    }

    @Override
    public void onBannerLoaded(MoPubView banner) {

    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {

    }

    @Override
    public void onBannerClicked(MoPubView banner) {

    }

    @Override
    public void onBannerExpanded(MoPubView banner) {

    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {

    }



    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }



    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCameraSource != null) {
                mCameraSource.doZoom(detector.getScaleFactor());
            }
        }
    }
}


