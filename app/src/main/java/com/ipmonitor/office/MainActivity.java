package com.ipmonitor.office;
import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
public class MainActivity extends Activity {
 Db db;LinearLayout items;TextView status;Handler ui=new Handler(Looper.getMainLooper());ExecutorService worker=Executors.newSingleThreadExecutor();Map<Long,Pinger.Result> results=new HashMap<>();boolean auto=false,busy=false;int interval=30;
 int navy=Color.rgb(27,64,112),blue=Color.rgb(54,117,186);
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 GradientDrawable back(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(9));return d;}
 final Runnable tick=new Runnable(){public void run(){if(auto){all();ui.postDelayed(this,interval*1000L);}}};
 public void onCreate(Bundle b){super.onCreate(b);db=new Db(this);getWindow().setStatusBarColor(navy);draw();}
 void button(LinearLayout row,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(12);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->action.run());}
 void draw(){LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xfff0f5fc);setContentView(root);
 TextView head=text("  IP Monitor 2007",24,Color.WHITE);head.setGravity(Gravity.CENTER_VERTICAL);head.setBackgroundColor(navy);root.addView(head,new LinearLayout.LayoutParams(-1,dp(72)));
 LinearLayout ribbon=new LinearLayout(this);ribbon.setPadding(dp(5),dp(5),dp(5),dp(5));ribbon.setBackgroundColor(0xffdae8fa);root.addView(ribbon);
 button(ribbon,"+ Добавить",()->edit(null));button(ribbon,"Пинг всех",this::all);button(ribbon,"Авто",()->{auto=!auto;ui.removeCallbacks(tick);if(auto){all();ui.postDelayed(tick,interval*1000L);}refresh();});
 LinearLayout extra=new LinearLayout(this);root.addView(extra);button(extra,"Обновить",this::refresh);button(extra,"Интервал",()->new AlertDialog.Builder(this).setTitle("Интервал проверки").setItems(new String[]{"10 сек","30 сек","60 сек","5 мин"},(d,w)->{interval=new int[]{10,30,60,300}[w];ui.removeCallbacks(tick);if(auto)ui.postDelayed(tick,interval*1000L);}).show());
 ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));items=new LinearLayout(this);items.setOrientation(1);items.setPadding(dp(10),dp(10),dp(10),dp(10));scroll.addView(items);
 status=text("",13,Color.WHITE);status.setPadding(dp(12),dp(12),dp(12),dp(12));status.setBackgroundColor(blue);root.addView(status);refresh();}
 void refresh(){items.removeAllViews();List<Db.Host> hosts=db.list();for(Db.Host h:hosts){LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(12),dp(9),dp(12),dp(9));card.setBackground(back(Color.WHITE));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);items.addView(card,lp);
 card.addView(text(h.title.isEmpty()?h.ip:h.title,19,navy));card.addView(text(h.ip,13,Color.DKGRAY));Pinger.Result r=results.get(h.id);card.addView(text(r==null?"● Не проверялся":r.online?"● Доступен ("+r.latency+" мс)":"● Недоступен",14,r==null?Color.GRAY:r.online?0xff198449:0xffbe3333));
 LinearLayout row=new LinearLayout(this);card.addView(row);button(row,"Пинг",()->one(h));button(row,"История",()->new AlertDialog.Builder(this).setTitle(h.ip).setMessage(db.history(h.id)).setPositiveButton("OK",null).show());button(row,"Правка",()->edit(h));button(row,"Удалить",()->new AlertDialog.Builder(this).setMessage("Удалить "+h.ip+"?").setNegativeButton("Нет",null).setPositiveButton("Да",(d,w)->{db.remove(h.id);results.remove(h.id);refresh();}).show());}
 if(hosts.isEmpty())items.addView(text("Нажмите «Добавить», чтобы сохранить IP-адрес.",16,navy));status.setText("Устройств: "+hosts.size()+"  |  Авто: "+(auto?"включено":"выключено"));}
 void edit(Db.Host host){LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(22),0,dp(22),0);EditText ip=new EditText(this);ip.setSingleLine();ip.setHint("IP: 192.168.1.1");ip.setText(host==null?"":host.ip);body.addView(ip);EditText name=new EditText(this);name.setSingleLine();name.setHint("Название");name.setText(host==null?"":host.title);body.addView(name);
 AlertDialog dlg=new AlertDialog.Builder(this).setTitle("IP-адрес").setView(body).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create();dlg.setOnShowListener(v->dlg.getButton(-1).setOnClickListener(w->{String address=ip.getText().toString().trim();if(!Pinger.valid(address)){ip.setError("Некорректный IPv4");return;}try{db.save(host,address,name.getText().toString().trim());results.clear();refresh();dlg.dismiss();}catch(Exception e){ip.setError("Такой IP уже есть");}}));dlg.show();}
 void one(Db.Host h){if(busy)return;busy=true;status.setText("Проверка "+h.ip);worker.execute(()->{Pinger.Result r=Pinger.check(h.ip);db.record(h.id,r);ui.post(()->{results.put(h.id,r);busy=false;refresh();});});}
 void all(){if(busy)return;List<Db.Host> list=db.list();if(list.isEmpty())return;busy=true;status.setText("Проверка...");worker.execute(()->{for(Db.Host h:list){Pinger.Result r=Pinger.check(h.ip);db.record(h.id,r);ui.post(()->{results.put(h.id,r);refresh();});}ui.post(()->{busy=false;refresh();});});}
 protected void onDestroy(){auto=false;ui.removeCallbacks(tick);worker.shutdownNow();db.close();super.onDestroy();}
}
