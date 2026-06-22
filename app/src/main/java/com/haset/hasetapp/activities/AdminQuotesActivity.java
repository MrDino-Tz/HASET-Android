package com.haset.hasetapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminQuotesActivity extends AppCompatActivity {

    private RecyclerView rvQuotes;
    private TextView tvEmpty;
    private MaterialButton btnAddQuote;
    private ImageView btnBack;
    private List<QuoteItem> quoteList = new ArrayList<>();
    private QuoteAdapter adapter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_quotes);

        initViews();
        loadQuotes();
    }

    private void initViews() {
        rvQuotes = findViewById(R.id.rvQuotes);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnAddQuote = findViewById(R.id.btnAddQuote);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnAddQuote.setOnClickListener(v -> showQuoteDialog(null, null, null));

        adapter = new QuoteAdapter(quoteList);
        rvQuotes.setLayoutManager(new LinearLayoutManager(this));
        rvQuotes.setAdapter(adapter);
    }

    private void loadQuotes() {
        progressDialog = ProgressDialog.show(this, "", "Loading quotes...");
        
        FirebaseHelper.getInstance().getDatabaseReference()
            .child("health_quotes")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    progressDialog.dismiss();
                    quoteList.clear();
                    if (snapshot.exists()) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String text = ds.child("text").getValue(String.class);
                            String author = ds.child("author").getValue(String.class);
                            if (text != null) {
                                quoteList.add(new QuoteItem(ds.getKey(), text, author));
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressDialog.dismiss();
                    Toast.makeText(AdminQuotesActivity.this, "Failed to load quotes", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateEmptyState() {
        if (quoteList.isEmpty()) {
            rvQuotes.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rvQuotes.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showQuoteDialog(String key, String currentText, String currentAuthor) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_quote, null);
        TextInputEditText etQuote = dialogView.findViewById(R.id.etQuoteText);
        TextInputEditText etAuthor = dialogView.findViewById(R.id.etQuoteAuthor);

        if (currentText != null) {
            etQuote.setText(currentText);
        }
        if (currentAuthor != null) {
            etAuthor.setText(currentAuthor);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(key == null ? "Add Quote" : "Edit Quote");
        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String quoteText = etQuote.getText() != null ? etQuote.getText().toString().trim() : "";
            String quoteAuthor = etAuthor.getText() != null ? etAuthor.getText().toString().trim() : "HASET Hospital";
            if (!quoteText.isEmpty()) {
                saveQuote(key, quoteText, quoteAuthor);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveQuote(String key, String text, String author) {
        if (key == null) {
            key = FirebaseHelper.getInstance().getDatabaseReference()
                .child("health_quotes").push().getKey();
        }
        
        if (key != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("health_quotes/" + key + "/text", text);
            updates.put("health_quotes/" + key + "/author", author);
            FirebaseHelper.getInstance().getDatabaseReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Quote saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save quote", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void deleteQuote(String key) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Quote")
            .setMessage("Are you sure you want to delete this quote?")
            .setPositiveButton("Delete", (dialog, which) -> {
                FirebaseHelper.getInstance().getDatabaseReference()
                    .child("health_quotes").child(key).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Quote deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private class QuoteItem {
        String key;
        String text;
        String author;
        
        QuoteItem(String key, String text, String author) {
            this.key = key;
            this.text = text;
            this.author = author != null ? author : "HASET Hospital";
        }
    }

    private class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder> {
        private List<QuoteItem> items;

        QuoteAdapter(List<QuoteItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public QuoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_quote, parent, false);
            return new QuoteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuoteViewHolder holder, int position) {
            QuoteItem item = items.get(position);
            holder.tvQuoteText.setText("\"" + item.text + "\"");
            holder.tvQuoteAuthor.setText("— " + item.author);
            holder.btnEdit.setOnClickListener(v -> showQuoteDialog(item.key, item.text, item.author));
            holder.btnDelete.setOnClickListener(v -> deleteQuote(item.key));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class QuoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuoteText, tvQuoteAuthor, btnEdit, btnDelete;

            QuoteViewHolder(View itemView) {
                super(itemView);
                tvQuoteText = itemView.findViewById(R.id.tvQuoteText);
                tvQuoteAuthor = itemView.findViewById(R.id.tvQuoteAuthor);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
