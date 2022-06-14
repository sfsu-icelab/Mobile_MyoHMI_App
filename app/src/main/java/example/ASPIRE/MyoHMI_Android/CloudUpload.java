/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package example.ASPIRE.MyoHMI_Android;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import java.io.File;

//import com.amazonaws.demo.s3transferutility.Constants;import com.amazonaws.demo.s3transferutility.R;import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;

/**
 * UploadActivity is a ListActivity of uploading, and uploaded records as well
 * as buttons for managing the uploads and creating new ones.
 */


public class CloudUpload {


    // Indicates that no upload is currently selected
    private static final int INDEX_NOT_CHECKED = -1;
    // TAG for logging;
    private static final String TAG = "UploadActivity";
    public static boolean delete = false;
    public static File file;
    public static long time;
    public static long acutime;
    View view;
    //  Button cloudButton = (Button) findViewById(R.id.btnCloud);
    Activity activity;
    Context context;
    // Button for upload operations
    private Button btnUploadFile;
    private Button btnUploadImage;
    // The TransferUtility is the primary class for managing transfer to S3
    private TransferUtility transferUtility;

    public CloudUpload() {

    }

    public CloudUpload(Context context) {
        transferUtility = Util.getTransferUtility(context);
        this.context = context;
        acutime = System.currentTimeMillis();
    }

    /*
     * Begins to upload the file specified by the file path.
     */
    public void beginUpload(File file) {

        this.file = file;
        TransferObserver observer = transferUtility.upload(Credentials.BUCKET_NAME, file.getName(), file);
        time = System.currentTimeMillis();

        Log.d("CloudUpload", "Time to gather data: " + String.valueOf(time - acutime) + " miliseconds");

        acutime = System.currentTimeMillis();
//        TransferState state = observer.getState();
//        TransferListener listener = new UploadListener();

        observer.setTransferListener(new UploadListener());
    }

    public void delete() {
        file.delete();
    }

    public boolean getDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public long getTime() {
        return time;
    }

    public File getFile() {
        return file;
    }
}


//===================================?=====================================

class UploadListener implements TransferListener {

    private CloudUpload cloudUpload = new CloudUpload();

    @Override
    public void onError(int id, Exception e) {
        Log.e("", "Error during upload: " + id, e);
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        Log.d("UploadProgress", String.format("onProgressChanged: %d, total: %d, current: %d",
                id, bytesTotal, bytesCurrent));
    }

    @Override
    public void onStateChanged(int id, TransferState newState) {
//        Log.d("CloudUpload", "onStateChanged: " + id + ", " + newState);
        if (newState.name() == "COMPLETED") {
//            Log.d("CloudUpload", "Completed: "+String.valueOf(cloudUpload.getDelete()));
            Log.d("CloudUpload", "Upload Time: " + String.valueOf(System.currentTimeMillis() - cloudUpload.getTime()) + " miliseconds");
            Log.d("CloudUpload", "File Size: " + String.valueOf(cloudUpload.getFile().length()) + " bytes");
            if (cloudUpload.getDelete())
                cloudUpload.delete();
        } else if (newState.name() == "FAILED") {
            //retry?
        }
    }
}
//===================================?=====================================