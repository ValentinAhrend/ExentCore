package com.exsent.core;

import android.annotation.SuppressLint;
import android.os.Handler;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jim-Linus Valentin Ahrend on 2/1/21.
 * Located in com.exsent.app.filesys in AndroidAppExample
 **/
public class SecureFile {

    private ArrayList<Function> functions;

    private ArrayList<File> files;

    private long[] time;

    private SecureFile instance;

    @SuppressLint("SdCardPath")
    private static final File SD_FILE = new File("/data/user/0/com.exsent.app/");
    private static final String ERR = "You don't have the permission to modify this file";

    public SecureFile getInstance() {

        functions = new ArrayList<>();
        files = new ArrayList<>();

        files = Function.Utils.listAllFiles(SD_FILE,true);

        time = new long[files.size()];

        handler = new android.os.Handler();

        run();

        return new SecureFile(false);

    }

    private SecureFile(boolean b){}

    public SecureFile(){
        if(SecureFileSave.getSecureFile()==null){SecureFileSave.setSecureFile(getInstance());instance = new SecureFile();}
        else instance = getInstance();
    }

    private Handler handler;

    private Runnable runnable = this::run;

    private ArrayList<File> selected = new ArrayList<>();

    private int los;

    private void run(){

        //check

        ArrayList<File> files = Function.Utils.listAllFiles(SD_FILE,true);

        if(files.size()-los!=time.length){

            //help
            killService();

        }

        for (int i = 0; i < time.length; i++) {
            File f = files.get(i);
            long l = f.lastModified();
            if(l!=time[i]){

                //help
                if(selected.contains(f))selected.remove(f);
                else killService();

            }
            time[i]=l;
        }

        handler.postDelayed(runnable,200);
    }

    private void killService(){
        for (Function function : functions) {
            function.interrupt();
        }
    }

    public void registerModification(File file, Function f, boolean ex){

        String path = file.getAbsolutePath();
        path = path.substring(SD_FILE.getAbsolutePath().length()+1);

        if(path.startsWith("app_all"+f.getFunctionID())){

            if(path.startsWith("app_all"+f.getFunctionID()+"/"+f.getName()+".dex") || path.startsWith("app_all"+f.getFunctionID()+"/"+f.getName()+".json")){

                //err

                throw new IllegalArgumentException(ERR);

            }

        }else {

            if (path.startsWith("cache")) {

                if (!path.startsWith("cache/func" + f.getFunctionID())) {

                    //err
                    throw new IllegalArgumentException(ERR);

                }

            }else{

                throw new IllegalArgumentException(ERR);

            }

        }

        selected.add(file);
        if(!ex)los++;
    }


}
class SecureFileSave {
    private static SecureFile secureFile;

    public static SecureFile getSecureFile() {
        return secureFile;
    }

    public static void setSecureFile(SecureFile secureFile) {
        SecureFileSave.secureFile = secureFile;
    }
}
