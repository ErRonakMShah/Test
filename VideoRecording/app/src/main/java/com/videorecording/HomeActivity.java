package com.videorecording;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;


public class HomeActivity extends Activity implements View.OnClickListener{

    private Button btn_takePicture,btn_recordVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_home);

        initialization();
        eventListener();
    }

    public void initialization(){
        btn_takePicture=(Button)findViewById(R.id.takepicture);
        btn_recordVideo=(Button)findViewById(R.id.recordvideo);
    }

    public void eventListener(){
        btn_takePicture.setOnClickListener(HomeActivity.this);
        btn_recordVideo.setOnClickListener(HomeActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.takepicture:
                Intent i=new Intent(HomeActivity.this,TakePictureActivity.class);
                startActivity(i);
                break;
            case R.id.recordvideo:
                Intent j=new Intent(HomeActivity.this,VideoActivity.class);
                startActivity(j);
                break;
        }
    }

}
