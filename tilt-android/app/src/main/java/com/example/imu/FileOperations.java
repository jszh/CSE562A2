package com.example.imu;

import android.app.Activity;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class FileOperations {

    public static void writetofile(Activity av, String fname) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String dir = av.getExternalFilesDir(null).toString();
                    File path = new File(dir);
                    if (!path.exists()) {
                        path.mkdirs();
                    }
                    File file = new File(dir, fname+"-acc.txt");
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(file,false));
                    for (int i = 0; i < Constants.rawaccx.size(); i++) {
                        outfile.append(Constants.rawaccx.get(i)+","+Constants.rawaccy.get(i)+","+Constants.rawaccz.get(i));
                        outfile.newLine();
                    }
                    outfile.flush();
                    outfile.close();

                    file = new File(dir, fname+"-tilt.txt");
                    outfile = new BufferedWriter(new FileWriter(file,false));
                    for (int i = 0; i < Constants.tiltAcc.size(); i++) {
                        outfile.append(Constants.tiltAcc.get(i)+","+Constants.tiltGyr.get(i)+","+Constants.tiltComp.get(i));
                        outfile.newLine();
                    }
                    outfile.flush();
                    outfile.close();
                } catch(Exception e) {
                    Log.e("ex", "writeRecToDisk");
                    Log.e("ex", e.getMessage());
                }
            }
        }).run();
    }
}
