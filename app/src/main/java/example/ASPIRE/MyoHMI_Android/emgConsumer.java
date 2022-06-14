//package example.ASPIRE.MyoHMI_Android;
//
//import android.os.Environment;
//import android.util.Log;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.text.SimpleDateFormat;
//import java.util.Arrays;
//import java.util.Date;
//
///**
// * Created by Alex on 8/25/2017.
// */
//
//public class emgConsumer extends EmgFragment{
//
//    private byte[] data;
//    private boolean newfile = true;
//    private String FileName;
//    private File file;
//    private CloudUpload cloudUpload = new CloudUpload(getActivity());
//
//    public emgConsumer(){
//        cloudUpload.setDelete(true);
//    }
//
//    public void consume(byte[] data){
//
//        if(newfile){
//            newfile=false;
//            file = null;
//
//            String state;
//            state = Environment.getExternalStorageState();
//
//            String date = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());
//
//            if(Environment.MEDIA_MOUNTED.equals(state)){
//                File Root = Environment.getExternalStorageDirectory();
//                File Dir = new File(Root.getAbsolutePath() + "/MyoAppFile");
//                if(!Dir.exists()){
//                    Dir.mkdir();
//                }
//
//                FileName  = "RAW_EMG_" + date + ".txt";
//
//                file = new File(Dir, FileName);
//
//                try {
//
//                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
//                    OutputStreamWriter osw = new OutputStreamWriter(fileOutputStream);
//
//                    osw.append(Arrays.toString(data));
//                    osw.append("\n");
//
//                    osw.flush();
//                    osw.close();
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }else{
//                Log.d("EXTERNAL STRG","No SD card found");
//            }
//        }else{
//            try {
//
//                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
//                OutputStreamWriter osw = new OutputStreamWriter(fileOutputStream);
//
//                osw.append(Arrays.toString(data));
//                osw.append("\n");
//
//                osw.flush();
//                osw.close();
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
////            System.out.println(String.valueOf(file.length()));
//
//            if(file.length()>= 1000000){//filesize > 1MB //must be large enough so that intervals are at least 1 minute apart or else we get duplicate filenames
//                newfile = true;
//                cloudUpload.beginUpload(file);
//            }
//        }
//    }
//}

package example.ASPIRE.MyoHMI_Android;

import android.util.Log;

import java.io.File;

/**
 * Created by Alex on 8/25/2017.
 */

public class emgConsumer extends EmgFragment {

    long time1;
    int linecount = 0;
    private byte[] data;
    private boolean newfile = true;
    private String FileName;
    private File file;
    private CloudUpload cloudUpload = new CloudUpload(getActivity());

    public emgConsumer() {
        cloudUpload.setDelete(true);
    }

    public void consume(byte[] data) {

        linecount++;

        if (newfile) {
            newfile = false;
            time1 = System.currentTimeMillis();
        }

        if (linecount >= 16000) {
            newfile = true;
            Log.d("Performance, Time", String.valueOf(System.currentTimeMillis() - time1));
            Log.d("Performance, Samples", String.valueOf(linecount * 2));
            linecount = 0;
        }
    }
}