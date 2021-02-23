package com.exsent.core;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.TaskStackBuilder;

import com.exsent.core.SecureFile;
import com.exsent.core.FunctionHandler1;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Jim-Linus Valentin Ahrend on 1/23/21.
 * Located in com.exsent.app.example in AndroidAppExample
 **/
public class Function extends AppCompatActivity {

    private Const c;
    private Object instance;

    public Object getInstance() {
        return instance;
    }

    public Function function;

    public Function(Function f){this.function = f;}
    public Function(){}

    private static final String TAG = Function.class.getName();

    private int functionID = 0;
    private String functionVersion;
    private String name;
    private String author;
    private boolean isBeta;
    private boolean isPrivate;

    private Storage storage;
    private Map<String, Drawable> images;


    /**
     * interrupt the current function
     * @see SecureFile
     */
    public void interrupt(){
        this.getMainLooper().getThread().interrupt();
        this.getMainLooper().quit();
    }

    public Map<String, Drawable> getDrawables(){
        return images;
    }

    public Drawable getDrawable(String name){
        if(!(name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".png")||name.endsWith(".xml"))){
            Log.e(TAG, "only .jpg/.jpeg/.png/.xml drawables are allowed, be sure the name contains the right ending: "+name);
            return null;
        }

        Drawable d = images.get(name);

        if(d==null)Log.e(TAG, "drawable with the name: "+name+ " failed to load");

        return images.get(name);
    }

    private Map<String,Drawable> getDrawables$(){
        ArrayList<File> files = Utils.listAllFiles(storage.assets(), false);
        if (files != null) {

            Map<String,Drawable> stringDrawableMap = new HashMap<>();

            files.forEach(file -> {
                final String name = file.getName();
                if(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")){
                    stringDrawableMap.put(name, Drawable.createFromPath(file.getAbsolutePath()));
                }
                if(name.endsWith(".xml")){
                    VectorDrawable d = new VectorDrawable();
                    try(InputStream in = new FileInputStream(file))
                    {
                        XmlPullParser p = Xml.newPullParser();
                        p.setInput( in, /*encoding, self detect*/null );
                        d.inflate( getResources(), p, Xml.asAttributeSet(p) ); // FAILS

                        stringDrawableMap.put(name, d);

                    } catch (IOException | XmlPullParserException e) {
                        e.printStackTrace();
                    }
                }

                         /*
                         NOTE: Svg-Files are not enabled, because it's much easier to work with .xml files.
                               Xml-Files are also vector images and you can put there the same path data in.
                          */

            });

            return stringDrawableMap;
        }else return null;
    }

    public class Storage {

        private Context c;
        private int id;

        private static final String S_DIR = "store", A_DIR = "assets", F_DIR = "files", C_DIR = "cache";


        Storage(Context c, int id){this.c=c;this.id=id;}

        private String getStore(){
            return c.getDir("all"+id, MODE_PRIVATE).getAbsolutePath()+File.separator+S_DIR;
        }

        /**
         *
         * Important: The asset folder is a copy of the original asset folder of the project
         *            This folder should not be edited. (it can)
         *            This folder is saving hardcoded image/text/... files
         *
         *
         * @return the file of the asset folder of the function
         */
        public File assets(){
            File f = new File(getStore()+File.separator+A_DIR);
            if(!f.exists())//noinspection ResultOfMethodCallIgnored
                f.mkdir();
            return f;
        }

        /**
         *
         * @param filename the filename including the suffix
         * @return the file out of the assets dir with the filename, could be null
         */
        @Nullable
        public File getAsset(String filename){
            return getAsset(null,filename);
        }

        /**
         * @param dir the path of the file in assets
         * @param filename the filename including the suffix
         * @return the file out of the assets dir with the filename, could be null
         */
        @Nullable
        public File getAsset(String dir, String filename){
            return new File(assets().getAbsolutePath()+(dir!=null?(File.separator+dir):null),filename);
        }

        /**
         *
         * Important: The files folder is a folder for medium and big sized data
         *            Be sure to keep it clean and handle its data nice
         *            It won't be deleted
         *
         * @return the file of the file folder of the function
         */
        public File files(){
            File f = new File(getStore()+File.separator+F_DIR);
            if(!f.exists())//noinspection ResultOfMethodCallIgnored
                f.mkdir();
            return f;
        }

        /**
         *
         * @param filename the filename including the suffix
         * @return the file out of the files dir with the filename, could be null
         */
        @Nullable
        public File getFile(String filename){
            return getFile(null,filename);
        }

        /**
         * @param dir the path of the file in files
         * @param filename the filename including the suffix
         * @return the file out of the files dir with the filename, could be null
         */
        @Nullable
        public File getFile(String dir, String filename){
            return new File(files().getAbsolutePath()+(dir!=null?(File.separator+dir):null),filename);
        }

        /**
         *
         * Important: The cache dir could be deleted spontaneously
         *            Don't save important information
         *            The user is able to delete the complete cache directory
         *
         * @return the file of the cache folder of the function
         */
        public File cache(){
            File f = new File(c.getCacheDir()+File.separator+"func"+id);
            if(!f.exists())//noinspection ResultOfMethodCallIgnored
                f.mkdir();
            return f;
        }

        /**
         *
         * @param filename the filename including the suffix
         * @return the file out of the cache dir with the filename, could be null
         */
        @Nullable
        public File getCacheFile(String filename){
            return new File(cache().getAbsolutePath(),filename);
        }

        /**
         * @param dir the path of the file in cache
         * @param filename the filename including the suffix
         * @return the file out of the cache dir with the filename, could be null
         */
        @Nullable
        public File getCacheFile(String dir,String filename){
            return new File(cache().getAbsolutePath()+(dir!=null?File.separator+dir:null),filename);
        }





    }

    private SecureFile secureFile;
    public void writeFile(File f, String data) throws IOException {
            if(secureFile==null)secureFile = new SecureFile();
            secureFile.registerModification(f,this, f.exists());
            FileOutputStream fIn = new FileOutputStream(f);
            OutputStreamWriter writer = new OutputStreamWriter(fIn);
            BufferedWriter writer1 = new BufferedWriter(writer);
            writer1.write((data));
    }

    public void writeFile(File f, byte[] data) throws IOException {
        writeFile(f,new String(data));
    }

    public boolean deleteFile(File f){
        if(secureFile==null)secureFile = new SecureFile();
        secureFile.registerModification(f,this, true);
        return f.delete();
    }


    public static class Utils {
        public static String readFile(File f){
            StringBuilder aBuffer = new StringBuilder();
            try {
                FileInputStream fIn = new FileInputStream(f);
                BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
                String aDataRow = "";
                while ((aDataRow = myReader.readLine()) != null) {
                    aBuffer.append(aDataRow);
                }
                myReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return aBuffer.toString();
        }

        public static XmlPullParser parseXml(String xml_){
            try {

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(new StringReader(xml_)); // pass input whatever xml you have
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {
                        //Log.d(TAG,"Start document");
                    } else if(eventType == XmlPullParser.START_TAG) {
                        //(TAG,"Start tag "+xpp.getName());
                    } else if(eventType == XmlPullParser.END_TAG) {
                        //Log.d(TAG,"End tag "+xpp.getName());
                    } else if(eventType == XmlPullParser.TEXT) {
                        //Log.d(TAG,"Text "+xpp.getText()); // here you get the text from xml
                    }
                    eventType = xpp.next();
                }
                //Log.d(TAG,"End document");

                return xpp;

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static ArrayList<File> listAllFiles(File src, boolean dirs_included){
            try {
                ArrayList<File> files_ = new ArrayList<>();
                File[] files = src.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            if(dirs_included)files_.add(file);
                            files_.addAll(Objects.requireNonNull(listAllFiles(file,dirs_included)));
                        }
                        else files_.add(file);
                    }
                }
                return files_;
            }catch (NullPointerException npe){
                npe.printStackTrace();
                return null;
            }
        }
    }

    private File sysDir(){
        return new File(getDir("all"+functionID,MODE_PRIVATE).getAbsolutePath()+"/resources/");
    }

    /**
     *
     * @return the id of the func
     */
    public int getFunctionID() {
        return functionID;
    }

    /**
     *
     * @return the version of the func
     */
    public String getFunctionVersion() {
        return functionVersion;
    }

    /**
     *
     * @return the name of the func
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return the author of the func
     */
    public String getAuthor() {
        return author;
    }

    /**
     *
     * @return if the current value of this function is in beta mode
     */
    public boolean isBeta() {
        return isBeta;
    }

    /**
     *
     * @return if the current value of this function is private
     */
    public boolean isPrivate() {
        return isPrivate;
    }


    /**
     * Connection between YourFunction.java
     * and the real activity (Function.java)
     *
     * When calling a method,
     * the get, set or call method will be called
     *
     * The call method calls a void method.
     *
     * The get method returns a value of a method
     *
     * The set method sets a value in a void method
     *
     */
    private class Const {

        private Class c;
        private int[] register;
        private int[] used;

        Const(Class c){
            this.c = c;
            register = new int[AppCompatActivity.class.getMethods().length];

            int var0 = 0;

            for(Method method : c.getMethods()){

                if(method.getAnnotations().length>0){

                    for (Annotation annotation : method.getAnnotations()){

                        if(annotation.getClass().equals(Override.class))var0++;

                    }
                }
            }

            used = new int[var0];

            var0 = 0;

            for(Method method : c.getMethods()){

                if(method.getAnnotations().length>0){

                    for (Annotation annotation : method.getAnnotations()){

                        if(annotation.getClass().equals(Override.class)){

                            StringBuilder sb = new StringBuilder();

                            for(Class param : method.getParameterTypes()){

                                sb.append(param.getName().replace(".","9")).append("0");

                            }

                            used[var0] = MethodOverride.valueOf(method.getName()+"$"+sb.toString()).id;
                        }

                    }
                }

                var0++;
            }

        }

        private boolean checkInternal(int id){
            for (int instance: used){
                if(instance == id)return true;
            }
            return false;
        }


        void call(int i,Object... args){

            MethodOverride override = getById(i);
            final String name = override.name();
            //call using java reflection
            String method_name = name.substring(0,name.indexOf("$"));
            String[] params = name.substring((method_name+"$").length(),name.length()-1).split("0");
            ArrayList<Class> classes = new ArrayList<>();

            for (int o = 0; o < params.length; o++){

                System.err.println("check param:"+params[o]);

                try {

                    String param = params[o].replace("9",".");
                    param = param.trim();
                    if(param.equals(""))param=null;
                    if(param !=null && param.contains("_Array")){
                        String x = param.substring(0, param.indexOf("_Array"));
                        param = "[L"+x+";";
                    }
                    if(param!=null) {

                        System.err.println("class for name:"+param);
                        classes.add(Class.forName(param));
                        System.err.println("class:"+Class.forName(param));

                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            try {
                Class[] classes1 = new Class[classes.size()];
                for (int j = 0; j < classes1.length; j++) {
                    classes1[j]=classes.get(j);
                }
                Method method = c.getMethod(method_name,classes1);
                System.err.println(args.length);
                method.invoke(instance,args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                if(e.getClass() == InvocationTargetException.class){
                    e.getCause().printStackTrace();
                }
            }
        }

        Object get(int i,Object... args){
            MethodOverride override = getById(i);
            final String name = Objects.requireNonNull(override).name();
            //call using java reflection
            String method_name = name.substring(0,name.indexOf("$"));
            String[] params = name.substring((method_name+"$").length(),name.length()-1).split("0");
            ArrayList<Class>classes = new ArrayList<>();
            for (int o = 0; o < params.length; o++){
                try {

                    String param = params[o].replace(".","9");
                    param = param.trim();
                    if(param.equals(""))param = null;
                    if(param !=null && param.contains("_Array")){
                        String x = param.substring(0, param.indexOf("_Array"));
                        param = "[L"+x+";";
                    }
                    if(param!=null) {

                        System.err.println(param);
                        classes.add(Class.forName(param));

                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try {
                Class[] classes1 = new Class[classes.size()];
                Method method = c.getMethod(method_name,classes.toArray(classes1));
                return method.invoke(instance,args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new ConnectionException("Invocation can not be proceeded");
            }
        }

        private MethodOverride getById(int id){
            System.err.println("id:"+id);
            for(MethodOverride override:MethodOverride.values()){
                if(override.id==id)return override;
            }
            return null;
        }


        /**
         *
         * @param i the id of the override method
         * @return 1 if the method is already registered, 0 if the method is not registered
         */
        int registerCall(int i){
            if(!checkInternal(i))return 2;
            int var0 = register[i];
            if(var0==0)register[i] = 1;
            return var0;
        }
    }



    @Override public void addContentView(android.view.View var0,android.view.ViewGroup.LayoutParams var1) { if(c!=null&&c.registerCall(0)==0)c.call(0,var0,var1);else super.addContentView(var0,var1);}
    @Override public void applyOverrideConfiguration(android.content.res.Configuration var0) { if(c!=null&&c.registerCall(1)==0)c.call(1,var0);else super.applyOverrideConfiguration(var0);}
    @Override public boolean bindIsolatedService(android.content.Intent var0,int var1,java.lang.String var2,java.util.concurrent.Executor var3,android.content.ServiceConnection var4) { if(c!=null&&c.registerCall(2)==0)return (boolean)c.get(2,var0,var1,var2,var3,var4);else return super.bindIsolatedService(var0,var1,var2,var3,var4);}
    @Override public boolean bindService(android.content.Intent var0,android.content.ServiceConnection var1,int var2) { if(c!=null&&c.registerCall(3)==0)return (boolean)c.get(3,var0,var1,var2);else return super.bindService(var0,var1,var2);}
    @Override public boolean bindService(android.content.Intent var0,int var1,java.util.concurrent.Executor var2,android.content.ServiceConnection var3) { if(c!=null&&c.registerCall(4)==0)return (boolean)c.get(4,var0,var1,var2,var3);else return super.bindService(var0,var1,var2,var3);}
    @Override public int checkCallingOrSelfPermission(java.lang.String var0) { if(c!=null&&c.registerCall(5)==0)return (int)c.get(5,var0);else return super.checkCallingOrSelfPermission(var0);}
    @Override public int checkCallingOrSelfUriPermission(android.net.Uri var0,int var1) { if(c!=null&&c.registerCall(6)==0)return (int)c.get(6,var0,var1);else return super.checkCallingOrSelfUriPermission(var0,var1);}
    @Override public int checkCallingPermission(java.lang.String var0) { if(c!=null&&c.registerCall(7)==0)return (int)c.get(7,var0);else return super.checkCallingPermission(var0);}
    @Override public int checkCallingUriPermission(android.net.Uri var0,int var1) { if(c!=null&&c.registerCall(8)==0)return (int)c.get(8,var0,var1);else return super.checkCallingUriPermission(var0,var1);}
    @Override public int checkPermission(java.lang.String var0,int var1,int var2) { if(c!=null&&c.registerCall(9)==0)return (int)c.get(9,var0,var1,var2);else return super.checkPermission(var0,var1,var2);}
    @Override public int checkSelfPermission(java.lang.String var0) { if(c!=null&&c.registerCall(10)==0)return (int)c.get(10,var0);else return super.checkSelfPermission(var0);}
    @Override public int checkUriPermission(android.net.Uri var0,int var1,int var2,int var3) { if(c!=null&&c.registerCall(11)==0)return (int)c.get(11,var0,var1,var2,var3);else return super.checkUriPermission(var0,var1,var2,var3);}
    @Override public int checkUriPermission(android.net.Uri var0,java.lang.String var1,java.lang.String var2,int var3,int var4,int var5) { if(c!=null&&c.registerCall(12)==0)return (int)c.get(12,var0,var1,var2,var3,var4,var5);else return super.checkUriPermission(var0,var1,var2,var3,var4,var5);}
    @Override public void clearWallpaper() throws IOException { if(c!=null&&c.registerCall(13)==0)c.call(13);else super.clearWallpaper();}
    @Override public void closeContextMenu() { if(c!=null&&c.registerCall(14)==0)c.call(14);else super.closeContextMenu();}
    @Override public void closeOptionsMenu() { if(c!=null&&c.registerCall(15)==0)c.call(15);else super.closeOptionsMenu();}
    @Override public android.content.Context createConfigurationContext(android.content.res.Configuration var0) { if(c!=null&&c.registerCall(16)==0)return (android.content.Context)c.get(16,var0);else return super.createConfigurationContext(var0);}
    @Override public android.content.Context createContextForSplit(java.lang.String var0) throws PackageManager.NameNotFoundException { if(c!=null&&c.registerCall(17)==0)return (android.content.Context)c.get(17,var0);else return super.createContextForSplit(var0);}
    @Override public android.content.Context createDeviceProtectedStorageContext() { if(c!=null&&c.registerCall(18)==0)return (android.content.Context)c.get(18);else return super.createDeviceProtectedStorageContext();}
    @Override public android.content.Context createDisplayContext(android.view.Display var0) { if(c!=null&&c.registerCall(19)==0)return (android.content.Context)c.get(19,var0);else return super.createDisplayContext(var0);}
    @Override public android.content.Context createPackageContext(java.lang.String var0,int var1) throws PackageManager.NameNotFoundException { if(c!=null&&c.registerCall(20)==0)return (android.content.Context)c.get(20,var0,var1);else return super.createPackageContext(var0,var1);}
    @Override public android.app.PendingIntent createPendingResult(int var0,android.content.Intent var1,int var2) { if(c!=null&&c.registerCall(21)==0)return (android.app.PendingIntent)c.get(21,var0,var1,var2);else return super.createPendingResult(var0,var1,var2);}
    @Override public String[] databaseList() { if(c!=null&&c.registerCall(22)==0)return (String[]) c.get(22);else return super.databaseList();}
    @Override public boolean deleteDatabase(java.lang.String var0) { if(c!=null&&c.registerCall(23)==0)return (boolean)c.get(23,var0);else return super.deleteDatabase(var0);}
    @Override public boolean deleteFile(java.lang.String var0) { if(c!=null&&c.registerCall(24)==0)return (boolean)c.get(24,var0);else return super.deleteFile(var0);}
    @Override public boolean deleteSharedPreferences(java.lang.String var0) { if(c!=null&&c.registerCall(25)==0)return (boolean)c.get(25,var0);else return super.deleteSharedPreferences(var0);}
    @Override public boolean dispatchGenericMotionEvent(android.view.MotionEvent var0) { if(c!=null&&c.registerCall(26)==0)return (boolean)c.get(26,var0);else return super.dispatchGenericMotionEvent(var0);}
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent var0) { if(c!=null&&c.registerCall(27)==0)return (boolean)c.get(27,var0);else return super.dispatchKeyEvent(var0);}
    @Override public boolean dispatchKeyShortcutEvent(android.view.KeyEvent var0) { if(c!=null&&c.registerCall(28)==0)return (boolean)c.get(28,var0);else return false;}
    @Override public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent var0) { if(c!=null&&c.registerCall(29)==0)return (boolean)c.get(29,var0);else return super.dispatchPopulateAccessibilityEvent(var0);}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent var0) { if(c!=null&&c.registerCall(30)==0)return (boolean)c.get(30,var0);else return super.dispatchTouchEvent(var0);}
    @Override public boolean dispatchTrackballEvent(android.view.MotionEvent var0) { if(c!=null&&c.registerCall(31)==0)return (boolean)c.get(31,var0);else return super.dispatchTrackballEvent(var0);}
    @Override public void dump(java.lang.String var0,java.io.FileDescriptor var1,java.io.PrintWriter var2,java.lang.String[] var3) { if(c!=null&&c.registerCall(32)==0)c.call(32,var0,var1,var2,var3);else super.dump(var0,var1,var2,var3);}
    @Override public void enforceCallingOrSelfPermission(java.lang.String var0,java.lang.String var1) { if(c!=null&&c.registerCall(33)==0)c.call(33,var0,var1);else super.enforceCallingOrSelfPermission(var0,var1);}
    @Override public void enforceCallingOrSelfUriPermission(android.net.Uri var0,int var1,java.lang.String var2) { if(c!=null&&c.registerCall(34)==0)c.call(34,var0,var1,var2);else super.enforceCallingOrSelfUriPermission(var0,var1,var2);}
    @Override public void enforceCallingPermission(java.lang.String var0,java.lang.String var1) { if(c!=null&&c.registerCall(35)==0)c.call(35,var0,var1);else super.enforceCallingPermission(var0,var1);}
    @Override public void enforceCallingUriPermission(android.net.Uri var0,int var1,java.lang.String var2) { if(c!=null&&c.registerCall(36)==0)c.call(36,var0,var1,var2);else super.enforceCallingUriPermission(var0,var1,var2);}
    @Override public void enforcePermission(java.lang.String var0,int var1,int var2,java.lang.String var3) { if(c!=null&&c.registerCall(37)==0)c.call(37,var0,var1,var2,var3);else super.enforcePermission(var0,var1,var2,var3);}
    @Override public void enforceUriPermission(android.net.Uri var0,int var1,int var2,int var3,java.lang.String var4) { if(c!=null&&c.registerCall(38)==0)c.call(38,var0,var1,var2,var3,var4);else super.enforceUriPermission(var0,var1,var2,var3,var4);}
    @Override public void enforceUriPermission(android.net.Uri var0,java.lang.String var1,java.lang.String var2,int var3,int var4,int var5,java.lang.String var6) { if(c!=null&&c.registerCall(39)==0)c.call(39,var0,var1,var2,var3,var4,var5,var6);else super.enforceUriPermission(var0,var1,var2,var3,var4,var5,var6);}
    @Override public void enterPictureInPictureMode() { if(c!=null&&c.registerCall(40)==0)c.call(40);else super.enterPictureInPictureMode();}
    @Override public boolean enterPictureInPictureMode(android.app.PictureInPictureParams var0) { if(c!=null&&c.registerCall(41)==0)return (boolean)c.get(41,var0);else return super.enterPictureInPictureMode(var0);}
    @Override public boolean equals(java.lang.Object var0) { if(c!=null&&c.registerCall(42)==0)return (boolean)c.get(42,var0);else return super.equals(var0);}
    @Override public String[] fileList() { if(c!=null&&c.registerCall(43)==0)return (String[]) c.get(43);else return super.fileList();}
    @Override public android.view.View findViewById(int var0) { if(c!=null&&c.registerCall(44)==0)return (android.view.View)c.get(44,var0);else return super.findViewById(var0);}
    @Override public void finish() { if(c!=null&&c.registerCall(45)==0)c.call(45);else super.finish();}
    @Override public void finishActivity(int var0) { if(c!=null&&c.registerCall(46)==0)c.call(46,var0);else super.finishActivity(var0);}
    @Override public void finishActivityFromChild(android.app.Activity var0,int var1) { if(c!=null&&c.registerCall(47)==0)c.call(47,var0,var1);else super.finishActivityFromChild(var0,var1);}
    @Override public void finishAffinity() { if(c!=null&&c.registerCall(48)==0)c.call(48);else super.finishAffinity();}
    @Override public void finishAfterTransition() { if(c!=null&&c.registerCall(49)==0)c.call(49);else super.finishAfterTransition();}
    @Override public void finishAndRemoveTask() { if(c!=null&&c.registerCall(50)==0)c.call(50);else super.finishAndRemoveTask();}
    @Override public void finishFromChild(android.app.Activity var0) { if(c!=null&&c.registerCall(51)==0)c.call(51,var0);else super.finishFromChild(var0);}
    @Override public android.app.ActionBar getActionBar() { if(c!=null&&c.registerCall(52)==0)return (android.app.ActionBar)c.get(52);else return super.getActionBar();}
    @Override public android.content.Context getApplicationContext() { if(c!=null&&c.registerCall(53)==0)return (android.content.Context)c.get(53);else return super.getApplicationContext();}
    @Override public android.content.pm.ApplicationInfo getApplicationInfo() { if(c!=null&&c.registerCall(54)==0)return (android.content.pm.ApplicationInfo)c.get(54);else return super.getApplicationInfo();}
    @Override public android.content.res.AssetManager getAssets() { if(c!=null&&c.registerCall(55)==0)return (android.content.res.AssetManager)c.get(55);else return super.getAssets();}
    @Override public android.content.Context getBaseContext() { if(c!=null&&c.registerCall(56)==0)return (android.content.Context)c.get(56);else return super.getBaseContext();}
    @Override public java.io.File getCacheDir() { if(c!=null&&c.registerCall(57)==0)return (java.io.File)c.get(57);else return super.getCacheDir();}
    @Override public android.content.ComponentName getCallingActivity() { if(c!=null&&c.registerCall(58)==0)return (android.content.ComponentName)c.get(58);else return super.getCallingActivity();}
    @Override public java.lang.String getCallingPackage() { if(c!=null&&c.registerCall(59)==0)return (java.lang.String)c.get(59);else return super.getCallingPackage();}
    @Override public int getChangingConfigurations() { if(c!=null&&c.registerCall(60)==0)return (int)c.get(60);else return super.getChangingConfigurations();}
    @Override public java.lang.ClassLoader getClassLoader() { if(c!=null&&c.registerCall(61)==0)return (java.lang.ClassLoader)c.get(61);else return super.getClassLoader();}
    @Override public java.io.File getCodeCacheDir() { if(c!=null&&c.registerCall(62)==0)return (java.io.File)c.get(62);else return super.getCodeCacheDir();}
    @Override public android.content.ComponentName getComponentName() { if(c!=null&&c.registerCall(63)==0)return (android.content.ComponentName)c.get(63);else try{return super.getComponentName();}catch (NullPointerException npe){return null;}}
    @Override public android.content.ContentResolver getContentResolver() { if(c!=null&&c.registerCall(64)==0)return (android.content.ContentResolver)c.get(64);else return super.getContentResolver();}
    @Override public android.transition.Scene getContentScene() { if(c!=null&&c.registerCall(65)==0)return (android.transition.Scene)c.get(65);else return super.getContentScene();}
    @Override public android.transition.TransitionManager getContentTransitionManager() { if(c!=null&&c.registerCall(66)==0)return (android.transition.TransitionManager)c.get(66);else return super.getContentTransitionManager();}
    @Override public android.view.View getCurrentFocus() { if(c!=null&&c.registerCall(67)==0)return (android.view.View)c.get(67);else return super.getCurrentFocus();}
    @Override public java.io.File getDataDir() { if(c!=null&&c.registerCall(68)==0)return (java.io.File)c.get(68);else return super.getDataDir();}
    @Override public java.io.File getDatabasePath(java.lang.String var0) { if(c!=null&&c.registerCall(69)==0)return (java.io.File)c.get(69,var0);else return super.getDatabasePath(var0);}
    @Override public androidx.appcompat.app.AppCompatDelegate getDelegate() { if(c!=null&&c.registerCall(70)==0)return (androidx.appcompat.app.AppCompatDelegate)c.get(70);else return super.getDelegate();}
    @Override public java.io.File getDir(java.lang.String var0,int var1) { if(c!=null&&c.registerCall(71)==0)return (java.io.File)c.get(71,var0,var1);else return super.getDir(var0,var1);}
    @Override public androidx.appcompat.app.ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() { if(c!=null&&c.registerCall(72)==0)return (androidx.appcompat.app.ActionBarDrawerToggle.Delegate)c.get(72);else return super.getDrawerToggleDelegate();}
    @Override public java.io.File getExternalCacheDir() { if(c!=null&&c.registerCall(73)==0)return (java.io.File)c.get(73);else return super.getExternalCacheDir();}
    @Override public File[] getExternalCacheDirs() { if(c!=null&&c.registerCall(74)==0)return (File[]) c.get(74);else return super.getExternalCacheDirs();}
    @Override public java.io.File getExternalFilesDir(java.lang.String var0) { if(c!=null&&c.registerCall(75)==0)return (java.io.File)c.get(75,var0);else return super.getExternalFilesDir(var0);}
    @Override public File[] getExternalFilesDirs(String var0) { if(c!=null&&c.registerCall(76)==0)return (File[]) c.get(76,var0);else return super.getExternalFilesDirs(var0);}
    @Override public File[] getExternalMediaDirs() { if(c!=null&&c.registerCall(77)==0)return (File[]) c.get(77);else return super.getExternalMediaDirs();}
    @Override public androidx.core.app.ComponentActivity.ExtraData getExtraData(java.lang.Class var0) { if(c!=null&&c.registerCall(78)==0)return (androidx.core.app.ComponentActivity.ExtraData)c.get(78,var0);else return null;}
    @Override public java.io.File getFileStreamPath(java.lang.String var0) { if(c!=null&&c.registerCall(79)==0)return (java.io.File)c.get(79,var0);else return super.getFileStreamPath(var0);}
    @Override public java.io.File getFilesDir() { if(c!=null&&c.registerCall(80)==0)return (java.io.File)c.get(80);else return super.getFilesDir();}
    @Override public android.app.FragmentManager getFragmentManager() { if(c!=null&&c.registerCall(81)==0)return (android.app.FragmentManager)c.get(81);else return super.getFragmentManager();}
    @Override public android.content.Intent getIntent() { if(c!=null&&c.registerCall(82)==0)return (android.content.Intent)c.get(82);else return super.getIntent();}
    @Override public java.lang.Object getLastCustomNonConfigurationInstance() { if(c!=null&&c.registerCall(83)==0)return (java.lang.Object)c.get(83);else return super.getLastCustomNonConfigurationInstance();}
    @Override public java.lang.Object getLastNonConfigurationInstance() { if(c!=null&&c.registerCall(84)==0)return (java.lang.Object)c.get(84);else return super.getLastNonConfigurationInstance();}
    @Override public android.view.LayoutInflater getLayoutInflater() { if(c!=null&&c.registerCall(85)==0)return (android.view.LayoutInflater)c.get(85);else return super.getLayoutInflater();}
    @Override public androidx.lifecycle.Lifecycle getLifecycle() { if(c!=null&&c.registerCall(86)==0)return (androidx.lifecycle.Lifecycle)c.get(86);else return super.getLifecycle();}
    @Override public android.app.LoaderManager getLoaderManager() { if(c!=null&&c.registerCall(87)==0)return (android.app.LoaderManager)c.get(87);else return super.getLoaderManager();}
    @Override public java.lang.String getLocalClassName() { if(c!=null&&c.registerCall(88)==0)return (java.lang.String)c.get(88);else return super.getLocalClassName();}
    @Override public java.util.concurrent.Executor getMainExecutor() { if(c!=null&&c.registerCall(89)==0)return (java.util.concurrent.Executor)c.get(89);else return super.getMainExecutor();}
    @Override public android.os.Looper getMainLooper() { if(c!=null&&c.registerCall(90)==0)return (android.os.Looper)c.get(90);else return super.getMainLooper();}
    @Override public int getMaxNumPictureInPictureActions() { if(c!=null&&c.registerCall(91)==0)return (int)c.get(91);else return super.getMaxNumPictureInPictureActions();}
    @Override public android.view.MenuInflater getMenuInflater() { if(c!=null&&c.registerCall(92)==0)return (android.view.MenuInflater)c.get(92);else return super.getMenuInflater();}
    @Override public java.io.File getNoBackupFilesDir() { if(c!=null&&c.registerCall(93)==0)return (java.io.File)c.get(93);else return super.getNoBackupFilesDir();}
    @Override public java.io.File getObbDir() { if(c!=null&&c.registerCall(94)==0)return (java.io.File)c.get(94);else return super.getObbDir();}
    @Override public File[] getObbDirs() { if(c!=null&&c.registerCall(95)==0)return (File[]) c.get(95);else return super.getObbDirs();}
    @Override public java.lang.String getOpPackageName() { if(c!=null&&c.registerCall(96)==0)return (java.lang.String)c.get(96);else return super.getOpPackageName();}
    @Override public java.lang.String getPackageCodePath() { if(c!=null&&c.registerCall(97)==0)return (java.lang.String)c.get(97);else return super.getPackageCodePath();}
    @Override public android.content.pm.PackageManager getPackageManager() { if(c!=null&&c.registerCall(98)==0)return (android.content.pm.PackageManager)c.get(98);else return super.getPackageManager();}
    @Override public java.lang.String getPackageName() { if(c!=null&&c.registerCall(99)==0)return (java.lang.String)c.get(99);else return super.getPackageName();}
    @Override public java.lang.String getPackageResourcePath() { if(c!=null&&c.registerCall(100)==0)return (java.lang.String)c.get(100);else return super.getPackageResourcePath();}
    @Override public android.content.Intent getParentActivityIntent() { if(c!=null&&c.registerCall(101)==0)return (android.content.Intent)c.get(101);else return super.getParentActivityIntent();}
    @Override public android.content.SharedPreferences getPreferences(int var0) { if(c!=null&&c.registerCall(102)==0)return (android.content.SharedPreferences)c.get(102,var0);else return super.getPreferences(var0);}
    @Override public android.net.Uri getReferrer() { if(c!=null&&c.registerCall(103)==0)return (android.net.Uri)c.get(103);else return super.getReferrer();}
    @Override public int getRequestedOrientation() { if(c!=null&&c.registerCall(104)==0)return (int)c.get(104);else return super.getRequestedOrientation();}
    @Override public android.content.res.Resources getResources() { if(c!=null&&c.registerCall(105)==0)return (android.content.res.Resources)c.get(105);else return super.getResources();}
    @Override public android.content.SharedPreferences getSharedPreferences(java.lang.String var0,int var1) { if(c!=null&&c.registerCall(106)==0)return (android.content.SharedPreferences)c.get(106,var0,var1);else return super.getSharedPreferences(var0,var1);}
    @Override public androidx.appcompat.app.ActionBar getSupportActionBar() { if(c!=null&&c.registerCall(107)==0)return (androidx.appcompat.app.ActionBar)c.get(107);else return super.getSupportActionBar();}
    @Override public androidx.fragment.app.FragmentManager getSupportFragmentManager() { if(c!=null&&c.registerCall(108)==0)return (androidx.fragment.app.FragmentManager)c.get(108);else return super.getSupportFragmentManager();}
    @Override public androidx.loader.app.LoaderManager getSupportLoaderManager() { if(c!=null&&c.registerCall(109)==0)return (androidx.loader.app.LoaderManager)c.get(109);else return super.getSupportLoaderManager();}
    @Override public android.content.Intent getSupportParentActivityIntent() { if(c!=null&&c.registerCall(110)==0)return (android.content.Intent)c.get(110);else return super.getSupportParentActivityIntent();}
    @Override public java.lang.Object getSystemService(java.lang.String var0) { if(c!=null&&c.registerCall(111)==0)return (java.lang.Object)c.get(111,var0);else return super.getSystemService(var0);}
    @Override public java.lang.String getSystemServiceName(java.lang.Class var0) { if(c!=null&&c.registerCall(112)==0)return (java.lang.String)c.get(112,var0);else return super.getSystemServiceName(var0);}
    @Override public int getTaskId() { if(c!=null&&c.registerCall(113)==0)return (int)c.get(113);else return super.getTaskId();}
    @Override public android.content.res.Resources.Theme getTheme() { if(c!=null&&c.registerCall(114)==0)return (android.content.res.Resources.Theme)c.get(114);else return super.getTheme();}
    @Override public androidx.lifecycle.ViewModelStore getViewModelStore() { if(c!=null&&c.registerCall(115)==0)return (androidx.lifecycle.ViewModelStore)c.get(115);else return super.getViewModelStore();}
    @Override public android.app.VoiceInteractor getVoiceInteractor() { if(c!=null&&c.registerCall(116)==0)return (android.app.VoiceInteractor)c.get(116);else return super.getVoiceInteractor();}
    @Override public android.graphics.drawable.Drawable getWallpaper() { if(c!=null&&c.registerCall(117)==0)return (android.graphics.drawable.Drawable)c.get(117);else return super.getWallpaper();}
    @Override public int getWallpaperDesiredMinimumHeight() { if(c!=null&&c.registerCall(118)==0)return (int)c.get(118);else return super.getWallpaperDesiredMinimumHeight();}
    @Override public int getWallpaperDesiredMinimumWidth() { if(c!=null&&c.registerCall(119)==0)return (int)c.get(119);else return super.getWallpaperDesiredMinimumWidth();}
    @Override public android.view.Window getWindow() { if(c!=null&&c.registerCall(120)==0)return (android.view.Window)c.get(120);else return super.getWindow();}
    @Override public android.view.WindowManager getWindowManager() { if(c!=null&&c.registerCall(121)==0)return (android.view.WindowManager)c.get(121);else return super.getWindowManager();}
    @Override public void grantUriPermission(java.lang.String var0,android.net.Uri var1,int var2) { if(c!=null&&c.registerCall(122)==0)c.call(122,var0,var1,var2);else super.grantUriPermission(var0,var1,var2);}
    @Override public boolean hasWindowFocus() { if(c!=null&&c.registerCall(123)==0)return (boolean)c.get(123);else return super.hasWindowFocus();}
    @Override public int hashCode() { if(c!=null&&c.registerCall(124)==0)return (int)c.get(124);else return super.hashCode();}
    @Override public void invalidateOptionsMenu() { if(c!=null&&c.registerCall(125)==0)c.call(125);else super.invalidateOptionsMenu();}
    @Override public boolean isActivityTransitionRunning() { if(c!=null&&c.registerCall(126)==0)return (boolean)c.get(126);else return super.isActivityTransitionRunning();}
    @Override public boolean isChangingConfigurations() { if(c!=null&&c.registerCall(127)==0)return (boolean)c.get(127);else return super.isChangingConfigurations();}
    @Override public boolean isDestroyed() { if(c!=null&&c.registerCall(128)==0)return (boolean)c.get(128);else return super.isDestroyed();}
    @Override public boolean isDeviceProtectedStorage() { if(c!=null&&c.registerCall(129)==0)return (boolean)c.get(129);else return super.isDeviceProtectedStorage();}
    @Override public boolean isFinishing() { if(c!=null&&c.registerCall(130)==0)return (boolean)c.get(130);else return super.isFinishing();}
    @Override public boolean isImmersive() { if(c!=null&&c.registerCall(131)==0)return (boolean)c.get(131);else return super.isImmersive();}
    @Override public boolean isInMultiWindowMode() { if(c!=null&&c.registerCall(132)==0)return (boolean)c.get(132);else return super.isInMultiWindowMode();}
    @Override public boolean isInPictureInPictureMode() { if(c!=null&&c.registerCall(133)==0)return (boolean)c.get(133);else return super.isInPictureInPictureMode();}
    @Override public boolean isLocalVoiceInteractionSupported() { if(c!=null&&c.registerCall(134)==0)return (boolean)c.get(134);else return super.isLocalVoiceInteractionSupported();}
    @Override public boolean isRestricted() { if(c!=null&&c.registerCall(135)==0)return (boolean)c.get(135);else return super.isRestricted();}
    @Override public boolean isTaskRoot() { if(c!=null&&c.registerCall(136)==0)return (boolean)c.get(136);else return super.isTaskRoot();}
    @Override public boolean isVoiceInteraction() { if(c!=null&&c.registerCall(137)==0)return (boolean)c.get(137);else return super.isVoiceInteraction();}
    @Override public boolean isVoiceInteractionRoot() { if(c!=null&&c.registerCall(138)==0)return (boolean)c.get(138);else return super.isVoiceInteractionRoot();}
    @Override public boolean moveDatabaseFrom(android.content.Context var0,java.lang.String var1) { if(c!=null&&c.registerCall(139)==0)return (boolean)c.get(139,var0,var1);else return super.moveDatabaseFrom(var0,var1);}
    @Override public boolean moveSharedPreferencesFrom(android.content.Context var0,java.lang.String var1) { if(c!=null&&c.registerCall(140)==0)return (boolean)c.get(140,var0,var1);else return super.moveSharedPreferencesFrom(var0,var1);}
    @Override public boolean moveTaskToBack(boolean var0) { if(c!=null&&c.registerCall(141)==0)return (boolean)c.get(141,var0);else return super.moveTaskToBack(var0);}
    @Override public boolean navigateUpTo(android.content.Intent var0) { if(c!=null&&c.registerCall(142)==0)return (boolean)c.get(142,var0);else return super.navigateUpTo(var0);}
    @Override public boolean navigateUpToFromChild(android.app.Activity var0,android.content.Intent var1) { if(c!=null&&c.registerCall(143)==0)return (boolean)c.get(143,var0,var1);else return super.navigateUpToFromChild(var0,var1);}
    @Override public void onActionModeFinished(android.view.ActionMode var0) { if(c!=null&&c.registerCall(144)==0)c.call(144,var0);else super.onActionModeFinished(var0);}
    @Override public void onActionModeStarted(android.view.ActionMode var0) { if(c!=null&&c.registerCall(145)==0)c.call(145,var0);else super.onActionModeStarted(var0);}
    @Override public void onActivityReenter(int var0,android.content.Intent var1) { if(c!=null&&c.registerCall(146)==0)c.call(146,var0,var1);else super.onActivityReenter(var0,var1);}
    @Override public void onAttachFragment(android.app.Fragment var0) { if(c!=null&&c.registerCall(147)==0)c.call(147,var0);else super.onAttachFragment(var0);}
    @Override public void onAttachFragment(androidx.fragment.app.Fragment var0) { if(c!=null&&c.registerCall(148)==0)c.call(148,var0);else super.onAttachFragment(var0);}
    @Override public void onAttachedToWindow() { if(c!=null&&c.registerCall(149)==0)c.call(149);else super.onAttachedToWindow();}
    @Override public void onBackPressed() { if(c!=null&&c.registerCall(150)==0)c.call(150);else super.onBackPressed();}
    @Override public void onConfigurationChanged(android.content.res.Configuration var0) { if(c!=null&&c.registerCall(151)==0)c.call(151,var0);else super.onConfigurationChanged(var0);}
    @Override public void onContentChanged() { if(c!=null&&c.registerCall(152)==0)c.call(152);else super.onContentChanged();}
    @Override public boolean onContextItemSelected(android.view.MenuItem var0) { if(c!=null&&c.registerCall(153)==0)return (boolean)c.get(153,var0);else return super.onContextItemSelected(var0);}
    @Override public void onContextMenuClosed(android.view.Menu var0) { if(c!=null&&c.registerCall(154)==0)c.call(154,var0);else super.onContextMenuClosed(var0);}
    @Override public void onCreate(android.os.Bundle var0,android.os.PersistableBundle var1) { if(c!=null&&c.registerCall(155)==0)c.call(155,var0,var1);else super.onCreate(var0,var1);}
    @Override public void onCreateContextMenu(android.view.ContextMenu var0,android.view.View var1,android.view.ContextMenu.ContextMenuInfo var2) { if(c!=null&&c.registerCall(156)==0)c.call(156,var0,var1,var2);else super.onCreateContextMenu(var0,var1,var2);}
    @Override public java.lang.CharSequence onCreateDescription() { if(c!=null&&c.registerCall(157)==0)return (java.lang.CharSequence)c.get(157);else return super.onCreateDescription();}
    @Override public void onCreateNavigateUpTaskStack(android.app.TaskStackBuilder var0) { if(c!=null&&c.registerCall(158)==0)c.call(158,var0);else super.onCreateNavigateUpTaskStack(var0);}
    @Override public boolean onCreateOptionsMenu(android.view.Menu var0) { if(c!=null&&c.registerCall(159)==0)return (boolean)c.get(159,var0);else return super.onCreateOptionsMenu(var0);}
    @Override public boolean onCreatePanelMenu(int var0,android.view.Menu var1) { if(c!=null&&c.registerCall(160)==0)return (boolean)c.get(160,var0,var1);else return super.onCreatePanelMenu(var0,var1);}
    @Override public android.view.View onCreatePanelView(int var0) { if(c!=null&&c.registerCall(161)==0)return (android.view.View)c.get(161,var0);else return super.onCreatePanelView(var0);}
    @Override public void onCreateSupportNavigateUpTaskStack(androidx.core.app.TaskStackBuilder var0) { if(c!=null&&c.registerCall(162)==0)c.call(162,var0);else super.onCreateSupportNavigateUpTaskStack(var0);}
    @Override public boolean onCreateThumbnail(android.graphics.Bitmap var0,android.graphics.Canvas var1) { if(c!=null&&c.registerCall(163)==0)return (boolean)c.get(163,var0,var1);else return super.onCreateThumbnail(var0,var1);}
    @Override public android.view.View onCreateView(java.lang.String var0,android.content.Context var1,android.util.AttributeSet var2) { if(c!=null&&c.registerCall(164)==0)return (android.view.View)c.get(164,var0,var1,var2);else return super.onCreateView(var0,var1,var2);}
    @Override public android.view.View onCreateView(android.view.View var0,java.lang.String var1,android.content.Context var2,android.util.AttributeSet var3) { if(c!=null&&c.registerCall(165)==0)return (android.view.View)c.get(165,var0,var1,var2,var3);else return super.onCreateView(var0,var1,var2,var3);}
    @Override public void onDetachedFromWindow() { if(c!=null&&c.registerCall(166)==0)c.call(166);else super.onDetachedFromWindow();}
    @Override public void onEnterAnimationComplete() { if(c!=null&&c.registerCall(167)==0)c.call(167);else super.onEnterAnimationComplete();}
    @Override public boolean onGenericMotionEvent(android.view.MotionEvent var0) { if(c!=null&&c.registerCall(168)==0)return (boolean)c.get(168,var0);else return super.onGenericMotionEvent(var0);}
    @Override public void onGetDirectActions(android.os.CancellationSignal var0,java.util.function.Consumer var1) { if(c!=null&&c.registerCall(169)==0)c.call(169,var0,var1);else super.onGetDirectActions(var0,var1);}
    @Override public boolean onKeyDown(int var0,android.view.KeyEvent var1) { if(c!=null&&c.registerCall(170)==0)return (boolean)c.get(170,var0,var1);else return super.onKeyDown(var0,var1);}
    @Override public boolean onKeyLongPress(int var0,android.view.KeyEvent var1) { if(c!=null&&c.registerCall(171)==0)return (boolean)c.get(171,var0,var1);else return super.onKeyLongPress(var0,var1);}
    @Override public boolean onKeyMultiple(int var0,int var1,android.view.KeyEvent var2) { if(c!=null&&c.registerCall(172)==0)return (boolean)c.get(172,var0,var1,var2);else return super.onKeyMultiple(var0,var1,var2);}
    @Override public boolean onKeyShortcut(int var0,android.view.KeyEvent var1) { if(c!=null&&c.registerCall(173)==0)return (boolean)c.get(173,var0,var1);else return super.onKeyShortcut(var0,var1);}
    @Override public boolean onKeyUp(int var0,android.view.KeyEvent var1) { if(c!=null&&c.registerCall(174)==0)return (boolean)c.get(174,var0,var1);else return super.onKeyUp(var0,var1);}
    @Override public void onLocalVoiceInteractionStarted() { if(c!=null&&c.registerCall(175)==0)c.call(175);else super.onLocalVoiceInteractionStarted();}
    @Override public void onLocalVoiceInteractionStopped() { if(c!=null&&c.registerCall(176)==0)c.call(176);else super.onLocalVoiceInteractionStopped();}
    @Override public void onLowMemory() { if(c!=null&&c.registerCall(177)==0)c.call(177);else super.onLowMemory();}
    @Override public boolean onMenuOpened(int var0,android.view.Menu var1) { if(c!=null&&c.registerCall(178)==0)return (boolean)c.get(178,var0,var1);else return super.onMenuOpened(var0,var1);}
    @Override public void onMultiWindowModeChanged(boolean var0) { if(c!=null&&c.registerCall(179)==0)c.call(179,var0);else super.onMultiWindowModeChanged(var0);}
    @Override public void onSupportContentChanged() { if(c!=null&&c.registerCall(180)==0)c.call(180);else super.onSupportContentChanged();}
    @Override public boolean onSupportNavigateUp() { if(c!=null&&c.registerCall(181)==0)return (boolean)c.get(181);else return super.onSupportNavigateUp();}
    @Override public void onTopResumedActivityChanged(boolean var0) { if(c!=null&&c.registerCall(182)==0)c.call(182,var0);else super.onTopResumedActivityChanged(var0);}
    @Override public boolean onTouchEvent(android.view.MotionEvent var0) { if(c!=null&&c.registerCall(183)==0)return (boolean)c.get(183,var0);else return super.onTouchEvent(var0);}
    @Override public boolean onTrackballEvent(android.view.MotionEvent var0) { if(c!=null&&c.registerCall(184)==0)return (boolean)c.get(184,var0);else return super.onTrackballEvent(var0);}
    @Override public void onTrimMemory(int var0) { if(c!=null&&c.registerCall(185)==0)c.call(185,var0);else super.onTrimMemory(var0);}
    @Override public void onUserInteraction() { if(c!=null&&c.registerCall(186)==0)c.call(186);else super.onUserInteraction();}
    @Override public void onVisibleBehindCanceled() { if(c!=null&&c.registerCall(187)==0)c.call(187);else super.onVisibleBehindCanceled();}
    @Override public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams var0) { if(c!=null&&c.registerCall(188)==0)c.call(188,var0);else super.onWindowAttributesChanged(var0);}
    @Override public void onWindowFocusChanged(boolean var0) { if(c!=null&&c.registerCall(189)==0)c.call(189,var0);else super.onWindowFocusChanged(var0);}
    @Override public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback var0) { if(c!=null&&c.registerCall(190)==0)return (android.view.ActionMode)c.get(190,var0);else return super.onWindowStartingActionMode(var0);}
    @Override public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback var0,int var1) { if(c!=null&&c.registerCall(191)==0)return (android.view.ActionMode)c.get(191,var0,var1);else return super.onWindowStartingActionMode(var0,var1);}
    @Override public androidx.appcompat.view.ActionMode onWindowStartingSupportActionMode(androidx.appcompat.view.ActionMode.Callback var0) { if(c!=null&&c.registerCall(192)==0)return (androidx.appcompat.view.ActionMode)c.get(192,var0);else return super.onWindowStartingSupportActionMode(var0);}
    @Override public void openContextMenu(android.view.View var0) { if(c!=null&&c.registerCall(193)==0)c.call(193,var0);else super.openContextMenu(var0);}
    @Override public java.io.FileInputStream openFileInput(java.lang.String var0) throws FileNotFoundException { if(c!=null&&c.registerCall(194)==0)return (java.io.FileInputStream)c.get(194,var0);else return super.openFileInput(var0);}
    @Override public java.io.FileOutputStream openFileOutput(java.lang.String var0,int var1) throws FileNotFoundException { if(c!=null&&c.registerCall(195)==0)return (java.io.FileOutputStream)c.get(195,var0,var1);else return super.openFileOutput(var0,var1);}
    @Override public void openOptionsMenu() { if(c!=null&&c.registerCall(196)==0)c.call(196);else super.openOptionsMenu();}
    @Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String var0,int var1,android.database.sqlite.SQLiteDatabase.CursorFactory var2) { if(c!=null&&c.registerCall(197)==0)return (android.database.sqlite.SQLiteDatabase)c.get(197,var0,var1,var2);else return super.openOrCreateDatabase(var0,var1,var2);}
    @Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String var0,int var1,android.database.sqlite.SQLiteDatabase.CursorFactory var2,android.database.DatabaseErrorHandler var3) { if(c!=null&&c.registerCall(198)==0)return (android.database.sqlite.SQLiteDatabase)c.get(198,var0,var1,var2,var3);else return super.openOrCreateDatabase(var0,var1,var2,var3);}
    @Override public void overridePendingTransition(int var0,int var1) { if(c!=null&&c.registerCall(199)==0)c.call(199,var0,var1);else super.overridePendingTransition(var0,var1);}
    @Override public boolean supportRequestWindowFeature(int var0) { if(c!=null&&c.registerCall(309)==0)return (boolean)c.get(309,var0);else return super.supportRequestWindowFeature(var0);}
    @Override public boolean supportShouldUpRecreateTask(android.content.Intent var0) { if(c!=null&&c.registerCall(310)==0)return (boolean)c.get(310,var0);else return super.supportShouldUpRecreateTask(var0);}
    @Override public void supportStartPostponedEnterTransition() { if(c!=null&&c.registerCall(311)==0)c.call(311);else super.supportStartPostponedEnterTransition();}
    @Override public void takeKeyEvents(boolean var0) { if(c!=null&&c.registerCall(312)==0)c.call(312,var0);else super.takeKeyEvents(var0);}
    @Override public java.lang.String toString() { if(c!=null&&c.registerCall(313)==0)return (java.lang.String)c.get(313);else return super.toString();}
    @Override public void triggerSearch(java.lang.String var0,android.os.Bundle var1) { if(c!=null&&c.registerCall(314)==0)c.call(314,var0,var1);else super.triggerSearch(var0,var1);}
    @Override public void unbindService(android.content.ServiceConnection var0) { if(c!=null&&c.registerCall(315)==0)c.call(315,var0);else super.unbindService(var0);}
    @Override public void unregisterActivityLifecycleCallbacks(android.app.Application.ActivityLifecycleCallbacks var0) { if(c!=null&&c.registerCall(316)==0)c.call(316,var0);else super.unregisterActivityLifecycleCallbacks(var0);}
    @Override public void unregisterComponentCallbacks(android.content.ComponentCallbacks var0) { if(c!=null&&c.registerCall(317)==0)c.call(317,var0);else super.unregisterComponentCallbacks(var0);}
    @Override public void unregisterForContextMenu(android.view.View var0) { if(c!=null&&c.registerCall(318)==0)c.call(318,var0);else super.unregisterForContextMenu(var0);}
    @Override public void unregisterReceiver(android.content.BroadcastReceiver var0) { if(c!=null&&c.registerCall(319)==0)c.call(319,var0);else super.unregisterReceiver(var0);}
    @Override public void updateServiceGroup(android.content.ServiceConnection var0,int var1,int var2) { if(c!=null&&c.registerCall(320)==0)c.call(320,var0,var1,var2);else super.updateServiceGroup(var0,var1,var2);}
    @Override public void attachBaseContext(android.content.Context var0) { if(c!=null&&c.registerCall(321)==0)c.call(321,var0);else super.attachBaseContext(var0);}
    @Override public void onApplyThemeResource(android.content.res.Resources.Theme var0,int var1,boolean var2) { if(c!=null&&c.registerCall(322)==0)c.call(322,var0,var1,var2);else super.onApplyThemeResource(var0,var1,var2);}
    @Override public void onChildTitleChanged(android.app.Activity var0,java.lang.CharSequence var1) { if(c!=null&&c.registerCall(323)==0)c.call(323,var0,var1);else super.onChildTitleChanged(var0,var1);}
    @Override public void onCreate(android.os.Bundle var0) {

        try {
            super.onCreate(var0);
        }catch (IllegalStateException ignored){
            /*
            happens when you call super.onCreate(var0) in you overridden method
             */
        }
        /*
        init c and instance
         */

        if (c == null || c.registerCall(324) == 0) {

            try {

                Class activity_not_in_manifest = FunctionHandler1.loaded_classes.get(super.getIntent().getStringExtra("function_name"));
                this.c = new Const(Objects.requireNonNull(activity_not_in_manifest));
                try {
                    this.instance = activity_not_in_manifest.getConstructor(Function.class).newInstance(this);
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new ConnectionException("Instance cannot be null!");
                }

                c.call(324, var0);

            }catch (NullPointerException ignored){

                /*
                happens when you call super.onCreate(var0) in you overridden method
                 */

            }
        }
    }
    @Override public android.app.Dialog onCreateDialog(int var0) { if(c!=null&&c.registerCall(325)==0)return (android.app.Dialog)c.get(325,var0);else return super.onCreateDialog(var0);}
    @Override public android.app.Dialog onCreateDialog(int var0,android.os.Bundle var1) { if(c!=null&&c.registerCall(326)==0)return (android.app.Dialog)c.get(326,var0,var1);else return super.onCreateDialog(var0,var1);}
    @Override public void onDestroy() { if(c!=null&&c.registerCall(327)==0)c.call(327);else super.onDestroy();}
    @Override public void onMultiWindowModeChanged(boolean var0,android.content.res.Configuration var1) { if(c!=null&&c.registerCall(328)==0)c.call(328,var0,var1);else super.onMultiWindowModeChanged(var0,var1);}
    @Override public boolean onNavigateUp() { if(c!=null&&c.registerCall(329)==0)return (boolean)c.get(329);else return super.onNavigateUp();}
    @Override public boolean onNavigateUpFromChild(android.app.Activity var0) { if(c!=null&&c.registerCall(330)==0)return (boolean)c.get(330,var0);else return super.onNavigateUpFromChild(var0);}
    @Override public void onNewIntent(android.content.Intent var0) { if(c!=null&&c.registerCall(331)==0)c.call(331,var0);else super.onNewIntent(var0);}
    @Override public boolean onOptionsItemSelected(android.view.MenuItem var0) { if(c!=null&&c.registerCall(332)==0)return (boolean)c.get(332,var0);else return super.onOptionsItemSelected(var0);}
    @Override public void onOptionsMenuClosed(android.view.Menu var0) { if(c!=null&&c.registerCall(333)==0)c.call(333,var0);else super.onOptionsMenuClosed(var0);}
    @Override public void onPanelClosed(int var0,android.view.Menu var1) { if(c!=null&&c.registerCall(334)==0)c.call(334,var0,var1);else super.onPanelClosed(var0,var1);}
    @Override public void onPause() { if(c!=null&&c.registerCall(335)==0)c.call(335);else super.onPause();}
    @Override public void onPerformDirectAction(java.lang.String var0,android.os.Bundle var1,android.os.CancellationSignal var2,java.util.function.Consumer var3) { if(c!=null&&c.registerCall(336)==0)c.call(336,var0,var1,var2,var3);else super.onPerformDirectAction(var0,var1,var2,var3);}
    @Override public void onPictureInPictureModeChanged(boolean var0) { if(c!=null&&c.registerCall(337)==0)c.call(337,var0);else super.onPictureInPictureModeChanged(var0);}
    @Override public void onPictureInPictureModeChanged(boolean var0,android.content.res.Configuration var1) { if(c!=null&&c.registerCall(338)==0)c.call(338,var0,var1);else super.onPictureInPictureModeChanged(var0,var1);}
    @Override public void onPostCreate(android.os.Bundle var0) { if(c!=null&&c.registerCall(339)==0)c.call(339,var0);else super.onPostCreate(var0);}
    @Override public void onPostCreate(android.os.Bundle var0,android.os.PersistableBundle var1) { if(c!=null&&c.registerCall(340)==0)c.call(340,var0,var1);else super.onPostCreate(var0,var1);}
    @Override public void onPostResume() { if(c!=null&&c.registerCall(341)==0)c.call(341);else super.onPostResume();}
    @Override public void onPrepareDialog(int var0,android.app.Dialog var1) { if(c!=null&&c.registerCall(342)==0)c.call(342,var0,var1);else super.onPrepareDialog(var0,var1);}
    @Override public void onPrepareDialog(int var0,android.app.Dialog var1,android.os.Bundle var2) { if(c!=null&&c.registerCall(343)==0)c.call(343,var0,var1,var2);else super.onPrepareDialog(var0,var1,var2);}
    @Override public void onPrepareNavigateUpTaskStack(android.app.TaskStackBuilder var0) { if(c!=null&&c.registerCall(344)==0)c.call(344,var0);else super.onPrepareNavigateUpTaskStack(var0);}
    @Override public boolean onPrepareOptionsMenu(android.view.Menu var0) { if(c!=null&&c.registerCall(345)==0)return (boolean)c.get(345,var0);else return super.onPrepareOptionsMenu(var0);}
    @Override public boolean onPreparePanel(int var0,android.view.View var1,android.view.Menu var2) { if(c!=null&&c.registerCall(346)==0)return (boolean)c.get(346,var0,var1,var2);else return super.onPreparePanel(var0,var1,var2);}
    @Override public void onProvideAssistContent(android.app.assist.AssistContent var0) { if(c!=null&&c.registerCall(347)==0)c.call(347,var0);else super.onProvideAssistContent(var0);}
    @Override public void onProvideAssistData(android.os.Bundle var0) { if(c!=null&&c.registerCall(348)==0)c.call(348,var0);else super.onProvideAssistData(var0);}
    @Override public void onProvideKeyboardShortcuts(java.util.List var0,android.view.Menu var1,int var2) { if(c!=null&&c.registerCall(349)==0)c.call(349,var0,var1,var2);else super.onProvideKeyboardShortcuts(var0,var1,var2);}
    @Override public android.net.Uri onProvideReferrer() { if(c!=null&&c.registerCall(350)==0)return (android.net.Uri)c.get(350);else return super.onProvideReferrer();}
    @Override public void onRestart() { if(c!=null&&c.registerCall(351)==0)c.call(351);else super.onRestart();}
    @Override public void onRestoreInstanceState(android.os.Bundle var0) { if(c!=null&&c.registerCall(352)==0)c.call(352,var0);else super.onRestoreInstanceState(var0);}
    @Override public void onRestoreInstanceState(android.os.Bundle var0,android.os.PersistableBundle var1) { if(c!=null&&c.registerCall(353)==0)c.call(353,var0,var1);else super.onRestoreInstanceState(var0,var1);}
    @Override public void onResume() { if(c!=null&&c.registerCall(354)==0)c.call(354);else super.onResume();}
    @Override public void onSaveInstanceState(android.os.Bundle var0) { if(c!=null&&c.registerCall(355)==0)c.call(355,var0);else super.onSaveInstanceState(var0);}
    @Override public void onSaveInstanceState(android.os.Bundle var0,android.os.PersistableBundle var1) { if(c!=null&&c.registerCall(356)==0)c.call(356,var0,var1);else super.onSaveInstanceState(var0,var1);}
    @Override public boolean onSearchRequested() { if(c!=null&&c.registerCall(357)==0)return (boolean)c.get(357);else return super.onSearchRequested();}
    @Override public boolean onSearchRequested(android.view.SearchEvent var0) { if(c!=null&&c.registerCall(358)==0)return (boolean)c.get(358,var0);else return super.onSearchRequested(var0);}
    @Override public void onStateNotSaved() { if(c!=null&&c.registerCall(359)==0)c.call(359);else super.onStateNotSaved();}
    @Override public void onStop() { if(c!=null&&c.registerCall(360)==0)c.call(360);else super.onStop();}
    @Override public void onTitleChanged(java.lang.CharSequence var0,int var1) { if(c!=null&&c.registerCall(361)==0)c.call(361,var0,var1);else super.onTitleChanged(var0,var1);}
    @Override public void onActivityResult(int var0,int var1,android.content.Intent var2) { if(c!=null&&c.registerCall(362)==0)c.call(362,var0,var1,var2);else super.onActivityResult(var0,var1,var2);}
    @Override public void onResumeFragments() { if(c!=null&&c.registerCall(363)==0)c.call(363);else super.onResumeFragments();}
    @Override public void onRequestPermissionsResult(int var0, @NonNull String[] var1, @NonNull int[] var2) {if(c!=null&&c.registerCall(364)==0)c.call(364,var0,var1,var2);else super.onRequestPermissionsResult(var0, var1, var2);}
    @Override public void onPointerCaptureChanged(boolean var0) {if(c!=null&&c.registerCall(365)==0)c.call(365,var0);else super.onPointerCaptureChanged(var0); }
    @Override protected void onUserLeaveHint() { if(c!=null&&c.registerCall(366)==0)c.call(366);else super.onUserLeaveHint(); }
    @Override protected void onNightModeChanged(int var0) { if(c!=null&&c.registerCall(367)==0)c.call(367,var0);else super.onNightModeChanged(var0); }
    @Override public void onPrepareSupportNavigateUpTaskStack(@NonNull TaskStackBuilder var0) { if(c!=null&&c.registerCall(368)==0)c.call(368,var0);else super.onPrepareSupportNavigateUpTaskStack(var0); }
    @Override public void onSupportActionModeFinished(@NonNull ActionMode var0) { if(c!=null&&c.registerCall(369)==0)c.call(369,var0);else super.onSupportActionModeFinished(var0); }
    @Override public void onSupportActionModeStarted(@NonNull ActionMode var0) { if(c!=null&&c.registerCall(370)==0)c.call(370,var0);else super.onSupportActionModeStarted(var0); }
    @Override public Object onRetainCustomNonConfigurationInstance() { if(c!=null&&c.registerCall(371)==0)return c.get(371);else return super.onRetainCustomNonConfigurationInstance(); }
    @Override protected void onStart() {if(c!=null && c.registerCall(372)==0) c.call(372);else super.onStart();}

    @Override public void sendBroadcast(android.content.Intent var0){if(c!=null&&c.registerCall(373)==0)c.call(373,var0);else super.sendBroadcast(var0);}
    @Override public void sendBroadcast(android.content.Intent var0,java.lang.String var1){if(c!=null&&c.registerCall(374)==0)c.call(374,var0,var1);else super.sendBroadcast(var0,var1);}
    @Override public void sendBroadcastAsUser(android.content.Intent var0,android.os.UserHandle var1){if(c!=null&&c.registerCall(377)==0)c.call(377,var0,var1);else super.sendBroadcastAsUser(var0,var1);}
    @Override public void sendBroadcastAsUser(android.content.Intent var0,android.os.UserHandle var1,java.lang.String var2){if(c!=null&&c.registerCall(378)==0)c.call(378,var0,var1,var2);else super.sendBroadcastAsUser(var0,var1,var2);}
    @Override public void sendOrderedBroadcast(android.content.Intent var0,java.lang.String var1){if(c!=null&&c.registerCall(381)==0)c.call(381,var0,var1);else super.sendOrderedBroadcast(var0,var1);}
    @Override public void sendOrderedBroadcast(android.content.Intent var0,java.lang.String var1,android.content.BroadcastReceiver var2,android.os.Handler var3,int var4,java.lang.String var5,android.os.Bundle var6){if(c!=null&&c.registerCall(382)==0)c.call(382,var0,var1,var2,var3,var4,var5,var6);else super.sendOrderedBroadcast(var0,var1,var2,var3,var4,var5,var6);}
    @Override public void sendOrderedBroadcastAsUser(android.content.Intent var0,android.os.UserHandle var1,java.lang.String var2,android.content.BroadcastReceiver var3,android.os.Handler var4,int var5,java.lang.String var6,android.os.Bundle var7){if(c!=null&&c.registerCall(385)==0)c.call(385,var0,var1,var2,var3,var4,var5,var6,var7);else super.sendOrderedBroadcastAsUser(var0,var1,var2,var3,var4,var5,var6,var7);}
    @Override public void sendStickyBroadcast(android.content.Intent var0){if(c!=null&&c.registerCall(388)==0)c.call(388,var0);else super.sendStickyBroadcast(var0);}
    @Override public void sendStickyBroadcastAsUser(android.content.Intent var0,android.os.UserHandle var1){if(c!=null&&c.registerCall(389)==0)c.call(389,var0,var1);else super.sendStickyBroadcastAsUser(var0,var1);}
    @Override public void sendStickyOrderedBroadcast(android.content.Intent var0,android.content.BroadcastReceiver var1,android.os.Handler var2,int var3,java.lang.String var4,android.os.Bundle var5){if(c!=null&&c.registerCall(390)==0)c.call(390,var0,var1,var2,var3,var4,var5);else super.sendStickyOrderedBroadcast(var0,var1,var2,var3,var4,var5);}
    @Override public void sendStickyOrderedBroadcastAsUser(android.content.Intent var0,android.os.UserHandle var1,android.content.BroadcastReceiver var2,android.os.Handler var3,int var4,java.lang.String var5,android.os.Bundle var6){if(c!=null&&c.registerCall(391)==0)c.call(391,var0,var1,var2,var3,var4,var5,var6);else super.sendStickyOrderedBroadcastAsUser(var0,var1,var2,var3,var4,var5,var6);}
    @Override public void setActionBar(android.widget.Toolbar var0){if(c!=null&&c.registerCall(392)==0)c.call(392,var0);else super.setActionBar(var0);}
    @Override public void setContentTransitionManager(android.transition.TransitionManager var0){if(c!=null&&c.registerCall(395)==0)c.call(395,var0);else super.setContentTransitionManager(var0);}
    @Override public void setContentView(int var0){if(c!=null&&c.registerCall(396)==0)c.call(396,var0);else super.setContentView(var0);}
    @Override public void setContentView(android.view.View var0){if(c!=null&&c.registerCall(397)==0)c.call(397,var0);else super.setContentView(var0);}
    @Override public void setEnterSharedElementCallback(android.app.SharedElementCallback var0){if(c!=null&&c.registerCall(401)==0)c.call(401,var0);else super.setEnterSharedElementCallback(var0);}
    @Override public void setEnterSharedElementCallback(androidx.core.app.SharedElementCallback var0){if(c!=null&&c.registerCall(402)==0)c.call(402,var0);else super.setEnterSharedElementCallback(var0);}
    @Override public void setExitSharedElementCallback(android.app.SharedElementCallback var0){if(c!=null&&c.registerCall(403)==0)c.call(403,var0);else super.setExitSharedElementCallback(var0);}
    @Override public void setExitSharedElementCallback(androidx.core.app.SharedElementCallback var0){if(c!=null&&c.registerCall(404)==0)c.call(404,var0);else super.setExitSharedElementCallback(var0);}
    @Override public void setFinishOnTouchOutside(boolean var0){if(c!=null&&c.registerCall(409)==0)c.call(409,var0);else super.setFinishOnTouchOutside(var0);}
    @Override public void setImmersive(boolean var0){if(c!=null&&c.registerCall(410)==0)c.call(410,var0);else super.setImmersive(var0);}
    @Override public void setInheritShowWhenLocked(boolean var0){if(c!=null&&c.registerCall(411)==0)c.call(411,var0);else super.setInheritShowWhenLocked(var0);}
    @Override public void setIntent(android.content.Intent var0){if(c!=null&&c.registerCall(412)==0)c.call(412,var0);else super.setIntent(var0);}
    @Override public void setPictureInPictureParams(android.app.PictureInPictureParams var0){if(c!=null&&c.registerCall(416)==0)c.call(416,var0);else super.setPictureInPictureParams(var0);}
    @Override public void setRequestedOrientation(int var0){if(c!=null&&c.registerCall(421)==0)c.call(421,var0);else super.setRequestedOrientation(var0);}
    @Override public void setShowWhenLocked(boolean var0){if(c!=null&&c.registerCall(425)==0)c.call(425,var0);else super.setShowWhenLocked(var0);}
    @Override public void setSupportActionBar(androidx.appcompat.widget.Toolbar var0){if(c!=null&&c.registerCall(426)==0)c.call(426,var0);else super.setSupportActionBar(var0);}
    @Override public void setSupportProgress(int var0){if(c!=null&&c.registerCall(427)==0)c.call(427,var0);else super.setSupportProgress(var0);}
    @Override public void setSupportProgressBarIndeterminate(boolean var0){if(c!=null&&c.registerCall(428)==0)c.call(428,var0);else super.setSupportProgressBarIndeterminate(var0);}
    @Override public void setSupportProgressBarIndeterminateVisibility(boolean var0){if(c!=null&&c.registerCall(429)==0)c.call(429,var0);else super.setSupportProgressBarIndeterminateVisibility(var0);}
    @Override public void setSupportProgressBarVisibility(boolean var0){if(c!=null&&c.registerCall(430)==0)c.call(430,var0);else super.setSupportProgressBarVisibility(var0);}
    @Override public void setTheme(int var0){if(c!=null&&c.registerCall(432)==0)c.call(432,var0);else super.setTheme(var0);}
    @Override public void setTitle(int var0){if(c!=null&&c.registerCall(434)==0)c.call(434,var0);else super.setTitle(var0);}
    @Override public void setTitle(java.lang.CharSequence var0){if(c!=null&&c.registerCall(435)==0)c.call(435,var0);else super.setTitle(var0);}
    @Override public void setTitleColor(int var0){if(c!=null&&c.registerCall(436)==0)c.call(436,var0);else super.setTitleColor(var0);}
    @Override public void setTurnScreenOn(boolean var0){if(c!=null&&c.registerCall(437)==0)c.call(437,var0);else super.setTurnScreenOn(var0);}
    @Override public void setVisible(boolean var0){if(c!=null&&c.registerCall(438)==0)c.call(438,var0);else super.setVisible(var0);}
    @Override public void setWallpaper(android.graphics.Bitmap var0) throws IOException {if(c!=null&&c.registerCall(441)==0)c.call(441,var0);else super.setWallpaper(var0);}
    @Override public void setWallpaper(java.io.InputStream var0) throws IOException {if(c!=null&&c.registerCall(442)==0)c.call(442,var0);else super.setWallpaper(var0);}
    @Override public boolean shouldShowRequestPermissionRationale(java.lang.String var0){if(c!=null&&c.registerCall(443)==0)return (boolean)c.get(443,var0);else return (boolean)super.shouldShowRequestPermissionRationale(var0);}
    @Override public boolean shouldUpRecreateTask(android.content.Intent var0){if(c!=null&&c.registerCall(444)==0)return (boolean)c.get(444,var0);else return (boolean)super.shouldUpRecreateTask(var0);}
    @Override public boolean showAssist(android.os.Bundle var0){if(c!=null&&c.registerCall(445)==0)return (boolean)c.get(445,var0);else return (boolean)super.showAssist(var0);}
    @Override public void showLockTaskEscapeMessage(){if(c!=null&&c.registerCall(448)==0)c.call(448);else super.showLockTaskEscapeMessage();}
    @Override public void startActivities(android.content.Intent[] var0){if(c!=null&&c.registerCall(451)==0)c.call(451,var0);else super.startActivities(var0);}
    @Override public void startActivities(android.content.Intent[] var0,android.os.Bundle var1){if(c!=null&&c.registerCall(452)==0)c.call(452,var0,var1);else super.startActivities(var0,var1);}
    @Override public void startActivity(android.content.Intent var0){if(c!=null&&c.registerCall(453)==0)c.call(453,var0);else super.startActivity(var0);}
    @Override public void startActivity(android.content.Intent var0,android.os.Bundle var1){if(c!=null&&c.registerCall(454)==0)c.call(454,var0,var1);else super.startActivity(var0,var1);}
    @Override public void startActivityForResult(android.content.Intent var0,int var1){if(c!=null&&c.registerCall(457)==0)c.call(457,var0,var1);else super.startActivityForResult(var0,var1);}
    @Override public void startActivityForResult(android.content.Intent var0,int var1,android.os.Bundle var2){if(c!=null&&c.registerCall(458)==0)c.call(458,var0,var1,var2);else super.startActivityForResult(var0,var1,var2);}
    @Override public void startActivityFromChild(android.app.Activity var0,android.content.Intent var1,int var2){if(c!=null&&c.registerCall(461)==0)c.call(461,var0,var1,var2);else super.startActivityFromChild(var0,var1,var2);}
    @Override public void startActivityFromChild(android.app.Activity var0,android.content.Intent var1,int var2,android.os.Bundle var3){if(c!=null&&c.registerCall(462)==0)c.call(462,var0,var1,var2,var3);else super.startActivityFromChild(var0,var1,var2,var3);}
    @Override public void startActivityFromFragment(android.app.Fragment var0,android.content.Intent var1,int var2){if(c!=null&&c.registerCall(463)==0)c.call(463,var0,var1,var2);else super.startActivityFromFragment(var0,var1,var2);}
    @Override public void startActivityFromFragment(androidx.fragment.app.Fragment var0,android.content.Intent var1,int var2){if(c!=null&&c.registerCall(464)==0)c.call(464,var0,var1,var2);else super.startActivityFromFragment(var0,var1,var2);}
    @Override public void startActivityFromFragment(android.app.Fragment var0,android.content.Intent var1,int var2,android.os.Bundle var3){if(c!=null&&c.registerCall(465)==0)c.call(465,var0,var1,var2,var3);else super.startActivityFromFragment(var0,var1,var2,var3);}
    @Override public void startActivityFromFragment(androidx.fragment.app.Fragment var0,android.content.Intent var1,int var2,android.os.Bundle var3){if(c!=null&&c.registerCall(466)==0)c.call(466,var0,var1,var2,var3);else super.startActivityFromFragment(var0,var1,var2,var3);}
    @Override public boolean startActivityIfNeeded(android.content.Intent var0,int var1){if(c!=null&&c.registerCall(467)==0)return (boolean)c.get(467,var0,var1);else return (boolean)super.startActivityIfNeeded(var0,var1);}
    @Override public boolean startActivityIfNeeded(android.content.Intent var0,int var1,android.os.Bundle var2){if(c!=null&&c.registerCall(468)==0)return (boolean)c.get(468,var0,var1,var2);else return (boolean)super.startActivityIfNeeded(var0,var1,var2);}
    @Override public android.content.ComponentName startForegroundService(android.content.Intent var0){if(c!=null&&c.registerCall(469)==0)return (android.content.ComponentName)c.get(469,var0);else return (android.content.ComponentName)super.startForegroundService(var0);}
    @Override public boolean startInstrumentation(android.content.ComponentName var0,java.lang.String var1,android.os.Bundle var2){if(c!=null&&c.registerCall(471)==0)return (boolean)c.get(471,var0,var1,var2);else return (boolean)super.startInstrumentation(var0,var1,var2);}
    @Override public void startIntentSender(android.content.IntentSender var0,android.content.Intent var1,int var2,int var3,int var4) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(472)==0)c.call(472,var0,var1,var2,var3,var4);else super.startIntentSender(var0,var1,var2,var3,var4);}
    @Override public void startIntentSender(android.content.IntentSender var0,android.content.Intent var1,int var2,int var3,int var4,android.os.Bundle var5) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(473)==0)c.call(473,var0,var1,var2,var3,var4,var5);else super.startIntentSender(var0,var1,var2,var3,var4,var5);}
    @Override public void startIntentSenderForResult(android.content.IntentSender var0,int var1,android.content.Intent var2,int var3,int var4,int var5) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(474)==0)c.call(474,var0,var1,var2,var3,var4,var5);else super.startIntentSenderForResult(var0,var1,var2,var3,var4,var5);}
    @Override public void startIntentSenderForResult(android.content.IntentSender var0,int var1,android.content.Intent var2,int var3,int var4,int var5,android.os.Bundle var6) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(475)==0)c.call(475,var0,var1,var2,var3,var4,var5,var6);else super.startIntentSenderForResult(var0,var1,var2,var3,var4,var5,var6);}
    @Override public void startIntentSenderFromChild(android.app.Activity var0,android.content.IntentSender var1,int var2,android.content.Intent var3,int var4,int var5,int var6) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(476)==0)c.call(476,var0,var1,var2,var3,var4,var5,var6);else super.startIntentSenderFromChild(var0,var1,var2,var3,var4,var5,var6);}
    @Override public void startIntentSenderFromChild(android.app.Activity var0,android.content.IntentSender var1,int var2,android.content.Intent var3,int var4,int var5,int var6,android.os.Bundle var7) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(477)==0)c.call(477,var0,var1,var2,var3,var4,var5,var6,var7);else super.startIntentSenderFromChild(var0,var1,var2,var3,var4,var5,var6,var7);}
    @Override public void startIntentSenderFromFragment(androidx.fragment.app.Fragment var0,android.content.IntentSender var1,int var2,android.content.Intent var3,int var4,int var5,int var6,android.os.Bundle var7) throws IntentSender.SendIntentException {if(c!=null&&c.registerCall(478)==0)c.call(478,var0,var1,var2,var3,var4,var5,var6,var7);else super.startIntentSenderFromFragment(var0,var1,var2,var3,var4,var5,var6,var7);}
    @Override public void startLocalVoiceInteraction(android.os.Bundle var0){if(c!=null&&c.registerCall(479)==0)c.call(479,var0);else super.startLocalVoiceInteraction(var0);}
    @Override public void startLockTask(){if(c!=null&&c.registerCall(480)==0)c.call(480);else super.startLockTask();}
    @Override public void startManagingCursor(android.database.Cursor var0){if(c!=null&&c.registerCall(481)==0)c.call(481,var0);else super.startManagingCursor(var0);}
    @Override public boolean startNextMatchingActivity(android.content.Intent var0){if(c!=null&&c.registerCall(482)==0)return (boolean)c.get(482,var0);else return (boolean)super.startNextMatchingActivity(var0);}
    @Override public boolean startNextMatchingActivity(android.content.Intent var0,android.os.Bundle var1){if(c!=null&&c.registerCall(483)==0)return (boolean)c.get(483,var0,var1);else return (boolean)super.startNextMatchingActivity(var0,var1);}
    @Override public void startPostponedEnterTransition(){if(c!=null&&c.registerCall(484)==0)c.call(484);else super.startPostponedEnterTransition();}
    @Override public void startSearch(java.lang.String var0,boolean var1,android.os.Bundle var2,boolean var3){if(c!=null&&c.registerCall(485)==0)c.call(485,var0,var1,var2,var3);else super.startSearch(var0,var1,var2,var3);}
    @Override public android.content.ComponentName startService(android.content.Intent var0){if(c!=null&&c.registerCall(486)==0)return (android.content.ComponentName)c.get(486,var0);else return (android.content.ComponentName)super.startService(var0);}
    @Override public void stopLocalVoiceInteraction(){if(c!=null&&c.registerCall(489)==0)c.call(489);else super.stopLocalVoiceInteraction();}
    @Override public void stopLockTask(){if(c!=null&&c.registerCall(490)==0)c.call(490);else super.stopLockTask();}
    @Override public void stopManagingCursor(android.database.Cursor var0){if(c!=null&&c.registerCall(491)==0)c.call(491,var0);else super.stopManagingCursor(var0);}
    @Override public boolean stopService(android.content.Intent var0){if(c!=null&&c.registerCall(492)==0)return (boolean)c.get(492,var0);else return (boolean)super.stopService(var0);}
    @Override public void supportFinishAfterTransition(){if(c!=null&&c.registerCall(494)==0)c.call(494);else super.supportFinishAfterTransition();}
    @Override public void supportInvalidateOptionsMenu(){if(c!=null&&c.registerCall(495)==0)c.call(495);else super.supportInvalidateOptionsMenu();}
    @Override public void supportNavigateUpTo(android.content.Intent var0){if(c!=null&&c.registerCall(496)==0)c.call(496,var0);else super.supportNavigateUpTo(var0);}
    @Override public void supportPostponeEnterTransition(){if(c!=null&&c.registerCall(497)==0)c.call(497);else super.supportPostponeEnterTransition();}
    @Override public void setContentView(View view, ViewGroup.LayoutParams params) {if (c != null && c.registerCall(498) == 0) c.call(498, view, params);else super.setContentView(view, params);}
    @Override public void setTaskDescription(ActivityManager.TaskDescription taskDescription) { if (c != null && c.registerCall(499) == 0) c.call(499,taskDescription);else super.setTaskDescription(taskDescription); }
    @Override public void setTheme(@Nullable Resources.Theme theme) {if (c != null && c.registerCall(500) == 0) c.call(500,theme);else super.setTheme(theme); }
    @Override public void recreate(){if(c!=null&&c.registerCall(501)==0)c.call(501);else super.recreate();}
    @Override public void registerComponentCallbacks(android.content.ComponentCallbacks var0){if(c!=null&&c.registerCall(503)==0)c.call(503,var0);else super.registerComponentCallbacks(var0);}
    @Override public void registerForContextMenu(android.view.View var0){if(c!=null&&c.registerCall(504)==0)c.call(504,var0);else super.registerForContextMenu(var0);}
    @Override public android.content.Intent registerReceiver(android.content.BroadcastReceiver var0,android.content.IntentFilter var1){if(c!=null&&c.registerCall(505)==0)return (android.content.Intent)c.get(505,var0,var1);else return (android.content.Intent)super.registerReceiver(var0,var1);}
    @Override public android.content.Intent registerReceiver(android.content.BroadcastReceiver var0,android.content.IntentFilter var1,int var2){if(c!=null&&c.registerCall(506)==0)return (android.content.Intent)c.get(506,var0,var1,var2);else return (android.content.Intent)super.registerReceiver(var0,var1,var2);}
    @Override public android.content.Intent registerReceiver(android.content.BroadcastReceiver var0,android.content.IntentFilter var1,java.lang.String var2,android.os.Handler var3){if(c!=null&&c.registerCall(507)==0)return (android.content.Intent)c.get(507,var0,var1,var2,var3);else return (android.content.Intent)super.registerReceiver(var0,var1,var2,var3);}
    @Override public android.content.Intent registerReceiver(android.content.BroadcastReceiver var0,android.content.IntentFilter var1,java.lang.String var2,android.os.Handler var3,int var4){if(c!=null&&c.registerCall(508)==0)return (android.content.Intent)c.get(508,var0,var1,var2,var3,var4);else return (android.content.Intent)super.registerReceiver(var0,var1,var2,var3,var4);}
    @Override public boolean releaseInstance(){if(c!=null&&c.registerCall(511)==0)return (boolean)c.get(511);else return (boolean)super.releaseInstance();}
     public void removeDialog$(int var0){if(c!=null&&c.registerCall(512)==0)c.call(512,var0);else super.removeDialog(var0);}
    @Override public void removeStickyBroadcast(android.content.Intent var0){if(c!=null&&c.registerCall(513)==0)c.call(513,var0);else super.removeStickyBroadcast(var0);}
    @Override public void removeStickyBroadcastAsUser(android.content.Intent var0,android.os.UserHandle var1){if(c!=null&&c.registerCall(514)==0)c.call(514,var0,var1);else super.removeStickyBroadcastAsUser(var0,var1);}
    @Override public void reportFullyDrawn(){if(c!=null&&c.registerCall(515)==0)c.call(515);else super.reportFullyDrawn();}
    @Override public android.view.DragAndDropPermissions requestDragAndDropPermissions(android.view.DragEvent var0){if(c!=null&&c.registerCall(516)==0)return (android.view.DragAndDropPermissions)c.get(516,var0);else return (android.view.DragAndDropPermissions)super.requestDragAndDropPermissions(var0);}
     public void requestPermissions$(java.lang.String[] var0,int var1){if(c!=null&&c.registerCall(517)==0)c.call(517,var0,var1);else super.requestPermissions(var0,var1);}
     public void requestShowKeyboardShortcuts$(){if(c!=null&&c.registerCall(518)==0)c.call(518);else super.requestShowKeyboardShortcuts();}
    @Override public boolean requestVisibleBehind(boolean var0){if(c!=null&&c.registerCall(519)==0)return (boolean)c.get(519,var0);else return (boolean)super.requestVisibleBehind(var0);}
    public boolean requestWindowFeature$(int var0){if(c!=null&&c.registerCall(520)==0)return (boolean)c.get(520,var0);else return (boolean)super.requestWindowFeature(var0);}
     public android.view.View requireViewById$(int var0){if(c!=null&&c.registerCall(521)==0)return (android.view.View)c.get(521,var0);else return (android.view.View)super.requireViewById(var0);}
    @Override public void revokeUriPermission(android.net.Uri var0,int var1){if(c!=null&&c.registerCall(522)==0)c.call(522,var0,var1);else super.revokeUriPermission(var0,var1);}
    @Override public void revokeUriPermission(java.lang.String var0,android.net.Uri var1,int var2){if(c!=null&&c.registerCall(523)==0)c.call(523,var0,var1,var2);else super.revokeUriPermission(var0,var1,var2);}
     public void runOnUiThread$(java.lang.Runnable var0){if(c!=null&&c.registerCall(524)==0)c.call(524,var0);else super.runOnUiThread(var0);}
    @Override public void setVrModeEnabled(boolean var0, @NonNull ComponentName var1) throws PackageManager.NameNotFoundException { if(c!=null&&c.registerCall(525)==0)c.call(525,var0,var1);else super.setVrModeEnabled(var0,var1); }
}

