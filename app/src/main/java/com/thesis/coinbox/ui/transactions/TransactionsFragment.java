package com.thesis.coinbox.ui.transactions;

import static com.thesis.coinbox.utilities.Constants.TRANSACTIONS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


public class TransactionsFragment extends RequireLoginFragment {
    private TransactionsAdapter adapter;

    private final List<Transaction> transactions = new ArrayList<>();
    private String type = "money";
    private FragmentTransactionsBinding binding;

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new TransactionsAdapter();

        binding.recyclerView.setAdapter(adapter);



        // Initialize the ActivityResultLauncher for requesting permissions to write to external storage
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean isGranted) {
                if (isGranted) {
                    // permission granted, proceed with creating the DOCX file
                    createDocxFile(transactions);
                } else {
                    // permission denied, show a toast message
                    Toast.makeText(requireContext(), "Permission denied. Cannot save the file", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return binding.getRoot();
    }

    private void refreshList() {
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
                        if (transactions.contains(transaction))
                            continue;
                        transactions.add(transaction);
                        adapter.notifyItemInserted(transactions.size() - 1);
                    }
                } else {
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
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] months = getResources().getStringArray(R.array.spinnerItems);
                String filter = months[i];
                if (filter.equals("All")) {
                    refreshList();
                    return;
                }
                int targetYear = Calendar.getInstance().get(Calendar.YEAR); // Replace with the desired year

                // Calculate the first and last day of the target month
                int targetMonth = i;
                LocalDate startDate = LocalDate.of(targetYear, targetMonth, 1);
                Date startDateTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                int lastDayOfMonth = startDate.getMonth().length(targetYear % 4 == 0);
                LocalDate endDate = LocalDate.of(targetYear, targetMonth, lastDayOfMonth);
                Date endDateTime = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Query collection = db.collection(TRANSACTIONS_COLLECTION);
                Query query = collection.whereGreaterThanOrEqualTo("date", startDateTime)
                        .whereLessThanOrEqualTo("date", endDateTime)
                        .where(Filter.or(
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
                                if (transactions.contains(transaction))
                                    continue;
                                transactions.add(transaction);
                                adapter.notifyItemInserted(transactions.size() - 1);
                            }
                        } else {
                            String message = task.getException().getMessage();
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request permission if needed before calling createDocxFile
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // Permission not granted, request it
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    } else {
                        // Permission already granted, proceed with creating the DOCX file
                        createDocxFile(transactions);
                    }
                } else {
                    // For devices running below Android 6.0, permission is granted at installation time
                    createDocxFile(transactions);
                }
            }

        });

    }

    // iterate through the list of transactions and create a DOCX file
    private void createDocxFile(List<Transaction> transactions) {
        try {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph paragraph = doc.createParagraph();

            XWPFRun run = paragraph.createRun();
            run.setText("Transaction List");
            run.setBold(true);
            run.setFontSize(14);

            for (Transaction transaction : transactions) {
                String date = formatDate(transaction.getDate());
                String sender = transaction.getSender().getId();
                String receiver = transaction.getReceiver().getId();
                String money = String.valueOf(transaction.getAmount());

                // Add transaction details to the document
                XWPFParagraph transactionParagraph = doc.createParagraph();
                XWPFRun transactionRun = transactionParagraph.createRun();
                transactionRun.setText("Date: " + date);
                transactionRun.addBreak();
                transactionRun.setText("Sender: " + sender);
                transactionRun.addBreak();
                transactionRun.setText("Receiver: " + receiver);
                transactionRun.addBreak();
                transactionRun.setText("Money: " + money);
                transactionRun.addBreak();
            }

            if (isExternalStorageWritable()) {
                // Get the Downloads directory on the external storage
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs(); // Create the directory if it doesn't exist
                }

                // File path and name for the document
                File outputFile = new File(downloadsDir, "TransactionList.docx");

                // Save the document to the specified file path
                FileOutputStream out = new FileOutputStream(outputFile);
                doc.write(out);
                out.close();
            }
                // Show a toast message to indicate successful file creation
                Toast.makeText(requireContext(), "Transaction list downloaded as DOCX", Toast.LENGTH_SHORT).show();
            } catch(Exception e){
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error creating DOCX file", Toast.LENGTH_SHORT).show();
        }
    }


    private Context getApplicationContext() {
        return requireContext();
    }

    // Helper method to check if external storage is available and writable
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    private String formatDate(Date date) {
        return date.toString();
    }
}
