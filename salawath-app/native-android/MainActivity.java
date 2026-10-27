package com.yaseenis.salawath;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
  static final int BG=0xff032d22, GOLD=0xffffbd55, CREAM=0xfffff5df, MUTED=0xffb7d0c2, PANEL=0xff073a2d;
  SharedPreferences prefs; int count=0,goal=1000; long total=0; boolean soundOn=false,vibrateOn=true;
  String dhikr="اللَّهُمَّ صَلِّ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ ﷺ";
  TextView number,percent,title,goalLabel,soundLabel,vibeLabel; CounterView counter;
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    prefs=getSharedPreferences("salawath_native",MODE_PRIVATE);String day=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());
    if(!day.equals(prefs.getString("day",""))){prefs.edit().putString("day",day).putInt("count",0).apply();}
    count=prefs.getInt("count",0);total=prefs.getLong("total",0);goal=prefs.getInt("goal",1000);soundOn=prefs.getBoolean("sound",false);vibrateOn=prefs.getBoolean("vibrate",true);makeUi();}
  GradientDrawable bg(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);if(stroke!=0)d.setStroke(2,stroke);return d;}
  TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);if(bold)t.setTypeface(null,1);return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER);return l;}
  LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
  TextView btn(String s){TextView t=label(s,22,BG,true);t.setBackground(bg(GOLD,0,80));t.setClickable(true);t.setFocusable(true);return t;}
  void makeUi(){
    LinearLayout page=col();page.setBackgroundColor(BG);page.setPadding(20,4,20,12);page.setGravity(Gravity.CENTER_HORIZONTAL);setContentView(page);
    LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);TextView back=label("‹",32,GOLD,false);top.addView(back,lp(42,48));back.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("Salawath Mubarak").setPositiveButton("சரி",null).show());
    LinearLayout branding=col();TextView brand=label("YASEENIS",12,GOLD,true);brand.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);TextView sub=label("SALAWATH MUBARAK",9,MUTED,true);sub.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);branding.addView(brand);branding.addView(sub);top.addView(branding,new LinearLayout.LayoutParams(0,48,1));TextView menu=label("⋮",25,GOLD,true);top.addView(menu,lp(34,48));menu.setOnClickListener(v->setGoal());page.addView(top);
    TextView heading=label("☾   Dhikr Counter   ✦",23,GOLD,true);LinearLayout.LayoutParams hp=lp(-1,44);hp.topMargin=3;page.addView(heading,hp);
    LinearLayout progress=row();progress.setPadding(22,0,22,0);TextView ptxt=label("TODAY",10,MUTED,true);progress.addView(ptxt);View line=new View(this);line.setBackgroundColor(GOLD);LinearLayout.LayoutParams linep=new LinearLayout.LayoutParams(0,3,1);linep.setMargins(12,0,12,0);progress.addView(line,linep);percent=label("0%",11,GOLD,true);progress.addView(percent);page.addView(progress,lp(-1,24));
    title=label(dhikr,23,CREAM,false);title.setTypeface(Typeface.create("serif",Typeface.NORMAL));title.setGravity(Gravity.CENTER);title.setPadding(4,8,4,8);page.addView(title,lp(-1,66));
    counter=new CounterView(this);LinearLayout.LayoutParams cv=lp(-1,0);cv.weight=1;cv.topMargin=0;cv.bottomMargin=0;page.addView(counter,cv);
    LinearLayout action=row();action.setGravity(Gravity.CENTER);action.setPadding(0,8,0,8);
    TextView minus=btn("−");LinearLayout.LayoutParams small=lp(58,58);small.setMargins(12,0,12,0);action.addView(minus,small);minus.setOnClickListener(v->{count=Math.max(0,count-1);save();render();tapFeedback();});
    TextView reset=btn("↻");LinearLayout.LayoutParams sm2=lp(58,58);sm2.setMargins(12,0,12,0);action.addView(reset,sm2);reset.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("எண்ணிக்கையை மீட்டமைக்கவா?").setNegativeButton("வேண்டாம்",null).setPositiveButton("ஆம்",(d,w)->{count=0;save();render();}).show());page.addView(action);
    LinearLayout bottom=row();bottom.setBackground(bg(0xff052f25,0x3355c5a0,24));bottom.setPadding(4,8,4,5);LinearLayout.LayoutParams bp=lp(-1,72);bp.topMargin=8;page.addView(bottom,bp);
    LinearLayout sound=bottomItem("◖))","Click Sound");soundLabel=(TextView)sound.getChildAt(1);bottom.addView(sound,new LinearLayout.LayoutParams(0,-1,1));sound.setOnClickListener(v->{soundOn=!soundOn;prefs.edit().putBoolean("sound",soundOn).apply();soundLabel.setText(soundOn?"Sound On":"Click Sound");});
    LinearLayout vibration=bottomItem("▯","Vibration");vibeLabel=(TextView)vibration.getChildAt(1);bottom.addView(vibration,new LinearLayout.LayoutParams(0,-1,1));vibration.setOnClickListener(v->{vibrateOn=!vibrateOn;prefs.edit().putBoolean("vibrate",vibrateOn).apply();vibeLabel.setText(vibrateOn?"Vibration On":"Vibration Off");});
    LinearLayout edit=bottomItem("✎","Edit Dhikr");bottom.addView(edit,new LinearLayout.LayoutParams(0,-1,1));edit.setOnClickListener(v->chooseDhikr());
    render();
  }
  LinearLayout bottomItem(String icon,String name){LinearLayout item=col();item.setGravity(Gravity.CENTER);TextView i=label(icon,18,BG,true);i.setBackground(bg(GOLD,0,16));item.addView(i,lp(38,34));TextView t=label(name,10,CREAM,true);LinearLayout.LayoutParams p=lp(-1,24);p.topMargin=2;item.addView(t,p);return item;}
  void chooseDhikr(){String[] names={"ஸலவாத்து","சுப்ஹானல்லாஹ்","அல்ஹம்துலில்லாஹ்","அல்லாஹு அக்பர்","அஸ்தக்ஃபிருல்லாஹ்","லா இலாஹ இல்லல்லாஹ்"};String[] arabic={"اللَّهُمَّ صَلِّ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ ﷺ","سُبْحَانَ اللَّهِ","الْحَمْدُ لِلَّهِ","اللَّهُ أَكْبَرُ","أَسْتَغْفِرُ اللَّهَ","لَا إِلَٰهَ إِلَّا اللَّهُ"};new android.app.AlertDialog.Builder(this).setTitle("திக்ரைத் தேர்ந்தெடுக்கவும்").setItems(names,(d,n)->{dhikr=arabic[n];title.setText(dhikr);}).show();}
  void setGoal(){EditText e=new EditText(this);e.setInputType(2);e.setText(String.valueOf(goal));new android.app.AlertDialog.Builder(this).setTitle("தினசரி இலக்கு").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{try{goal=Math.max(1,Integer.parseInt(e.getText().toString()));save();render();}catch(Exception ignored){}}).show();}
  void save(){prefs.edit().putInt("count",count).putLong("total",total).putInt("goal",goal).apply();}
  void addOne(){count++;total++;save();render();tapFeedback();if(count==goal)Toast.makeText(this,"மாஷா அல்லாஹ்! இலக்கு நிறைவேறியது",Toast.LENGTH_SHORT).show();}
  void tapFeedback(){if(vibrateOn){try{android.os.Vibrator vb=(android.os.Vibrator)getSystemService(VIBRATOR_SERVICE);if(android.os.Build.VERSION.SDK_INT>=26)vb.vibrate(android.os.VibrationEffect.createOneShot(12,45));else vb.vibrate(12);}catch(Exception ignored){}}}
  void render(){if(counter!=null){counter.invalidate();percent.setText(Math.min(100,count*100/Math.max(1,goal))+"%");}}
  class CounterView extends View{
    Paint p=new Paint(3);float downX,downY;boolean down=false;
    CounterView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(true);}
    void paintText(Canvas c,String s,float x,float y,float size,int color,Typeface face){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setColor(color);p.setTypeface(face);c.drawText(s,x,y,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();float cx=w/2,cy=h*.51f;float scale=Math.min(w/360f,h/570f);c.save();c.translate(cx,cy);c.scale(scale,scale);
      // Subtle mosque arch and hanging lanterns
      p.setShader(new LinearGradient(0,-300,0,320,0xff0b4635,0xff03251d,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);Path arch=new Path();arch.moveTo(-205,280);arch.lineTo(-205,-50);arch.lineTo(-165,-75);arch.lineTo(-165,-130);arch.quadTo(-165,-250,0,-270);arch.quadTo(165,-250,165,-130);arch.lineTo(165,-75);arch.lineTo(205,-50);arch.lineTo(205,280);arch.close();c.drawPath(arch,p);p.setShader(null);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0x776d8b58);c.drawPath(arch,p);p.setStyle(Paint.Style.FILL);
      for(int x:new int[]{-150,150}){p.setColor(0x557b6337);c.drawLine(x,-270,x,-205,p);p.setColor(0xffb77b32);Path lamp=new Path();lamp.moveTo(x-14,-205);lamp.lineTo(x+14,-205);lamp.lineTo(x+9,-185);lamp.lineTo(x,-179);lamp.lineTo(x-9,-185);lamp.close();c.drawPath(lamp,p);}
      // Counter body inspired by the reference image
      Path body=new Path();body.moveTo(-115,-20);body.cubicTo(-155,-85,-145,-155,-90,-175);body.cubicTo(-40,-196,40,-196,90,-175);body.cubicTo(145,-155,155,-85,115,-20);body.cubicTo(100,10,115,80,80,110);body.cubicTo(45,145,-45,145,-80,110);body.cubicTo(-115,80,-100,10,-115,-20);body.close();
      p.setShader(new LinearGradient(-120,-160,120,145,0xff073d2d,0xff021d17,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawPath(body,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(GOLD);c.drawPath(body,p);p.setStyle(Paint.Style.FILL);
      // LCD-style counter
      p.setShadowLayer(6,0,2,0x55000000);p.setColor(0xffdce4ce);c.drawRoundRect(-74,-135,74,-78,9,9,p);p.clearShadowLayer();
      p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));p.setTextAlign(Paint.Align.RIGHT);p.setTextSize(38);p.setColor(0xff1d2823);String digits=String.format(Locale.US,"%04d",count%10000);c.drawText(digits,60,-94,p);
      // buttons on the counter
      p.setColor(GOLD);c.drawCircle(-53,-42,16,p);c.drawCircle(53,-42,16,p);paintText(c,"−",-53,-36,22,BG,Typeface.DEFAULT_BOLD);paintText(c,"↻",53,-36,18,BG,Typeface.DEFAULT_BOLD);
      p.setShadowLayer(10,0,4,0x558a5b20);p.setColor(GOLD);c.drawCircle(0,32,38,p);p.clearShadowLayer();paintText(c,"+",0,44,45,BG,Typeface.DEFAULT);
      paintText(c,"TAP TO COUNT",0,92,10,MUTED,Typeface.create("sans-serif-medium",Typeface.NORMAL));
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0x33ffffff);c.drawArc(-168,-288,168,48,-90,360,false,p);p.setColor(GOLD);c.drawArc(-168,-288,168,48,-90,360f*Math.min(1,count/(float)Math.max(goal,1)),false,p);p.setStyle(Paint.Style.FILL);
      c.restore();
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){down=true;setScaleX(.99f);setScaleY(.99f);return true;}if(e.getAction()==MotionEvent.ACTION_UP){setScaleX(1);setScaleY(1);if(down){performClick();addOne();}down=false;return true;}if(e.getAction()==MotionEvent.ACTION_CANCEL){setScaleX(1);setScaleY(1);down=false;return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
  }
}