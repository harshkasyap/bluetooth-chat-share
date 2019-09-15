package com.example.android.bluetoothchat;

/**
 * Created by root on 9/6/15.
 */
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataBase {

    public static final String  devId="did";
    public static final String  devAdd="dadd";
    public static final String  devName="dname";
    public static final String  devHash="dhash";
    public static final String  devDesc="ddesc";
    public static final String  devPriv="dpriv";
    public static final String  devPub="dpub";
    public static final String  sId="sid";

    public static final String  fileName="fname";
    public static final String  fileDevId="fdid";
    public static final String  fileHash="fhash";
    public static final String  fId="fid";

    public static final String DATABASE_NAME="DataDb2";
    public static final String DEV_DETAILS="dev_details";
    public static final String FILE_DETAILS="file_details";

    public static final int DATABASE_VERSION=1;

    private DbHelper ourHelper;
    private final Context ourContext;
    private SQLiteDatabase ourDatabase;


    private class DbHelper extends SQLiteOpenHelper{

        public DbHelper(Context context){
            super(context,DATABASE_NAME,null,DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db){
            db.execSQL("CREATE TABLE "+DEV_DETAILS+" ("+
                    devId+ " TEXT NOT NULL, "+
                    devName+ " TEXT NOT NULL, "+
                    devAdd+ " TEXT NOT NULL, "+
                    devHash+ " TEXT NOT NULL, "+
                    devDesc+ " TEXT NOT NULL, "+
                    devPriv+ " TEXT NOT NULL, "+
                    devPub+ " TEXT NOT NULL, "+
                    sId+ " INTEGER PRIMARY KEY AUTOINCREMENT, UNIQUE ("+ devAdd + ","+ devDesc + "));"
            );

            db.execSQL("CREATE TABLE "+FILE_DETAILS+" ("+
                    fileName+ " TEXT NOT NULL, "+
                    fileHash+ " TEXT NOT NULL, "+
                    fileDevId+ " TEXT NOT NULL, "+
                    fId+ " INTEGER PRIMARY KEY AUTOINCREMENT, UNIQUE (" + fileName + ","+ fileHash + "));"
            );
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

        }
    }

    public DataBase(Context c){
        ourContext=c;
    }

    public DataBase open()throws SQLException{
        ourHelper=new DbHelper(ourContext);
        ourDatabase=ourHelper.getWritableDatabase();
        return this;
    }

    public void close(){
        ourHelper.close();
    }

    public long addNode(String deviceId, String deviceName, String deviceAdd, String deviceHash, String deviceDesc, String priv, String pub) {
        try {
            ContentValues cv=new ContentValues();
            cv.put(devId, deviceId);
            cv.put(devName, deviceName);
            cv.put(devAdd, deviceAdd);
            cv.put(devHash, deviceHash);
            cv.put(devDesc, deviceDesc);
            cv.put(devPriv, priv);
            cv.put(devPub, pub);
            return ourDatabase.insert(DEV_DETAILS, null, cv);
        } catch(Exception e) {
            return 0;
        }
    }

    public String updateOverlayId(String deviceId) {
        try {
            String devDescStr = "mine";
            ContentValues cv = new ContentValues();
            cv.put(devId, deviceId);
            ourDatabase.update(DEV_DETAILS, cv, devDesc + "="+ "'"+"mine"+"'" , null);
            return "";
        } catch(Exception e) {
            return e.toString();
        }
    }

    public String updateAddNOverlayId(String add, String hash) {
        try {
            ContentValues cv = new ContentValues();
            cv.put(devAdd, add);
            cv.put(devHash, hash);
            ourDatabase.update(DEV_DETAILS, cv, devDesc + "="+ "'"+"mine"+"'" , null);
            return "";
        } catch(Exception e) {
            return e.toString();
        }
    }

    public long addFileInfo(String _fileName, String _fileHash, String _fileDevId) {
        try {
            ContentValues cv=new ContentValues();
            cv.put(fileName, _fileName);
            cv.put(fileHash, _fileHash);
            cv.put(fileDevId, _fileDevId);
            return ourDatabase.insert(FILE_DETAILS, null, cv);
        } catch(Exception e) {
            return 0;
        }
    }

    /*public boolean delStudNote(String id){

        return ourDatabase.delete(STUD_NOTES, id_NOTE + "=" + id, null) > 0;

    }*/

    public String getHead(String desc) {
        String head = "HeadNotFound";
        Cursor c = null;
        try{
            c = ourDatabase.rawQuery("select * from dev_details where ddesc='"+desc+"'  ", null);

            c.moveToLast();

            head =  "headFound  ;  "+ c.getString(1)+ "  ;  "+ c.getString(2) + "  ;  "+ c.getString(3) + " ; " + c.getString(5) + " ; " + c.getString(6);
        }
        catch(Exception e){
            //
        }
        finally {
            c.close();
        }
        return  head;
    }

    public String showAllNodes() {
        String nodes = "";
        Cursor c = null;
        try{
            c = ourDatabase.rawQuery("select * from dev_details ", null);

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                nodes +=  c.getString(0)+ "  ;  " + c.getString(1) + "  ;  " +  c.getString(2) + "  ;  " +  c.getString(4)+"\n";
            }
        }
        catch(Exception e){
            //
        }
        finally {
            c.close();
        }
        return  nodes;
    }

    /*public void delAll(String spid){
        Cursor cc=null;
        try{
            cc = ourDatabase.rawQuery("delete from scheduler where spid='"+spid+"'", null);
            cc.moveToFirst();
        }
        catch(Exception e){}
        finally {
            cc.close();
        }
    }*/

}



