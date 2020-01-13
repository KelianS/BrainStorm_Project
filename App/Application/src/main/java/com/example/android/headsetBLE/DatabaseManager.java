package com.example.android.headsetBLE;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.renderscript.Sampler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DataBase_Evenements";
    private static final int  DATABASE_VERSION = 1;

    public DatabaseManager(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String strSql = "create table T_SeekBar ("
                + "   idScore integer primary key autoincrement,"
                + "   value_ integer not null"
                + ")";
        db.execSQL( strSql);
        Log.i( "DATABASE", "onCreate invoked");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String strSql = "drop table T_SeekBar"; //Uselles if we don't use new version
        db.execSQL( strSql );
        this.onCreate( db );
        Log.i( "DATABASE", "onUpgrade invoked" );
    }

    public void insertScore( String values){
        values = values.replace( "'", "''");
        String strSql = "insert into T_SeekBar (value_) values ('"
                + values + "'" + ")";
        this.getWritableDatabase().execSQL( strSql);
        Log.i( "DATABASE", "insertScore invoked" );
    }
    public List<RobotData> readTop100() {
        List<RobotData> action = new ArrayList<>();
        // 1ère technique : SQL
        String strSql = "select * from T_SeekBar";
        Cursor cursor = this.getReadableDatabase().rawQuery( strSql, null );
        cursor.moveToFirst();
        while( ! cursor.isAfterLast() ) {
            RobotData actionRobot = new RobotData( cursor.getInt( 0 ), cursor.getString( 1 ));
            action.add( actionRobot );
            cursor.moveToNext();
        }
        // 2nd technique "plus objet"
//        Cursor cursor = this.getReadableDatabase().query( "T_Scores",
//                new String[] { "idScore", "name", "score", "when_" },
//                null, null, null, null, "score desc", "10" );
//        cursor.moveToFirst();
//        while( ! cursor.isAfterLast() ) {
//            ScoreData score = new ScoreData( cursor.getInt( 0 ), cursor.getString( 1 ),
//                    cursor.getInt( 2 ), new Date( cursor.getLong( 3 ) ) );
//            scores.add( score );
//            cursor.moveToNext();
//        }
        cursor.close();

        return action;
    }
}
