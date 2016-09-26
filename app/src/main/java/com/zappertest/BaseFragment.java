package com.zappertest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import org.json.JSONArray;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import ORMlite.DatabaseHelper;
import classes.BaseObject;
import network.ApiHelper;
import network.URLConstants;
import utility.VolleySingleton;

public class BaseFragment extends ListFragment {
    OnBaseObjectSelectedListener mCallback;

    public static final int REFRESH_TIME = 120000; //2 Minutes

    private DatabaseHelper databaseHelper;
    private ConnectionSource connection;
    private Dao<BaseObject, String> baseObjectDao;
    ArrayList<BaseObject> baseObjects;

    Thread updateThread = null;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface OnBaseObjectSelectedListener {
        /** Called by BaseFragment when a list item is selected */
        public void onDetailSelected(int position);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = getHelper();
        connection = databaseHelper.getConnectionSource();

        loadBaseObjectList();

        updateThread = new Thread(new Runnable() {
            public void run() {
                try {
                    //Sleeps the thread for time specified in global variable
                    Thread.sleep(REFRESH_TIME);

                    loadBaseObjectList();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //Starts the thread
        updateThread.start();
    }

    @Override
    public void onStart() {
        super.onStart();

        // When in two-pane layout, set the listview to highlight the selected list item
        // (We do this during onStart because at the point the listview is available.)
        if (getFragmentManager().findFragmentById(R.id.detail_fragment) != null) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnBaseObjectSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBaseObjectSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (baseObjects != null && !baseObjects.isEmpty()) {
            int objectId = baseObjects.get(position).getId();

            // Notify the parent activity of selected item
            mCallback.onDetailSelected(objectId);

            // Set the item as checked to be highlighted when in two-pane layout
            getListView().setItemChecked(position, true);
        }
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(getActivity(),
                    DatabaseHelper.class);
        }
        return databaseHelper;
    }

    public void loadBaseObjectList() {
        try {
            baseObjectDao = databaseHelper.getDao(BaseObject.class);
            baseObjects = (ArrayList<BaseObject>) baseObjectDao.queryForAll();

            if (baseObjects.isEmpty()) {
                fetchBaseData();
            } else {
                fullListView(baseObjects);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void fullListView(ArrayList<BaseObject> array) {
        // We need to use a different list item layout for devices older than Honeycomb
        int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;

        ArrayList<String> arrayList = new ArrayList<String>();
        for (BaseObject obj : array) {
            arrayList.add(obj.getFirstName() + " " + obj.getLastName());
        }

        setListAdapter(new ArrayAdapter<String>(getActivity(), layout, arrayList));
    }

    //Calls the API to get all the base date
    public void fetchBaseData() {
        StringRequest zapperBaseObjectCall = ApiHelper.buildApiStringCall(
                Request.Method.GET,
                URLConstants.URLS.BASE_URL(),
                null,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new Gson();
                        JsonParser jsonParser = new JsonParser();
                        JsonArray jsonArray = (JsonArray) jsonParser.parse(response);

                        Type listType = new TypeToken<ArrayList<BaseObject>>() {}.getType();
                        final ArrayList<BaseObject> baseObjectResponse = gson.fromJson(jsonArray, listType);
                        if (!baseObjectResponse.isEmpty()) {
                            if (baseObjectDao != null) {
                                try {
                                    baseObjectDao = databaseHelper.getDao(BaseObject.class);


                                } catch (SQLException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                for (BaseObject baseObject : baseObjectResponse) {
                                    baseObjectDao.createOrUpdate(baseObject);
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            loadBaseObjectList();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        zapperBaseObjectCall.setTag(URLConstants.CALL_ZAPPER_BASE);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(zapperBaseObjectCall);
    }
}
