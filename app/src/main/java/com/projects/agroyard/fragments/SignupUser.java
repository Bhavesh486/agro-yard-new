package com.projects.agroyard.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.projects.agroyard.R;
import com.projects.agroyard.client.StateClient;
import com.projects.agroyard.client.callback.StateDataCallback;
import com.projects.agroyard.constants.Constants;
import com.projects.agroyard.model.StateModel;
import com.projects.agroyard.model.StatesModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignupUser extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signup_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSpinnerAdapter(view);

        getStateDistList(statesDistMap -> {
            Log.i("Map", statesDistMap.toString());
            if (statesDistMap != null && !statesDistMap.isEmpty()) {
                populateStateAndDistricts(view, statesDistMap);
            }
        });

        view.findViewById(R.id.loginText).setOnClickListener(v -> openLogin());

    }
    private void getStateDistList(final StateDataCallback callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        StateClient stateClient = retrofit.create(StateClient.class);
        stateClient.getStateDistrict().enqueue(new Callback<StatesModel>() {
            @Override
            public void onResponse(Call<StatesModel> call, Response<StatesModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatesModel stateList = response.body();
                    if(!stateList.getStates().isEmpty()) {
                        Map<String, List<String>> statesDistMap = new HashMap<>();
                        stateList.getStates().forEach(stateModel -> statesDistMap.put(stateModel.getState(), stateModel.getDistricts()));
                        callback.onStateDataFetched(statesDistMap);
                    }
                }
                else
                    Log.e("API Response", "Response was not successful. Status code: " + response.code());
            }

            @Override
            public void onFailure(Call<StatesModel> call, Throwable throwable) {
                Log.e("API Error", "Failed to fetch data", throwable);
            }
        });
    }

    private void setSpinnerAdapter(View view) {
        // Array of spinner IDs and their corresponding string arrays
        int[] spinnerIds = {
                R.id.userTypeSpinner,
                R.id.stateSpinner,
                R.id.districtSpinner
        };

        int[] stringArrays = {
                R.array.user_types,
                R.array.states,
                R.array.districts
        };

        // Loop through the spinner IDs and string arrays to set the adapters
        for (int i = 0; i < spinnerIds.length; i++) {
            Spinner spinner = view.findViewById(spinnerIds[i]);
            if(i > 0) {
                spinner.setEnabled(false);
            }
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    getContext(), stringArrays[i], R.layout.spinner_item); // Custom layout applied
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

        // Handle visibility of market name based on user type selection
        TextView marketName = view.findViewById(R.id.marketName);
        Spinner userTypeSpinner = view.findViewById(R.id.userTypeSpinner);

        userTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals(Constants.MEMBER)) {
                    marketName.setVisibility(View.VISIBLE);
                } else {
                    marketName.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void populateStateAndDistricts(View view, Map<String, List<String>> statesDistMap) {
        Spinner state = view.findViewById(R.id.stateSpinner);
        Spinner district = view.findViewById(R.id.districtSpinner);

        state.setEnabled(true);

        List<String> statesList = new ArrayList<>();
        statesList.add(Constants.SELECT_STATE);
        statesList.addAll(statesDistMap.keySet());

        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, statesList);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        state.setAdapter(stateAdapter);


        ArrayList<String> districtList = new ArrayList<>(List.of(Constants.SELECT_DISTRICT));
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, districtList);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        district.setAdapter(districtAdapter);

        state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position != 0) {
                    district.setEnabled(true);
                    String selectedState = statesList.get(position);
                    List<String> districts = statesDistMap.get(selectedState);
                    districts.add(0, Constants.SELECT_DISTRICT);

                    district.setSelection(0);
                    districtList.clear();
                    districtList.addAll(districts != null && !districts.isEmpty() ? districts : List.of("Select District"));
                    districtAdapter.notifyDataSetChanged();
                } else {
                    district.setEnabled(false);
                    district.setSelection(0);
                    districtList.clear();
                    districtList.add(Constants.SELECT_DISTRICT);
                    districtAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void openLogin() {
        Fragment loginUsers = new LoginUsers();
        FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, loginUsers);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}