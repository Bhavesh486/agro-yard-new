package com.projects.agroyard.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.projects.agroyard.R;

public class LoginUsers extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Handle login button click
        view.findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());

        // Handle signup redirection
        view.findViewById(R.id.signupText).setOnClickListener(v -> openSignup());
    }

    private void loginUser() {
        // TODO: Add actual login logic (authentication, API call, etc.)

        // After successful login, navigate to BottomNavigationFragment
        Fragment homeFragment = new BottomNavigationFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, homeFragment);
        transaction.commit();
    }

    private void openSignup() {
        Fragment signupFragment = new SignupUser();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, signupFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
