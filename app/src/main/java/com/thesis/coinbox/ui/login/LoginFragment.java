package com.thesis.coinbox.ui.login;

import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.thesis.coinbox.data.model.LoggedInUser;
import com.thesis.coinbox.databinding.FragmentLoginBinding;

import com.thesis.coinbox.R;
import com.thesis.coinbox.ui.main.MainActivity;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private NavController navController;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();


        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final ProgressBar loadingProgressBar = binding.loading;


        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isUserNameValid = LoginViewModel.isUserNameValid(usernameEditText.getText().toString().trim());
                boolean isPasswordValid = LoginViewModel.isPasswordValid(passwordEditText.getText().toString().trim());

                loginButton.setEnabled(isUserNameValid && isPasswordValid);

                if(!isUserNameValid)
                    usernameEditText.setError("Invalid");

                if(!isPasswordValid)
                    passwordEditText.setError("Invalid");
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loadingProgressBar.setVisibility(View.VISIBLE);

                login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);

            login(usernameEditText.getText().toString().trim(),
                    passwordEditText.getText().toString().trim());
        });

        navController = Navigation.findNavController(requireActivity(), R.id.navHostFragment);

        binding.registerLink.setOnClickListener(v -> navController.navigate(R.id.registerFragment));
    }

    public void login(String username, String password) {
        // can be launched in a separate asynchronous job

        firebaseAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(task.getResult().getUser().getUid()).get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        startActivity(new Intent(requireContext(), MainActivity.class));
                                        requireActivity().finish();
                                    }
                                    else{
                                        showLoginFailed(task2.getException().getMessage());
                                    }
                                    binding.loading.setVisibility(View.GONE);
                                });

                    } else {
                        showLoginFailed(task.getException().getMessage());
                        binding.loading.setVisibility(View.GONE);
                    }
                });
    }

    private void showLoginFailed(String errorString) {
        Toast.makeText(requireContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}