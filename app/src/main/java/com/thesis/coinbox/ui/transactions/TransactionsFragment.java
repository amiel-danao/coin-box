package com.thesis.coinbox.ui.transactions;

import static com.thesis.coinbox.utilities.Constants.TRANSACTIONS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.thesis.coinbox.R;
import com.thesis.coinbox.RequireLoginFragment;
import com.thesis.coinbox.adapters.TransactionsAdapter;
import com.thesis.coinbox.data.model.Transaction;
import com.thesis.coinbox.databinding.FragmentELoadBinding;
import com.thesis.coinbox.databinding.FragmentTransactionsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends RequireLoginFragment {
    private TransactionsAdapter adapter;

    private final List<Transaction> transactions = new ArrayList<>();
    private String type = "money";
    private FragmentTransactionsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new TransactionsAdapter();

        binding.recyclerView.setAdapter(adapter);

        return binding.getRoot();
    }

    private void refreshList(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference transactionsRef = db.collection(TRANSACTIONS_COLLECTION);

        Query query = transactionsRef.where(Filter.or(
                        Filter.equalTo("sender", db.collection(USERS_COLLECTION).document(loggedInUser.getUserId())),
                        Filter.equalTo("receiver", db.collection(USERS_COLLECTION).document(loggedInUser.getUserId()))
                ))
                .whereEqualTo("type", type)
                .orderBy("date", Query.Direction.DESCENDING);

        adapter.notifyItemRangeRemoved(0, transactions.size());
        transactions.clear();
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transaction.setId(document.getId());
                        if(transactions.contains(transaction))
                            continue;
                        transactions.add(transaction);
                        adapter.notifyItemInserted(transactions.size()-1);
                    }
                }
                else{
                    String message = task.getException().getMessage();
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    @Override
    public void onFetchedUser() {
        adapter.setTransactions(transactions, loggedInUser, requireContext());
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                type = tab.getText().toString().toLowerCase(Locale.getDefault());
                refreshList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Called when a tab is unselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Called when a tab is reselected (tab was already selected)
            }
        });
        refreshList();
    }
}
