package com.raspicamstreamer.raspicamstreamer;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.ArrayList;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.util.StringTokenizer;


public class CamSelectActivity extends AppCompatActivity {

    SharedPreferences preferences = null;
    ListView listView = null;
    ArrayAdapter<String> listAdapter;
    ViewGroup m_parent;

    public Context getContext() { return (Context)this; }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("SELECT OPTION");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_cam_select, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId())
        {
            case R.id.delete:
                listAdapter.remove(listAdapter.getItem(info.position));
                listAdapter.notifyDataSetChanged();
                SaveListToStorage();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    void SaveListToStorage()
    {
        SharedPreferences.Editor editor = preferences.edit();
        String str_all="";
        for(int i = 0; i < listAdapter.getCount(); i++)
            str_all += listAdapter.getItem(i) + ",";
        if(str_all.length() != 0)
        {
            editor.putString("my_cams", str_all.substring(0, str_all.length()-1));
        }
        editor.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button fab = (Button) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Dialog d = new Dialog(view.getContext());
                d.setContentView(R.layout.dialog);
                d.setTitle("Add camera");
                d.setCancelable(true);
                final EditText edit = (EditText) d.findViewById(R.id.editTextPlanet);
                Button b = (Button) d.findViewById(R.id.button1);
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String cam_str = edit.getText().toString();
                        if(!cam_str.isEmpty()) {
                            listAdapter.add(edit.getText().toString());
                            listAdapter.notifyDataSetChanged();
                            SaveListToStorage();
                        }
                        d.dismiss();
                    }
                });

                d.show();
                //adapter.add(new CameraHolder("sdf"));
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());

        listView = (ListView) findViewById(R.id.cameras);
        listAdapter = new ArrayAdapter<String>(this, R.layout.camera_addr);

        //load saved cameras
        String settingsString = preferences.getString("my_cams", "");
        StringTokenizer st = new StringTokenizer(settingsString, ",");
        while (st.hasMoreTokens())
            listAdapter.add(st.nextToken());


        listView.setAdapter(listAdapter);
        registerForContextMenu(listView);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index

                Intent intent = new Intent(getContext(), VideoActivity.class);
                intent.putExtra(VideoActivity.CAMERA, listView.getItemAtPosition(position).toString());
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_cam_select, menu);
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
}
