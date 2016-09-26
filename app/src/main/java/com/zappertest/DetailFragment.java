package com.zappertest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

import ORMlite.DatabaseHelper;
import classes.DetailObject;
import network.ApiHelper;
import network.URLConstants;
import utility.VolleySingleton;

public class DetailFragment extends Fragment {
    final static String ARG_POSITION = "position";
    int mCurrentItemId = -1;

    private DatabaseHelper databaseHelper;
    private ConnectionSource connection;
    private Dao<DetailObject, String> detailObjectDao;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
            mCurrentItemId = savedInstanceState.getInt(ARG_POSITION);
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.detail_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        databaseHelper = getHelper();
        connection = databaseHelper.getConnectionSource();



        // During startup, check if there are arguments passed to the fragment.
        // onStart is a good place to do this because the layout has already been
        // applied to the fragment at this point so we can safely call the method
        // below that sets the article text.
        Bundle args = getArguments();
        if (args != null) {
            // Set article based on argument passed in
            loadDetailItem(args.getInt(ARG_POSITION));
        } else if (mCurrentItemId != -1) {

        }
    }

    public void updateDetailView(DetailObject item) {
        TextView firstName = (TextView) getActivity().findViewById(R.id.first_name);
        TextView lastName = (TextView) getActivity().findViewById(R.id.last_name);
        TextView age = (TextView) getActivity().findViewById(R.id.age);
        TextView favColour = (TextView) getActivity().findViewById(R.id.favourite_colour);

        firstName.setText(item.getFirstName());
        lastName.setText(item.getLastName());
        age.setText(item.getAge() + "");
        favColour.setText(item.getFavouriteColour());

        mCurrentItemId = item.getId();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current article selection in case we need to recreate the fragment
        outState.putInt(ARG_POSITION, mCurrentItemId);
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(getActivity(),
                    DatabaseHelper.class);
        }
        return databaseHelper;
    }

    public void loadDetailItem(int id) {
        try {
            detailObjectDao = databaseHelper.getDao(DetailObject.class);
            DetailObject detailObject = (DetailObject) detailObjectDao.queryForId(id + "");

            if (detailObject != null) {
                updateDetailView(detailObject);
            } else {
                fetchDetailItem(id);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void fetchDetailItem(final int id) {
        StringRequest zapperDetailObjectCall = ApiHelper.buildApiStringCall(
                Request.Method.GET,
                URLConstants.URLS.DETAIL_URL(id + ""),
                null,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new Gson();
                        DetailObject object = gson.fromJson(response, DetailObject.class);
                        object.setId(id);
                        if (object != null) {
                            if (detailObjectDao != null) {
                                try {
                                    detailObjectDao = databaseHelper.getDao(DetailObject.class);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                detailObjectDao.createOrUpdate(object);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            updateDetailView(object);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
        zapperDetailObjectCall.setTag(URLConstants.CALL_ZAPPER_DETAIL);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(zapperDetailObjectCall);
    }
}
