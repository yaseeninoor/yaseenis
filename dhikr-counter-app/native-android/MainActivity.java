package com.yaseenis.dhikr;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
  static final int BG=0xff081b14, PANEL=0xff10271e, PANEL2=0xff152f24, GOLD=0xffc8b86d, WHITE=0xffedf2e9, MUTED=0xff9aaca0, GREEN=0xff203c2d;
  static final int PUBLIC_TARGET=700000, DAILY_TARGET=2000;
  SharedPreferences sp; int today=0, selected=0; long lifetime=0, elapsedBefore=0, startedAt=0; boolean running=false;
  TextView countText,totalTodayText,targetText,timerText,dhikrArabic,dhikrEnglish,progressText,statusText,meaningText;
  View ring; LinearLayout root; Handler handler=new Handler(Looper.getMainLooper());
  String[] names={"SubhanAllah","Alhamdulillah","Allahu Akbar","Astaghfirullah","La ilaha illallah","Salawath Mubarak"};
  String[] arabic={"سُبْحَانَ اللَّهِ","الْحَمْدُ لِلَّهِ","اللَّهُ أَكْبَرُ","أَسْتَغْفِرُ اللَّهَ","لَا إِلَٰهَ إِلَّا اللَّهُ","اللَّهُمَّ صَلِّ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ ﷺ"};
  String[] meanings={"Glory be to Allah","All praise is for Allah","Allah is the Greatest","I seek forgiveness from Allah","There is no god but Allah","O Allah, send blessings upon our Prophet Muhammad ﷺ"};
  Runnable clockTick=new Runnable(){public void run(){if(timerText!=null){timerText.setText(formatTime(sessionMillis()));}handler.postDelayed(this,1000);}};
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    sp=getSharedPreferences("dhikr_counter_v1",MODE_PRIVATE);String day=dayKey();if(!day.equals(sp.getString("day",""))){sp.edit().putString("day",day).putInt("today",0).apply();}
    today=sp.getInt("today",0);lifetime=sp.getLong("lifetime",0);selected=sp.getInt("selected",5);elapsedBefore=sp.getLong("elapsed",0);startedAt=sp.getLong("startedAt",0);running=sp.getBoolean("running",false);
    if(running&&startedAt==0)startedAt=System.currentTimeMillis();buildUi();handler.post(clockTick);}
  String dayKey(){return new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());}
  GradientDrawable shape(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);if(stroke!=0)d.setStroke(2,stroke);return d;}
  TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  LinearLayout.LayoutParams size(int w,int h){return new LinearLayout.LayoutParams(w,h);}
  TextView button(String s){TextView t=text(s,15,WHITE,true);t.setPadding(12,12,12,12);t.setBackground(shape(PANEL2,0xff31473a,18));t.setClickable(true);t.setFocusable(true);return t;}
  LinearLayout panel(){LinearLayout l=column();l.setPadding(16,15,16,15);l.setBackground(shape(PANEL,0xff20392d,22));return l;}
  void buildUi(){
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);root=column();root.setPadding(18,12,18,14);scroll.addView(root);setContentView(scroll);
    LinearLayout top=row();TextView logo=text("☾",27,GOLD,true);logo.setBackground(shape(PANEL2,0xff324b39,17));top.addView(logo,size(48,48));LinearLayout brand=column();brand.setPadding(12,0,0,0);TextView b=text("YASEENIS",12,GOLD,true);b.setGravity(Gravity.LEFT);TextView st=text("DHIKR COUNTER",18,WHITE,true);st.setGravity(Gravity.LEFT);brand.addView(b);brand.addView(st);top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));TextView reset=button("↻");top.addView(reset,size(48,48));reset.setOnClickListener(v->resetToday());root.addView(top);
    TextView intro=text("Track your daily tasbeeh",14,MUTED,false);intro.setGravity(Gravity.LEFT);LinearLayout.LayoutParams ip=size(-1,35);ip.topMargin=4;root.addView(intro,ip);
    LinearLayout select=panel();LinearLayout.LayoutParams spx=size(-1,-2);spx.topMargin=14;root.addView(select,spx);
    TextView cap=text("SELECT DHIKR",9,MUTED,true);cap.setGravity(Gravity.LEFT);select.addView(cap);
    LinearLayout choose=row();LinearLayout labels=column();labels.setGravity(Gravity.CENTER_VERTICAL);dhikrArabic=text(arabic[selected],24,GOLD,true);dhikrArabic.setGravity(Gravity.RIGHT);dhikrEnglish=text(names[selected],14,WHITE,true);dhikrEnglish.setGravity(Gravity.LEFT);labels.addView(dhikrArabic);labels.addView(dhikrEnglish);choose.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView drop=button("⌄");choose.addView(drop,size(48,48));select.addView(choose);select.setOnClickListener(v->pickDhikr());drop.setOnClickListener(v->pickDhikr());
    meaningText=text(meanings[selected],13,MUTED,false);LinearLayout.LayoutParams mp=size(-1,38);mp.topMargin=6;root.addView(meaningText,mp);
    // timer strip
    LinearLayout timer=panel();LinearLayout.LayoutParams tp=size(-1,-2);tp.topMargin=8;root.addView(timer,tp);LinearLayout tr=row();TextView timerIcon=text("◷",21,GOLD,true);tr.addView(timerIcon,size(42,44));LinearLayout tc=column();TextView timerTitle=text("REMEMBRANCE TIMER",10,MUTED,true);timerTitle.setGravity(Gravity.LEFT);timerText=text(formatTime(sessionMillis()),23,WHITE,true);timerText.setGravity(Gravity.LEFT);tc.addView(timerTitle);tc.addView(timerText);tr.addView(tc,new LinearLayout.LayoutParams(0,-2,1));TextView timerBtn=button(running?"Ⅱ Pause":"▶ Start");tr.addView(timerBtn);timer.addView(tr);timerBtn.setOnClickListener(v->{running=!running;if(running)startedAt=System.currentTimeMillis();else{elapsedBefore=sessionMillis();startedAt=0;}sp.edit().putBoolean("running",running).putLong("elapsed",elapsedBefore).putLong("startedAt",startedAt).apply();timerBtn.setText(running?"Ⅱ Pause":"▶ Start");timerText.setText(formatTime(sessionMillis()));});
    // counter circle
    LinearLayout center=column();center.setGravity(Gravity.CENTER);LinearLayout.LayoutParams cp=size(-1,-2);cp.topMargin=18;root.addView(center,cp);
    FrameLayout circle=new FrameLayout(this);circle.setBackground(shape(0xff12291f,0xff314b3a,150));circle.setElevation(4);LinearLayout.LayoutParams circleP=size(190,190);center.addView(circle,circleP);
    View inner=new View(this);inner.setBackground(shape(0xff12291f,GOLD,120));FrameLayout.LayoutParams inn=new FrameLayout.LayoutParams(-1,-1);inn.setMargins(13,13,13,13);circle.addView(inner,inn);
    LinearLayout numberBox=column();numberBox.setGravity(Gravity.CENTER);FrameLayout.LayoutParams nb=new FrameLayout.LayoutParams(-1,-1);circle.addView(numberBox,nb);countText=text(String.valueOf(today),49,WHITE,true);countText.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));numberBox.addView(countText);TextView slash=text("/ "+DAILY_TARGET,14,MUTED,false);numberBox.addView(slash);
    circle.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){v.setScaleX(.97f);v.setScaleY(.97f);return true;}if(e.getAction()==MotionEvent.ACTION_UP){v.setScaleX(1f);v.setScaleY(1f);add(1);v.performClick();return true;}if(e.getAction()==MotionEvent.ACTION_CANCEL){v.setScaleX(1f);v.setScaleY(1f);return true;}return true;});circle.setClickable(true);
    TextView tapHint=text("Tap to count",11,MUTED,false);LinearLayout.LayoutParams th=size(-1,30);th.topMargin=5;center.addView(tapHint,th);
    LinearLayout stats=row();LinearLayout.LayoutParams statsp=size(-1,76);statsp.topMargin=10;root.addView(stats,statsp);LinearLayout stoday=statCard("TOTAL TODAY",String.valueOf(today));LinearLayout target=statCard("DAILY TARGET",String.valueOf(DAILY_TARGET));LinearLayout.LayoutParams each=new LinearLayout.LayoutParams(0,-1,1);each.setMargins(0,0,5,0);stats.addView(stoday,each);LinearLayout.LayoutParams each2=new LinearLayout.LayoutParams(0,-1,1);each2.setMargins(5,0,0,0);stats.addView(target,each2);totalTodayText=(TextView)stoday.getChildAt(1);targetText=(TextView)target.getChildAt(1);
    LinearLayout overall=panel();LinearLayout.LayoutParams op=size(-1,-2);op.topMargin=14;root.addView(overall,op);TextView ot=text("PUBLIC SALAWATH TARGET",10,MUTED,true);ot.setGravity(Gravity.LEFT);overall.addView(ot);TextView pub=text(String.format(Locale.US,"%,d",lifetime),31,0xff91c9f4,true);pub.setGravity(Gravity.LEFT);overall.addView(pub);TextView goal=text("TARGET 700,000",12,WHITE,true);goal.setGravity(Gravity.LEFT);LinearLayout.LayoutParams gp=size(-1,28);gp.topMargin=4;overall.addView(goal,gp);ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(PUBLIC_TARGET);pb.setProgress((int)Math.min(PUBLIC_TARGET,lifetime));pb.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));pb.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff273a31));overall.addView(pb,size(-1,8));progressText=text(String.format(Locale.US,"%.2f%% complete · %,d remaining",Math.min(100,lifetime*100.0/PUBLIC_TARGET),Math.max(0,PUBLIC_TARGET-lifetime)),12,0xff91c9f4,false);overall.addView(progressText);
    LinearLayout quick=panel();LinearLayout.LayoutParams qp=size(-1,-2);qp.topMargin=14;root.addView(quick,qp);quick.addView(text("QUICK COUNT",11,GOLD,true));LinearLayout qr=row();String[] inc={"+10","+33","+100"};int[] vals={10,33,100};for(int i=0;i<inc.length;i++){final int n=vals[i];TextView bt=button(inc[i]);LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(0,52,1);qlp.setMargins(4,12,4,0);qr.addView(bt,qlp);bt.setOnClickListener(v->add(n));}quick.addView(qr);
    LinearLayout tools=row();LinearLayout.LayoutParams toolp=size(-1,54);toolp.topMargin=10;root.addView(tools,toolp);TextView topList=button("🏆 Top list");TextView custom=button("＋ Custom");TextView history=button("◷ Sessions");tools.addView(topList,new LinearLayout.LayoutParams(0,-1,1));tools.addView(custom,new LinearLayout.LayoutParams(0,-1,1));tools.addView(history,new LinearLayout.LayoutParams(0,-1,1));topList.setOnClickListener(v->showTopList());custom.setOnClickListener(v->customCount());history.setOnClickListener(v->showSessions());
    statusText=text("",12,MUTED,false);LinearLayout.LayoutParams statusP=size(-1,-2);statusP.topMargin=8;root.addView(statusText,statusP);
    TextView foot=text("Yaseenis · Dhikr & Salawath Mubarak",11,MUTED,false);LinearLayout.LayoutParams fp=size(-1,40);fp.topMargin=12;root.addView(foot,fp);
  }
  LinearLayout statCard(String heading,String val){LinearLayout c=column();c.setGravity(Gravity.CENTER);c.setPadding(8,8,8,8);c.setBackground(shape(PANEL,0xff263d30,17));TextView h=text(heading,9,MUTED,true);c.addView(h);TextView n=text(val,22,GOLD,true);c.addView(n);return c;}
  void pickDhikr(){new AlertDialog.Builder(this).setTitle("Select Dhikr").setItems(names,(d,which)->{selected=which;sp.edit().putInt("selected",selected).apply();dhikrArabic.setText(arabic[selected]);dhikrEnglish.setText(names[selected]);meaningText.setText(meanings[selected]);Toast.makeText(this,meanings[selected],Toast.LENGTH_SHORT).show();}).show();}
  void add(int amount){today+=amount;lifetime+=amount;sp.edit().putInt("today",today).putLong("lifetime",lifetime).putString("day",dayKey()).apply();countText.setText(String.valueOf(today));totalTodayText.setText(String.valueOf(today));progressText.setText(String.format(Locale.US,"%.2f%% complete · %,d remaining",Math.min(100,lifetime*100.0/PUBLIC_TARGET),Math.max(0,PUBLIC_TARGET-lifetime)));if(android.os.Build.VERSION.SDK_INT>=26){Vibrator vib=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(vib!=null&&vib.hasVibrator())vib.vibrate(VibrationEffect.createOneShot(12,45));}if(today>=DAILY_TARGET)statusText.setText("Masha Allah! Daily target reached.");else statusText.setText("+"+amount+" added · "+today+"/"+DAILY_TARGET+" today");}
  long sessionMillis(){return elapsedBefore+(running&&startedAt>0?System.currentTimeMillis()-startedAt:0);}
  String formatTime(long ms){long sec=Math.max(0,ms/1000);return String.format(Locale.US,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60);}
  void customCount(){EditText input=new EditText(this);input.setInputType(2);input.setHint("Enter 1–10,000");new AlertDialog.Builder(this).setTitle("Custom Count").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{try{int n=Integer.parseInt(input.getText().toString().trim());if(n<1||n>10000)throw new Exception();add(n);}catch(Exception e){Toast.makeText(this,"Enter a number from 1 to 10,000",Toast.LENGTH_LONG).show();}}).show();}
  void showTopList(){String[] items=new String[names.length];for(int i=0;i<names.length;i++)items[i]=(i+1)+".  "+names[i]+(i==selected?"  ✓":"");new AlertDialog.Builder(this).setTitle("Dhikr Top List").setItems(items,(d,i)->{selected=i;sp.edit().putInt("selected",selected).apply();dhikrArabic.setText(arabic[i]);dhikrEnglish.setText(names[i]);meaningText.setText(meanings[i]);}).setPositiveButton("Close",null).show();}
  void showSessions(){new AlertDialog.Builder(this).setTitle("Remember Timer · Session").setMessage("Current session: "+formatTime(sessionMillis())+"\nDaily count: "+today+"\nDaily target: "+DAILY_TARGET+"\nLifetime count: "+lifetime).setPositiveButton("OK",null).show();}
  void resetToday(){new AlertDialog.Builder(this).setTitle("Reset today's count?").setMessage("This resets today's count only. Lifetime progress remains saved.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{today=0;sp.edit().putInt("today",0).apply();countText.setText("0");totalTodayText.setText("0");statusText.setText("Today's count reset.");}).show();}
  @Override protected void onPause(){super.onPause();elapsedBefore=sessionMillis();startedAt=0;sp.edit().putLong("elapsed",elapsedBefore).putLong("startedAt",0).putBoolean("running",running).apply();}
  @Override protected void onResume(){super.onResume();if(running&&startedAt==0)startedAt=System.currentTimeMillis();}
  @Override protected void onDestroy(){handler.removeCallbacks(clockTick);super.onDestroy();}
}