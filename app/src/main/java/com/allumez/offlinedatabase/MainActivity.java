package com.allumez.offlinedatabase;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /*
     * this is the url to our webservice
     * make sure you are using the ip instead of localhost
     * it will not work if you are using localhost
     * */
    public static final String URL_SAVE_NAME = "http://api.hostingfunda.com/Offline-DB/offlinedb-3.0.php";

    //database helper object
    private DatabaseHelper db;

    //View objects
    private Button buttonSave,buttonDelete,buttonRefresh;
    private EditText editTextName,editTextId,editTextPhone;
    private ListView listViewNames;

    //List to store all the names
    private List<Name> names;

    //1 means data is synced and 0 means data is not synced
    public static final int NAME_SYNCED_WITH_SERVER = 1;
    public static final int NAME_NOT_SYNCED_WITH_SERVER = 0;

    //a broadcast to know weather the data is synced or not
    public static final String DATA_SAVED_BROADCAST = "net.simplifiedcoding.datasaved";

    //Broadcast receiver to know the sync status
    private BroadcastReceiver broadcastReceiver;

    //adapterobject for list view
    private NameAdpater nameAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(new NetworkStateChecker(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        //initializing views and objects
        db = new DatabaseHelper(this);
        names = new ArrayList<>();

        buttonSave = (Button) findViewById(R.id.buttonSave);
        buttonDelete= (Button) findViewById(R.id.buttonDelete);
        buttonRefresh= (Button) findViewById(R.id.buttonRefresh);

        editTextName = (EditText) findViewById(R.id.editTextName);
        editTextPhone = (EditText) findViewById(R.id.editTextPhone);
        editTextId = (EditText) findViewById(R.id.editTextId);

        listViewNames = (ListView) findViewById(R.id.listViewNames);

        //adding click listener to button
        buttonSave.setOnClickListener(this);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                recreate();
            }
        });
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteByCheckingTheServer();
            }
        });
        //calling the method to load all the stored names
        loadNames();

        //the broadcast receiver to update sync status
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //loading the names again
                loadNames();
            }
        };

        //registering the broadcast receiver to update sync status
        registerReceiver(broadcastReceiver, new IntentFilter(DATA_SAVED_BROADCAST));
    }
    /*
     *
     * this method will
     * load the names from the database
     * with updated sync status
     *
     */
    private void loadNames()
    {
        names.clear();
        Cursor cursor = db.getNames();
        if (cursor.moveToFirst())
        {
            do
            {
                Name name = new Name(
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PHONE)),
                        cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS))
                );
                names.add(name);
            }
            while (cursor.moveToNext());
        }

        nameAdapter = new NameAdpater(this, R.layout.names, names);
        listViewNames.setAdapter(nameAdapter);
    }
    public int deleteNames()
    {
        Cursor cursor = db.deleteTabledata();
//        int v = cursor.getCount();
//        Log.e("v", String.valueOf(v));

        if (cursor.moveToFirst()) {
            do {
                Name name = new Name(
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PHONE)),
                        cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS))
                );
                names.add(name);
            } while (cursor.moveToNext());
        }
        nameAdapter = new NameAdpater(this, R.layout.names, names);
        listViewNames.setAdapter(nameAdapter);

        return 0;
    }

    /*
     * this method will simply refresh the list
     */
    private void refreshList() {
        nameAdapter.notifyDataSetChanged();
    }

    /*
     * this method is saving the name to ther server
     */


    private void saveNameToServer() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving Name...");
        progressDialog.show();
        Cursor c = db.getId();

//        int id = nameAdapter.getId();

        final String online = "1";
        final String mId = String.valueOf(c.getCount()+1);
        Log.e("mId",mId);

        final String name = editTextName.getText().toString().trim();
        final String phone = editTextPhone.getText().toString().trim();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_SAVE_NAME,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.e("Response",response);
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("error")) {
                                //if there is a success
                                //storing the name to sqlite with status synced
                                saveNameToLocalStorage(mId,name,phone, NAME_NOT_SYNCED_WITH_SERVER);
                                loadNames();
                            }
                            else
                            {
                                //if there is some error
                                //saving the name to sqlite with status unsynced
                                saveNameToLocalStorage(mId,name,phone, NAME_NOT_SYNCED_WITH_SERVER);
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        //on error storing the name to sqlite with status unsynced
                        saveNameToLocalStorage(mId,name, phone,NAME_NOT_SYNCED_WITH_SERVER);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
//                params.put("name", name);
                params.put("online", online);
//                params.put("id", mId);
//                params.put("phone", phone);

                Log.e("123", mId+" "+name+" "+phone);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    //saving the name to local storage
    private void saveNameToLocalStorage(String id,String name, String phone, int status) {
        editTextName.setText("");
        editTextPhone.setText("");

        db.addName(name,phone,status);
        Name n = new Name(id,name,phone, status);

        names.add(n);
        refreshList();
    }

    @Override
    public void onClick(View view) {
        saveNameToServer();
    }
    private void deleteByCheckingTheServer() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Delete Name...");
        progressDialog.show();

        int a = nameAdapter.getId();

        final String mId = String.valueOf(a);


        final String name = editTextName.getText().toString().trim();
        final String phone = editTextPhone.getText().toString().trim();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://api.hostingfunda.com/Offline-DB/checktodelete.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                        Log.e("Response",response);
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (!obj.getBoolean("record")) {
                                //if there is a success
                                //storing the name to sqlite with status synced
                                deleteNameFromLocalStorage(mId,name,phone, NAME_NOT_SYNCED_WITH_SERVER);
                                deleteNames();
                                refreshList();

                            }
                            else
                            {
                                //if there is some error
                                //saving the name to sqlite with status unsynced
                                deleteNameFromLocalStorage(mId,name,phone, NAME_NOT_SYNCED_WITH_SERVER);
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        //on error storing the name to sqlite with status unsynced
                        deleteNameFromLocalStorage(mId,name, phone,NAME_NOT_SYNCED_WITH_SERVER);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("id", mId);
                Log.e("Delete ID",mId);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }
    private void deleteNameFromLocalStorage(String id,String name, String phone, int status) {

        db.deleteTabledata();
        Name n = new Name(id,name,phone, status);

        names.remove(n);
        refreshList();
    }
}