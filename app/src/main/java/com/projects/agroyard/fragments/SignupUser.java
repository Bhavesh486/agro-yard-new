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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private FirebaseAuth mAuth; // Firebase Auth instance
    private EditText emailEditText, passwordEditText; // Fields for email and password

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase-related UI elements
        emailEditText = view.findViewById(R.id.emailInput);
        passwordEditText = view.findViewById(R.id.passwordInput);
        Button signupButton = view.findViewById(R.id.signupButton);

        setSpinnerAdapter(view);

        getStateDistList(statesDistMap -> {
            Log.i("Map", statesDistMap.toString());
            if (statesDistMap != null && !statesDistMap.isEmpty()) {
                populateStateAndDistricts(view, statesDistMap);
            }
        });

        view.findViewById(R.id.loginText).setOnClickListener(v -> openLogin());

        // Signup button click listener
        signupButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(getContext(), "Signup successful: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            openLogin(); // Redirect to login after signup
                        } else {
                            Toast.makeText(getContext(), "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
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
                    if (!stateList.getStates().isEmpty()) {
                        Map<String, List<String>> statesDistMap = new HashMap<>();
                        stateList.getStates().forEach(stateModel -> statesDistMap.put(stateModel.getState(), stateModel.getDistricts()));
                        callback.onStateDataFetched(statesDistMap);
                    }
                } else {
                    Log.e("API Response", "Response was not successful. Status code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<StatesModel> call, Throwable throwable) {
                Log.e("API Error", "Failed to fetch data", throwable);
            }
        });
    }

    private void setSpinnerAdapter(View view) {
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

        for (int i = 0; i < spinnerIds.length; i++) {
            Spinner spinner = view.findViewById(spinnerIds[i]);
            if (i > 0) {
                spinner.setEnabled(false);
            }
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    getContext(), stringArrays[i], R.layout.spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

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
                if (position != 0) {
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
                // No action needed
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