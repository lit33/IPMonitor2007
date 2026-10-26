package com.ipmonitor.office;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;
class Db extends SQLiteOpenHelper {
 Db(Context c){super(c,"monitor.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE hosts (id INTEGER PRIMARY KEY AUTOINCREMENT,ip TEXT UNIQUE NOT NULL,title TEXT NOT NULL)");d.execSQL("CREATE TABLE checks (id INTEGER PRIMARY KEY AUTOINCREMENT,host_id INTEGER,ts INTEGER,online INTEGER,latency INTEGER)");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 static class Host {long id;String ip,title;Host(long i,String p,String t){id=i;ip=p;title=t;}}
 synchronized List<Host> list(){List<Host> l=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,ip,title FROM hosts ORDER BY title,ip",null)){while(c.moveToNext())l.add(new Host(c.getLong(0),c.getString(1),c.getString(2)));}return l;}
 synchronized void save(Host h,String ip,String title){ContentValues v=new ContentValues();v.put("ip",ip);v.put("title",title);if(h==null)getWritableDatabase().insertOrThrow("hosts",null,v);else getWritableDatabase().update("hosts",v,"id=?",new String[]{""+h.id});}
 synchronized void remove(long id){getWritableDatabase().delete("hosts","id=?",new String[]{""+id});getWritableDatabase().delete("checks","host_id=?",new String[]{""+id});}
 synchronized void record(long id,Pinger.Result r){ContentValues v=new ContentValues();v.put("host_id",id);v.put("ts",System.currentTimeMillis());v.put("online",r.online?1:0);if(r.latency!=null)v.put("latency",r.latency);getWritableDatabase().insert("checks",null,v);}
 synchronized String history(long id){StringBuilder s=new StringBuilder();try(Cursor c=getReadableDatabase().rawQuery("SELECT ts,online,latency FROM checks WHERE host_id=? ORDER BY ts DESC LIMIT 50",new String[]{""+id})){java.text.DateFormat f=new java.text.SimpleDateFormat("dd.MM HH:mm:ss",Locale.getDefault());while(c.moveToNext()){s.append(f.format(new Date(c.getLong(0)))).append("  ").append(c.getInt(1)==1?"Доступен":"Недоступен");if(!c.isNull(2))s.append("  ").append(c.getLong(2)).append(" мс");s.append("\n");}}return s.length()==0?"История пуста":s.toString();}
}
