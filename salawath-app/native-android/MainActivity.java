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
  private static final int BG=Color.rgb(5,27,22), PANEL=Color.rgb(12,48,36), GOLD=Color.rgb(235,202,119), CREAM=Color.rgb(255,247,225), MUTED=Color.rgb(174,201,185);
  private SharedPreferences prefs;
  private int count=0, goal=1000;
  private long total=0;
  private String dhikr="اللَّهُمَّ صَلِّ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ ﷺ";
  private TextView countText, goalText, percentText, dhikrText, modeText;
  private MeterView meter;
  private LinearLayout root;
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    prefs=getSharedPreferences("salawath_native",MODE_PRIVATE);
    String today=new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date());
    String saved=prefs.getString("day","");
    if(!today.equals(saved)){prefs.edit().putString("day",today).putInt("count",0).apply();}
    count=prefs.getInt("count",0);total=prefs.getLong("total",0);goal=prefs.getInt("goal",1000);
    buildUi();
  }
  private GradientDrawable shape(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);if(stroke!=0)d.setStroke(1,stroke);return d;}
  private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);t.setGravity(Gravity.CENTER);return t;}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
  private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER);return l;}
  private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
  private void pad(View v,int a,int b,int c,int d){v.setPadding(a,b,c,d);}
  private TextView button(String label, boolean primary){TextView v=text(label,14,primary?BG:CREAM,true);v.setBackground(shape(primary?GOLD:Color.rgb(10,38,29),primary?GOLD:0x55EBC A77,22));v.setPadding(8,14,8,14);v.setClickable(true);v.setFocusable(true);return v;}
  private void buildUi(){
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);root=column();pad(root,18,8,18,24);scroll.addView(root);setContentView(scroll);
    LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);TextView logo=text("✦  YASEENIS",15,GOLD,true);logo.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);header.addView(logo,new LinearLayout.LayoutParams(0,48,1));TextView badge=text("☾  DHIKR MODE",10,MUTED,true);badge.setBackground(shape(PANEL,0x44EBC877,40));pad(badge,12,8,12,8);header.addView(badge);root.addView(header);
    TextView crescent=text("☾  ✧  ☽",28,GOLD,false);LinearLayout.LayoutParams c=lp(-1,48);c.topMargin=12;root.addView(crescent,c);
    TextView title=text("ஸலவாத்து முபாரக்",26,CREAM,true);root.addView(title,lp(-1,-2));
    TextView subtitle=text("அமைதியாக நினைவு கூருங்கள்",13,MUTED,false);root.addView(subtitle,lp(-1,32));
    dhikrText=text(dhikr,23,GOLD,false);dhikrText.setGravity(Gravity.CENTER);pad(dhikrText,4,10,4,14);root.addView(dhikrText,lp(-1,-2));
    LinearLayout card=column();card.setBackground(shape(PANEL,0x55EBC877,30));pad(card,18,20,18,18);LinearLayout.LayoutParams cp=lp(-1,-2);cp.topMargin=8;root.addView(card,cp);
    card.addView(text("உங்கள் எண்ணிக்கை",12,MUTED,true),lp(-1,24));
    countText=text("0",52,CREAM,true);countText.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));card.addView(countText,lp(-1,68));
    card.addView(text("இன்றைய திக்ர்",11,MUTED,false),lp(-1,22));
    meter=new MeterView(this);LinearLayout.LayoutParams mp=lp(-1,224);mp.topMargin=8;card.addView(meter,mp);
    TextView hint=text("தொடுவதற்கு மெதுவாக அழுத்தவும்",11,MUTED,false);card.addView(hint,lp(-1,24));
    LinearLayout quick=row();quick.setPadding(0,14,0,0);int[] adds={10,33,100};String[] labels={"+10","+33","+100"};
    for(int i=0;i<adds.length;i++){final int n=adds[i];TextView b=smallButton(labels[i],false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.setMargins(4,0,4,0);quick.addView(b,bp);b.setOnClickListener(v->add(n));}
    TextView custom=smallButton("தனிப்பயன்",true);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.setMargins(4,0,4,0);quick.addView(custom,bp);custom.setOnClickListener(v->customCount());card.addView(quick);
    LinearLayout stats=row();stats.setPadding(0,14,0,0);LinearLayout s1=stat("இலக்கு / GOAL");LinearLayout s2=stat("நிறைவு / DONE");stats.addView(s1,new LinearLayout.LayoutParams(0,-2,1));stats.addView(s2,new LinearLayout.LayoutParams(0,-2,1));card.addView(stats);goalText=(TextView)s1.getChildAt(1);percentText=(TextView)s2.getChildAt(1);
    TextView goalBtn=smallButton("⚙  தினசரி இலக்கு அமைக்க",false);LinearLayout.LayoutParams gp=lp(-1,-2);gp.topMargin=12;card.addView(goalBtn,gp);goalBtn.setOnClickListener(v->setGoal());
    TextView reset=smallButton("↻  இன்றைய எண்ணிக்கையை மீட்டமை",false);LinearLayout.LayoutParams rp=lp(-1,-2);rp.topMargin=8;card.addView(reset,rp);reset.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("எண்ணிக்கையை மீட்டமைக்கவா?").setMessage("இன்றைய எண்ணிக்கை மட்டும் பூஜ்யமாகும்.").setNegativeButton("வேண்டாம்",null).setPositiveButton("மீட்டமை",(d,w)->{count=0;save();render();}).show());
    TextView selectTitle=text("தஸ்பீஹ் தேர்வு",16,CREAM,true);selectTitle.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams st=lp(-1,50);st.topMargin=10;root.addView(selectTitle,st);
    String[][] options={{"ஸலவாத்து","اللَّهُمَّ صَلِّ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ ﷺ"},{"சுப்ஹானல்லாஹ்","سُبْحَانَ اللَّهِ"},{"அல்ஹம்துலில்லாஹ்","الْحَمْدُ لِلَّهِ"},{"அல்லாஹு அக்பர்","اللَّهُ أَكْبَرُ"},{"அஸ்தக்ஃபிருல்லாஹ்","أَسْتَغْفِرُ اللَّهَ"}};
    for(String[] o:options){TextView item=text(o[0]+"     ·     "+o[1],14,CREAM,false);item.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);pad(item,14,15,14,15);item.setBackground(shape(PANEL,0x22FFFFFF,16));LinearLayout.LayoutParams ip=lp(-1,-2);ip.topMargin=7;root.addView(item,ip);item.setOnClickListener(v->{dhikr=o[1];dhikrText.setText(dhikr);modeText.setText(o[0]);Toast.makeText(this,o[0]+" தேர்வு செய்யப்பட்டது",Toast.LENGTH_SHORT).show();});}
    modeText=text("தற்போதைய திக்ர்: ஸலவாத்து",11,MUTED,false);LinearLayout.LayoutParams fp=lp(-1,44);fp.topMargin=10;root.addView(modeText,fp);
    TextView footer=text("YASEENIS  •  REMEMBER WITH PEACE",10,0xff668978,true);LinearLayout.LayoutParams ft=lp(-1,48);root.addView(footer,ft);render();
  }
  private LinearLayout stat(String label){LinearLayout l=column();l.setBackground(shape(0x1affffff,0x22ffffff,16));pad(l,10,10,10,10);TextView a=text(label,10,MUTED,true);a.setGravity(Gravity.LEFT);l.addView(a);TextView b=text("0",19,GOLD,true);b.setGravity(Gravity.LEFT);l.addView(b);return l;}
  private TextView smallButton(String label,boolean primary){TextView v=text(label,12,primary?BG:CREAM,true);v.setBackground(shape(primary?GOLD:0xff0a281e,primary?GOLD:0x44EBC877,14));pad(v,5,13,5,13);return v;}
  private void add(int n){count+=n;total+=n;save();render();if(android.os.Build.VERSION.SDK_INT>=26)((android.os.Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(android.os.VibrationEffect.createOneShot(14,45));if(count>=goal)Toast.makeText(this,"மாஷா அல்லாஹ்! இலக்கு நிறைவேறியது 🌙",Toast.LENGTH_SHORT).show();}
  private void save(){prefs.edit().putInt("count",count).putLong("total",total).putInt("goal",goal).apply();}
  private void render(){if(countText==null)return;countText.setText(String.format(Locale.US,"%,d",count));goalText.setText(String.format(Locale.US,"%,d",goal));percentText.setText(Math.min(100,(int)(count*100L/Math.max(goal,1)))+"%");meter.invalidate();}
  private void customCount(){EditText input=new EditText(this);input.setInputType(2);input.setHint("எண்ணிக்கை");new android.app.AlertDialog.Builder(this).setTitle("எண்ணிக்கையைச் சேர்க்க").setView(input).setNegativeButton("ரத்து",null).setPositiveButton("சேர்",(d,w)->{try{int n=Integer.parseInt(input.getText().toString());if(n>0&&n<=1000000)add(n);else Toast.makeText(this,"சரியான எண்ணை உள்ளிடவும்",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"எண்ணை உள்ளிடவும்",Toast.LENGTH_SHORT).show();}}).show();}
  private void setGoal(){EditText input=new EditText(this);input.setInputType(2);input.setText(String.valueOf(goal));new android.app.AlertDialog.Builder(this).setTitle("தினசரி இலக்கு").setView(input).setNegativeButton("ரத்து",null).setPositiveButton("சேமி",(d,w)->{try{int n=Integer.parseInt(input.getText().toString());if(n>0&&n<=10000000){goal=n;save();render();}}catch(Exception ignored){}}).show();}
  private class MeterView extends View {
    Paint p=new Paint(3);float downX,downY;boolean pressed=false;
    MeterView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;float r=Math.min(getWidth(),getHeight())*.39f;
      p.setShader(new RadialGradient(cx-r*.3f,cy-r*.42f,r*1.9f,new int[]{0xff2b7954,0xff145039,0xff082b20},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,r,p);p.setShader(null);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(GOLD);c.drawCircle(cx,cy,r,p);p.setStrokeWidth(1);p.setColor(0x66EBC877);c.drawCircle(cx,cy,r-9,p);
      p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setColor(GOLD);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(37);c.drawText("۞",cx,cy-12,p);
      p.setColor(CREAM);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextSize(17);c.drawText("தஸ்பீஹ்  +1",cx,cy+25,p);
      p.setColor(MUTED);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(10);c.drawText("TAP TO COUNT",cx,cy+46,p);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0x33ffffff);c.drawArc(cx-r-9,cy-r-9,cx+r+9,cy+r+9,-90,360,false,p);p.setColor(GOLD);c.drawArc(cx-r-9,cy-r-9,cx+r+9,cy+r+9,-90,360f*Math.min(1f,count/(float)Math.max(goal,1)),false,p);p.setStyle(Paint.Style.FILL);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){pressed=true;setScaleX(.985f);setScaleY(.985f);return true;}if(e.getAction()==MotionEvent.ACTION_UP){setScaleX(1f);setScaleY(1f);if(pressed){performClick();add(1);}pressed=false;return true;}if(e.getAction()==MotionEvent.ACTION_CANCEL){setScaleX(1f);setScaleY(1f);pressed=false;return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
  }
}