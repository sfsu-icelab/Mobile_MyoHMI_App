package example.ASPIRE.MyoHMI_Android;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Alex on 6/28/2018.
 */

//we should try bytes in sql instead of objects
// https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array

public class ObjectIO {

    File Root = Environment.getExternalStorageDirectory();
    File Dir = new File(Root.getAbsolutePath() + "/MyoAppFile");

    public Object ReadObjectFromFile(String filepath) {

        try {

            FileInputStream fileIn = new FileInputStream(Dir + "/" + filepath);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            Object obj = objectIn.readObject();

            System.out.println("The Object has been read from the file");
            objectIn.close();
            return obj;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void WriteObjectToFile(Object serObj) {

        String date = new SimpleDateFormat("yyyy-MM-dd-hh-mm").format(new Date());
        String FileName = "Saved_Object_" + date;
        File file = new File(Dir, FileName);

        try {

            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(serObj);
            objectOut.close();
            System.out.println("The Object  was succesfully written to a file");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
