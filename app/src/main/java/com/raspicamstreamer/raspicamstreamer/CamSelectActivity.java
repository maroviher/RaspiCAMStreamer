package com.raspicamstreamer.raspicamstreamer;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
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
    CamerasAdapter adapter = null;
    ViewGroup m_parent;

    class CameraHolder {
        String name;
        EditText editText;
        CameraHolder(String name)
        {
            this.name = name;
        }
    }
    class ViewHolder
    {
        EditText tv_cam;
        Button bt_remove;
    }

    public class CamerasAdapter extends ArrayAdapter<CameraHolder> {
        Context context;

        public CamerasAdapter(Context context, ArrayList<CameraHolder> data) {
            super(context, 0, data);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CameraHolder cam = getItem(position);
            View row = convertView;
            ViewHolder view_hold;
            if (row == null) {
                m_parent = parent;
                Log.d("position", String.format("%d, cam.name=%s", position, cam.name));
                row = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
                view_hold = new ViewHolder();
                cam.editText = view_hold.tv_cam = (EditText) row.findViewById(R.id.cam_addr);
                view_hold.tv_cam.setText(cam.name);
                view_hold.tv_cam.setTag(view_hold);
                view_hold.tv_cam.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        ViewHolder view_hold = (ViewHolder) v.getTag();
                        if (!hasFocus) {
                            view_hold.bt_remove.setText("DEL");
                            //Log.d("vasa", String.format("!hasFocus, %s", v.toString()));
                        }
                        else {
                            view_hold.bt_remove.setText("SET");
                            //Log.d("vasa", String.format("hasFocus, %s", v.toString()));
                        }
                    }
                });

                view_hold.bt_remove = (Button) row.findViewById(R.id.cam_del_set);
                view_hold.bt_remove.setTag(cam);
                Log.d("vasa", String.format("view_hold.bt_remove.setTag, position=%d, cam.name=%s", position, cam.name));
                view_hold.bt_remove.setText("DEL");
                view_hold.bt_remove.setOnClickListener(mMyButtonClickListener);

                row.setTag(view_hold);
                /*holder = new CameraHolder();
                row.setTag(holder);
                Log.d("vasa", String.format("setTag=%s, row == null, position=%d", row.getTag().toString(), position));
                holder.name = (TextView) row.findViewById(R.id.cam_addr);
                holder.b = (Button) row.findViewById(R.id.cam_del);
                holder.b.setText(String.format("%d", position));
                holder.b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //CameraHolder click_holder = (CameraHolder) (View)v.getParent();
                        View par = (View)v.getParent();

                        if(par.getTag() != null)
                            Log.d("vasa getTag", par.getTag().toString());
                        else
                            Log.d("vasa", "pusto");
                        /*for(int i=0; i<data.size(); i++)
                        {
                            if(data.get(i) == click_holder.name.getText()) {
                                data.remove(i);
                                break;
                            }
                        }*/

                        //Log.d("qwe", String.format("w=%s, data.size()=%d", w.name.getText().toString(), data.size()));
                        /*notifyDataSetChanged();
                        SharedPreferences.Editor editor = preferences.edit();
                        String str_all="";
                        for(String str_tmp : data)
                            str_all += str_tmp + ",";
                        if(str_all.length() != 0)
                        {
                            editor.putString("my_cams", str_all.substring(0, str_all.length()-1));
                            editor.commit();
                        }
                    }
                });*/
            }
            else
            {
                view_hold = (ViewHolder) row.getTag();

                //Log.d("vasa", String.format("setTag=%s, row != null, position=%d", row.getTag().toString(), position));
            }

            view_hold.tv_cam.setText(cam.name);
            /*String name1 = data.get(position);
            holder.name.setText(name1);
            holder.b.setText(String.format("%d", position));*/

            return row;
        }

        private View.OnClickListener mMyButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.cam_del_set:
                        Button but = (Button) v;
                        CameraHolder cam = (CameraHolder) v.getTag();
                        if(but.getText() == "DEL") {
                            Log.d("vasa", String.format("DEL, position=%d, cam.name=%s", adapter.getPosition(cam), cam.name));
                            adapter.remove(cam);
                            adapter.notifyDataSetChanged();
                        }
                        /*else if(but.getText() == "SET")
                        {
                            but.setText("DEL");

                            //Log.d("vasa2", String.format("position=%d, cam.name=%s", position, cam.name));
                            int pos = adapter.getPosition(cam);
                            Log.d("vasa", String.format("SET, position=%d, cam.name=%s", pos, cam.name));
                            String tmp = cam.editText.getText().toString();
                            Log.d("vasa", String.format("tmp=%s", tmp));
                            adapter.remove(cam);
                            adapter.insert(new CameraHolder(tmp), pos);
                            Log.d("vasa", String.format("adapter.getItem(pos).name=%s", adapter.getItem(pos).name));
                            getView(pos, null, m_parent);
                            adapter.notifyDataSetChanged();
                        }*/
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.add(new CameraHolder("sdf"));
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("my_cams", "192.168.42.129:12345,192.168.1.2:123,192.168.11.9:1234");
        editor.commit();

        listView = (ListView) findViewById(R.id.cameras);
        ArrayList<CameraHolder> arrayOfUsers = new ArrayList<>();
        adapter = new CamerasAdapter(this, arrayOfUsers);

        for(int i = 0; i < 10; i++)
            adapter.add(new CameraHolder(String.format("%d", i)));

        /*String settingsString = preferences.getString("my_cams", "");
        StringTokenizer st = new StringTokenizer(settingsString, ",");
        while (st.hasMoreTokens())
            adapter.add(new CameraHolder(st.nextToken()));*/


        listView.setAdapter(adapter);
        /*listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;
                Log.d("asfasdf", "asdfaasdf");

                // ListView Clicked item value
                /*User itemValue = (User) listView.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + "  ListItem : " + itemValue.name, Toast.LENGTH_LONG).show();
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cam_select, menu);
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
