package com.haset.hasetapp.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.haset.hasetapp.R;
import com.haset.hasetapp.utils.CustomDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminSupportTicketsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_support_tickets);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new TicketsPagerAdapter());
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Open");
                    break;
                case 1:
                    tab.setText("Resolved");
                    break;
            }
        }).attach();
    }

    private class TicketsPagerAdapter extends FragmentStateAdapter {
        public TicketsPagerAdapter() {
            super(AdminSupportTicketsActivity.this);
        }

        @Override
        public int getItemCount() { return 2; }

        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            return SupportTicketsFragment.newInstance(position == 0 ? "open" : "resolved");
        }
    }

    public static class SupportTicketsFragment extends androidx.fragment.app.Fragment {
        private static final String ARG_STATUS = "status";
        private final List<Map<String, Object>> tickets = new ArrayList<>();
        private RecyclerView recyclerView;
        private View emptyView;
        private TicketsAdapter adapter;

        public static SupportTicketsFragment newInstance(String status) {
            SupportTicketsFragment fragment = new SupportTicketsFragment();
            Bundle args = new Bundle();
            args.putString(ARG_STATUS, status);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_support_tickets_list, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            recyclerView = view.findViewById(R.id.rvTickets);
            emptyView = view.findViewById(R.id.emptyView);
            
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new TicketsAdapter();
            recyclerView.setAdapter(adapter);

            loadTickets();
        }

        private void loadTickets() {
            String status = getArguments() != null ? getArguments().getString(ARG_STATUS) : "open";
            
            FirebaseDatabase.getInstance().getReference("support_tickets")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tickets.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Map<String, Object> ticket = new HashMap<>();
                            ticket.put("id", ds.getKey());
                            for (DataSnapshot child : ds.getChildren()) {
                                ticket.put(child.getKey(), child.getValue());
                            }
                            String ticketStatus = (String) ticket.get("status");
                            if (status.equals(ticketStatus)) {
                                tickets.add(ticket);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(tickets.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(requireContext(), R.string.error_loading, Toast.LENGTH_SHORT).show();
                    }
                });
        }

        class TicketsAdapter extends RecyclerView.Adapter<TicketsAdapter.TicketViewHolder> {
            @NonNull
            @Override
            public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new TicketViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_support_ticket, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
                Map<String, Object> ticket = tickets.get(position);
                holder.bind(ticket);
            }

            @Override
            public int getItemCount() { return tickets.size(); }

            class TicketViewHolder extends RecyclerView.ViewHolder {
                CircleImageView ivProfile;
                TextView tvUserName, tvTimestamp, tvReport, tvStatus;
                MaterialButton btnMarkResolved;

                TicketViewHolder(@NonNull View itemView) {
                    super(itemView);
                    ivProfile = itemView.findViewById(R.id.ivProfile);
                    tvUserName = itemView.findViewById(R.id.tvUserName);
                    tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                    tvReport = itemView.findViewById(R.id.tvReport);
                    tvStatus = itemView.findViewById(R.id.tvStatus);
                    btnMarkResolved = itemView.findViewById(R.id.btnMarkResolved);
                }

                void bind(Map<String, Object> ticket) {
                    tvUserName.setText(ticket.get("userName") != null ? 
                        ticket.get("userName").toString() : "Unknown User");
                    tvReport.setText(ticket.get("report") != null ? 
                        ticket.get("report").toString() : "");
                    
                    Object timestamp = ticket.get("timestamp");
                    if (timestamp != null) {
                        long ts = timestamp instanceof Long ? (Long) timestamp : Long.parseLong(timestamp.toString());
                        tvTimestamp.setText(getTimeAgo(ts));
                    }

                    String status = ticket.get("status") != null ? ticket.get("status").toString() : "open";
                    tvStatus.setText(status.toUpperCase());
                    
                    if ("resolved".equals(status)) {
                        btnMarkResolved.setVisibility(View.GONE);
                        tvStatus.setBackgroundColor(itemView.getResources().getColor(R.color.status_approved, null));
                    } else {
                        btnMarkResolved.setVisibility(View.VISIBLE);
                        tvStatus.setBackgroundColor(itemView.getResources().getColor(R.color.status_pending, null));
                    }

                    btnMarkResolved.setOnClickListener(v -> markResolved(ticket));
                }

                private void markResolved(Map<String, Object> ticket) {
                    new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Mark as Resolved")
                        .setMessage("Are you sure you want to mark this ticket as resolved?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String id = (String) ticket.get("id");
                            FirebaseDatabase.getInstance().getReference("support_tickets")
                                .child(id)
                                .child("status")
                                .setValue("resolved");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }

                private String getTimeAgo(long timestamp) {
                    long now = System.currentTimeMillis();
                    long diff = now - timestamp;
                    
                    if (diff < 60000) return "Just now";
                    if (diff < 3600000) return (diff / 60000) + " minutes ago";
                    if (diff < 86400000) return (diff / 3600000) + " hours ago";
                    if (diff < 604800000) return (diff / 86400000) + " days ago";
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    return sdf.format(new Date(timestamp));
                }
            }
        }
    }
}
