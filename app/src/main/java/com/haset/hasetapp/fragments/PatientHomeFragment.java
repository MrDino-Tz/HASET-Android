package com.haset.hasetapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.haset.hasetapp.R;
import com.haset.hasetapp.activities.ArticleDetailActivity;
import com.haset.hasetapp.activities.DashboardActivity;
import com.haset.hasetapp.activities.DoctorsActivity;
import com.haset.hasetapp.activities.NotificationActivity;
import com.haset.hasetapp.activities.SearchActivity;
import com.haset.hasetapp.activities.EditProfileActivity;
import com.haset.hasetapp.adapters.CategoryAdapter;
import com.haset.hasetapp.adapters.PatientBannerAdapter;
import com.haset.hasetapp.models.Doctor;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.haset.hasetapp.utils.FirebaseHelper;
import com.haset.hasetapp.utils.MemoryMonitor;
import com.haset.hasetapp.utils.NetworkUtils;
import com.haset.hasetapp.utils.NotificationBadgeHelper;
import com.haset.hasetapp.utils.PerformanceMonitor;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.ProfilePhotoHelper;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;
import android.util.Log;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.HomeViewModel;

/**
 * Main Home Fragment for Patients.
 * <p>
 * <b>Memory Management:</b>
 * This fragment manages multiple heavy resources (ViewPager, RecyclerViews, Auto-scroll Runnables).
 * It implements strict cleanup in {@link #onDestroyView()} to:
 * <ul>
 *   <li>Stop auto-scroll handlers immediately.</li>
 *   <li>Null out all View references to allow Garbage Collection.</li>
 *   <li>Remove network callbacks to prevent leaking the Fragment instance.</li>
 * </ul>
 */
public class PatientHomeFragment extends Fragment {
    private TextView tvUserName;
    private TextView etSearch;
    private ImageView ivNotification;
    private TextView tvNotificationBadge;
    private ImageView ivMessage;
    private TextView tvMessageBadge;
    private CircleImageView ivProfile;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerProfile;
    private List<Doctor> allDoctors;
    private PreferenceManager preferenceManager;
    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private LinearLayout moreSettingsLayout;
    private NetworkUtils.NetworkCallback networkCallback;
    private ViewPager2 viewPagerBanner;
    private LinearLayout layoutPaginationIndicators;
    private PatientBannerAdapter bannerAdapter;
    private List<PatientBannerAdapter.BannerItem> bannersList = new ArrayList<>();
    private android.os.Handler autoScrollHandler;
    private Runnable autoScrollRunnable;

    // New UI members
    private TextView tvUserInitials;
    private TextView tvUserNameNew;
    private ImageView ivSearchIconNew, ivNotificationNew;
    private LinearLayout llChatDoctor, llMenstruation, llBuyMedicine, llArticlesAction, llHospitals;
    private RecyclerView rvMedicineNew, rvPopularArticles;
    private TextView tvViewAllMedicine, tvViewAllArticles;
    private android.widget.LinearLayout shimmerPopularArticles;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerPageLoading;
    private android.widget.LinearLayout layoutHomeContent;
    private com.haset.hasetapp.adapters.PopularArticleAdapter popularArticleAdapter;
    private HomeViewModel viewModel;

    // Header Profile Components
    private ImageView ivProfileHeader;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerProfileHeader;
    private android.view.View profileImageContainer;
    
    // Scroll to Top & Quick Access
    private android.widget.FrameLayout fabScrollTop;
    private NestedScrollView nestedScrollView;
    
    // Quick Access Row Components
    private LinearLayout layoutChatDoctor, layoutNews, layoutChildren;
    
    // Health Quote Components
    private LinearLayout layoutHealthQuote;
    private TextView tvHealthQuote, tvHealthQuoteAuthor;
    private List<String> healthQuotes = new ArrayList<>();
    private android.os.Handler quoteHandler;
    private Runnable quoteRunnable;
    private ValueEventListener quotesListener;
    private int previousQuotesSize = 0;
    private int currentQuoteIndex = 0;
    
    // M3 Search Components
    private com.google.android.material.search.SearchBar searchBarHome;
    private com.google.android.material.search.SearchView searchViewHome;
    private RecyclerView rvSearchResultsHome;
    private com.haset.hasetapp.adapters.SearchResultAdapter searchResultAdapter;
    private List<com.haset.hasetapp.database.entities.ArticlePostEntity> fullArticleList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferenceManager = new PreferenceManager(requireContext());

        initViews(view);
        setupCategoriesRecyclerView();
        setupBanner();

        // Set username in the proper TextView
        String userName = preferenceManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
            tvUserNameNew.setText(userName);
            if (tvUserInitials != null) tvUserInitials.setText(com.haset.hasetapp.utils.ProfilePhotoHelper.getInitials(userName));
        }


        setupOnClickListeners();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        setupObservers();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Start performance monitoring
        PerformanceMonitor.startMonitoring();
        
        // Initialize network monitoring
        initializeNetworkMonitoring();
        
        // Restart auto-scroll when fragment becomes visible
        if (bannersList != null && bannersList.size() > 1) {
            startAutoScroll(bannersList.size());
        }
        
        // Update message badge
        updateMessageBadge();
        
        // Refresh header profile photo
        refreshHeaderProfile();
        
        MemoryMonitor.logMemoryUsage("PatientHome_onResume");
    }
    
    private void setupSearchFunctionality() {
        if (searchViewHome == null) return;

        rvSearchResultsHome = searchViewHome.findViewById(R.id.rvSearchResultsHome);
        if (rvSearchResultsHome != null) {
            rvSearchResultsHome.setLayoutManager(new LinearLayoutManager(requireContext()));
            searchResultAdapter = new com.haset.hasetapp.adapters.SearchResultAdapter(new com.haset.hasetapp.adapters.SearchResultAdapter.OnItemClickListener() {
                @Override
                public void onDoctorClick(com.haset.hasetapp.models.Doctor doctor) {
                    com.haset.hasetapp.fragments.DoctorDetailsBottomSheet bottomSheet = com.haset.hasetapp.fragments.DoctorDetailsBottomSheet.newInstance(doctor);
                    bottomSheet.show(getParentFragmentManager(), "doctor_details");
                }

                @Override
                public void onArticleClick(com.haset.hasetapp.models.Article article) {
                    Gson gson = new Gson();
                    com.haset.hasetapp.database.entities.ArticlePostEntity postEntity = new com.haset.hasetapp.database.entities.ArticlePostEntity(
                        article.getArticleId() != null ? article.getArticleId() : "",
                        "text",
                        article.getTitle(),
                        article.getDescription(),
                        article.getAuthorName() != null ? article.getAuthorName() : "HASET",
                        article.getTags(),
                        "published"
                    );
                    Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.ArticleDetailActivity.class);
                    intent.putExtra(com.haset.hasetapp.activities.ArticleDetailActivity.EXTRA_ARTICLE, gson.toJson(postEntity));
                    startActivity(intent);
                }
                @Override
                public void onServiceClick(com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem service) {
                    if (searchViewHome != null) searchViewHome.hide();
                    switch (service.actionId) {
                        case "chat":
                            startActivity(new Intent(requireContext(), DoctorsActivity.class));
                            break;
                        case "menstruation":
                            showComingSoonDialog(getString(R.string.menstruation_tracker));
                            break;
                        case "medicine":
                            Toast.makeText(requireContext(), "Pharmacy module coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case "articles":
                            startActivity(new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class));
                            break;
                        case "hospitals":
                            startActivity(new Intent(requireContext(), com.haset.hasetapp.activities.HospitalsActivity.class));
                            break;
                    }
                }
            });
            rvSearchResultsHome.setAdapter(searchResultAdapter);
        }

        searchViewHome.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSearch(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        searchViewHome.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            filterSearch(v.getText().toString());
            return false;
        });
    }

    private void filterSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (searchResultAdapter != null) searchResultAdapter.setResults(new ArrayList<>());
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<Object> filteredResults = new ArrayList<>();

        // Search App Services
        List<com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem> availableServices = java.util.Arrays.asList(
            new com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem("Chat with Doctor", R.drawable.user_md_24, "chat"),
            new com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem("Menstruation Tracker", R.drawable.ic_medical, "menstruation"),
            new com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem("Buy Medicine", R.drawable.ic_medical, "medicine"),
            new com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem("Health Articles", R.drawable.ic_news_paper, "articles"),
            new com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem("Find Hospital", R.drawable.ic_hospital_24, "hospitals")
        );
        for (com.haset.hasetapp.adapters.SearchResultAdapter.ServiceItem service : availableServices) {
            if (service.name.toLowerCase().contains(lowerQuery)) {
                filteredResults.add(service);
            }
        }

        // Search Doctors
        if (allDoctors != null) {
            for (com.haset.hasetapp.models.Doctor doctor : allDoctors) {
                if (doctor.getFullName() != null && doctor.getFullName().toLowerCase().contains(lowerQuery)) {
                    filteredResults.add(doctor);
                } else if (doctor.getSpecialty() != null && doctor.getSpecialty().toLowerCase().contains(lowerQuery)) {
                    filteredResults.add(doctor);
                }
            }
        }

        // Search Articles
        if (fullArticleList != null) {
            for (com.haset.hasetapp.database.entities.ArticlePostEntity article : fullArticleList) {
                boolean matchesTitle = article.getTitle() != null && article.getTitle().toLowerCase().contains(lowerQuery);
                boolean matchesDesc = article.getDescription() != null && article.getDescription().toLowerCase().contains(lowerQuery);
                boolean matchesTags = article.getTags() != null && article.getTags().toLowerCase().contains(lowerQuery);
                boolean matchesAuthor = article.getProfileName() != null && article.getProfileName().toLowerCase().contains(lowerQuery);
                
                if (matchesTitle || matchesDesc || matchesTags || matchesAuthor) {
                    filteredResults.add(article);
                }
            }
        }

        if (searchResultAdapter != null) {
            searchResultAdapter.setResults(filteredResults);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop performance monitoring
        PerformanceMonitor.stopMonitoring();
        
        // Stop auto-scroll when fragment is not visible
        stopAutoScroll();
        
        MemoryMonitor.logMemoryUsage("PatientHome_onPause");
    }
    
    /**
     * Update the message badge with current unread count
     */
    private void updateMessageBadge() {
        if (tvMessageBadge == null || !isAdded()) {
            return;
        }
        NotificationBadgeHelper badgeHelper = new NotificationBadgeHelper(requireContext());
        int unreadCount = badgeHelper.getTotalUnreadCount();
        NotificationBadgeHelper.updateMessageBadge(tvMessageBadge, unreadCount);
        Log.d("PatientHomeFragment", "Message badge updated with count: " + unreadCount);
    }
    
    /**
     * Public method to refresh message badge (called from DashboardActivity)
     */
    public void refreshMessageBadge() {
        updateMessageBadge();
    }
    
    private void initializeNetworkMonitoring() {
        networkCallback = new NetworkUtils.NetworkCallback() {
            @Override
            public void onNetworkAvailable() {
                // Network is available, refresh data
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // In MVVM, we can just trigger a reload in the ViewModel if needed
                        // or let the existing observers handle it.
                        updateMessageBadge(); 
                    });
                }
            }
            
            @Override
            public void onNetworkLost() {
                // Network is lost, show message to user
                if (isAdded()) {
                    android.view.View view = getView();
                    if (view != null) {
                        try {
                            com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar.make(
                                view, R.string.network_lost, com.google.android.material.snackbar.Snackbar.LENGTH_LONG);
                            snackbar.setBackgroundTint(getResources().getColor(R.color.colorError));
                            snackbar.show();
                        } catch (Exception e) {
                            // Backup toast if SnackBar fails for any reason
                            android.app.Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> Toast.makeText(activity, R.string.network_lost, Toast.LENGTH_SHORT).show());
                            }
                        }
                    }
                }
            }
        };
        
        NetworkUtils.addNetworkCallback(requireContext(), networkCallback);
    }

    private void setupOnClickListeners() {
        ivNotification.setOnClickListener(v->{
            if (!isAdded()) return;
            Context context = getContext();
            if (context == null) return;
            
            // Clear notification badge when opening notifications
            if (viewModel != null) {
                viewModel.clearNotificationCount();
            }
            
            Intent intent = new Intent(context, NotificationActivity.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // Get the center coordinates of the clicked view
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                int centerX = location[0] + v.getWidth() / 2;
                int centerY = location[1] + v.getHeight() / 2;
                
                android.app.ActivityOptions options = android.app.ActivityOptions.makeClipRevealAnimation(
                    v, centerX, centerY, 0, 0);
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
        });
        
        // Set up click listener for the message icon to open ChatListFragment via Dashboard
        ivMessage.setOnClickListener(v -> {
            DashboardActivity dashboardActivity = (getActivity() instanceof DashboardActivity)
                    ? (DashboardActivity) getActivity() : null;
            if (dashboardActivity != null) {
                dashboardActivity.getBottomNavigation().setSelectedItemId(R.id.nav_chat);
            }
        });
        
        // Update message badge will be done in updateNotificationBadge()
        
        // Setup profile click listener
        ivProfile.setOnClickListener(v -> navigateToProfile());

        // Setup search listener
        etSearch.setOnClickListener(v -> {
            // Navigate to search activity
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && getActivity() != null) {
                // Get the center coordinates of the clicked view
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                int centerX = location[0] + v.getWidth() / 2;
                int centerY = location[1] + v.getHeight() / 2;
                
                android.app.ActivityOptions options = android.app.ActivityOptions.makeClipRevealAnimation(
                    v, centerX, centerY, 0, 0);
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
        });

        // New search icon listener - triggers M3 expansion
        if (ivSearchIconNew != null) {
            ivSearchIconNew.setOnClickListener(v -> {
                if (searchViewHome != null) {
                    searchViewHome.show();
                } else {
                    // Fallback to old search activity if SearchView initialization failed
                    Intent intent = new Intent(requireContext(), SearchActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (ivNotificationNew != null) {
            ivNotificationNew.setOnClickListener(v -> {
                // Clear notification badge when opening notifications
                if (viewModel != null) {
                    viewModel.clearNotificationCount();
                }
                
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.NotificationActivity.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && getActivity() != null) {
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int centerX = location[0] + v.getWidth() / 2;
                    int centerY = location[1] + v.getHeight() / 2;
                    
                    android.app.ActivityOptions options = android.app.ActivityOptions.makeClipRevealAnimation(
                        v, centerX, centerY, 0, 0);
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            });
        }

        // New action listeners
        if (llChatDoctor != null) {
            llChatDoctor.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), DoctorsActivity.class);
                startActivity(intent);
            });
        }

        if (llMenstruation != null) {
            llMenstruation.setOnClickListener(v -> showComingSoonDialog(getString(R.string.menstruation_tracker)));
        }



        if (llBuyMedicine != null) {
            llBuyMedicine.setOnClickListener(v -> {
                // DISABLED FOR V1 - PHARMACY COMING IN VERSION 2.0
                showComingSoonDialog(getString(R.string.pharmacy));
            });
        }

        if (llArticlesAction != null) {
            llArticlesAction.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class);
                startActivity(intent);
            });
        }
        
        if (llHospitals != null) {
            llHospitals.setOnClickListener(v -> {
                new HospitalsLocationBottomSheet().show(getChildFragmentManager(), HospitalsLocationBottomSheet.TAG);
            });
        }

        if (tvViewAllMedicine != null) {
            tvViewAllMedicine.setOnClickListener(v -> {
                // DISABLED FOR V1 - PHARMACY COMING IN VERSION 2.0
                showComingSoonDialog(getString(R.string.pharmacy));
            });
        }
        if (tvViewAllArticles != null) {
            tvViewAllArticles.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class);
                startActivity(intent);
            });
        }
        
        // Quick access row click listeners
        if (layoutChatDoctor != null) {
            layoutChatDoctor.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.DoctorsActivity.class);
                startActivity(intent);
            });
        }
        
        if (layoutNews != null) {
            layoutNews.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class);
                startActivity(intent);
            });
        }
        
        if (layoutChildren != null) {
            layoutChildren.setOnClickListener(v -> {
                showComingSoonDialog(getString(R.string.childrens));
            });
        }
    }
    
    private void setupScrollToTop() {
        if (fabScrollTop != null && nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > 300) {
                    fabScrollTop.setVisibility(View.VISIBLE);
                } else {
                    fabScrollTop.setVisibility(View.GONE);
                }
            });
            
            fabScrollTop.setOnClickListener(v -> {
                if (nestedScrollView != null) {
                    nestedScrollView.smoothScrollTo(0, 0);
                }
            });
        }
    }
    
    private void hidePageShimmer() {
        if (shimmerPageLoading != null && shimmerPageLoading.getVisibility() == View.VISIBLE) {
            shimmerPageLoading.stopShimmer();
            shimmerPageLoading.setVisibility(View.GONE);
            if (layoutHomeContent != null) {
                layoutHomeContent.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void loadHealthQuotes() {
        healthQuotes.clear();
        addDefaultQuotes();
        seedDefaultQuotesIfNeeded();
        
        quotesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    healthQuotes.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String quote = ds.child("text").getValue(String.class);
                        if (quote != null && !quote.isEmpty()) {
                            healthQuotes.add(quote);
                        }
                    }
                }
                if (healthQuotes.isEmpty()) {
                    addDefaultQuotes();
                }
                
                boolean isNewQuote = healthQuotes.size() > previousQuotesSize;
                previousQuotesSize = healthQuotes.size();
                
                if (isNewQuote) {
                    currentQuoteIndex = healthQuotes.size() - 1;
                } else if (currentQuoteIndex >= healthQuotes.size()) {
                    currentQuoteIndex = 0;
                }
                
                startQuoteRotation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (healthQuotes.isEmpty()) {
                    addDefaultQuotes();
                }
                startQuoteRotation();
            }
        };
        
        FirebaseHelper.getInstance().getDatabaseReference()
            .child("health_quotes")
            .addValueEventListener(quotesListener);
    }
    
    private void addDefaultQuotes() {
        healthQuotes.add("Afya ni mali yenye thamani kuliko fedha zote.");
        healthQuotes.add("Kula vizuri,ishi vizuri.");
        healthQuotes.add("Mazoezi ya kila siku huwa weka mwili strong.");
        healthQuotes.add("Maji ni muhimu kwa afya yako kila siku.");
        healthQuotes.add("Pumzika vya kutosha kwa mwili wa afya.");
        healthQuotes.add("Usisahau kula matunda na mbogamboga kila siku.");
        healthQuotes.add("Kutembea kila siku kunasaidia mwili wako.");
        healthQuotes.add("Usingizii wa kutosha ni muhimu kwa afya bora.");
    }
    
    private void seedDefaultQuotesIfNeeded() {
        FirebaseHelper.getInstance().getDatabaseReference()
            .child("health_quotes")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                        Map<String, Object> defaultQuotes = new HashMap<>();
                        defaultQuotes.put("quote_1/text", "Afya ni mali yenye thamani kuliko fedha zote.");
                        defaultQuotes.put("quote_1/author", "HASET Hospital");
                        defaultQuotes.put("quote_2/text", "Kula vizuri,ishi vizuri.");
                        defaultQuotes.put("quote_2/author", "HASET Hospital");
                        defaultQuotes.put("quote_3/text", "Mazoezi ya kila siku huwa weka mwili strong.");
                        defaultQuotes.put("quote_3/author", "HASET Hospital");
                        defaultQuotes.put("quote_4/text", "Maji ni muhimu kwa afya yako kila siku.");
                        defaultQuotes.put("quote_4/author", "HASET Hospital");
                        defaultQuotes.put("quote_5/text", "Pumzika vya kutosha kwa mwili wa afya.");
                        defaultQuotes.put("quote_5/author", "HASET Hospital");
                        
                        FirebaseHelper.getInstance().getDatabaseReference()
                            .child("health_quotes")
                            .updateChildren(defaultQuotes);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
    }
    
    private void startQuoteRotation() {
        if (healthQuotes.isEmpty() || !isAdded()) return;
        
        stopQuoteRotation();
        
        quoteHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        quoteRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || layoutHealthQuote == null) return;
                
                Animation fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!isAdded() || tvHealthQuote == null || tvHealthQuoteAuthor == null) return;
                        
                        currentQuoteIndex = (currentQuoteIndex + 1) % healthQuotes.size();
                        tvHealthQuote.setText("\"" + healthQuotes.get(currentQuoteIndex) + "\"");
                        tvHealthQuoteAuthor.setText("— HASET Hospital");
                        
                        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
                        layoutHealthQuote.startAnimation(fadeIn);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                layoutHealthQuote.startAnimation(fadeOut);
                
                quoteHandler.postDelayed(this, 10000);
            }
        };
        
        if (!healthQuotes.isEmpty()) {
            tvHealthQuote.setText("\"" + healthQuotes.get(0) + "\"");
            tvHealthQuoteAuthor.setText("— HASET Hospital");
        }
        
        Animation fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in);
        layoutHealthQuote.startAnimation(fadeIn);
        
        quoteHandler.postDelayed(quoteRunnable, 10000);
    }
    
    private void stopQuoteRotation() {
        if (quoteHandler != null && quoteRunnable != null) {
            quoteHandler.removeCallbacks(quoteRunnable);
        }
        if (quotesListener != null) {
            FirebaseHelper.getInstance().getDatabaseReference()
                .child("health_quotes")
                .removeEventListener(quotesListener);
        }
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        etSearch = view.findViewById(R.id.etSearch);
        ivNotification = view.findViewById(R.id.ivNotification);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        ivProfile = view.findViewById(R.id.ivProfile);
        shimmerProfile = view.findViewById(R.id.shimmerProfile);
        rvCategories = view.findViewById(R.id.rvCategories); // Ensure it's initialized here
        
        // Initialize message icon and badge
        ivMessage = view.findViewById(R.id.ivMessage);
        tvMessageBadge = view.findViewById(R.id.tvBadgeCount);
        
        // Initialize banner ViewPager2 and pagination indicators
        viewPagerBanner = view.findViewById(R.id.viewPagerBanner);
        layoutPaginationIndicators = view.findViewById(R.id.layoutPaginationIndicators);

        // Initialize new UI components
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        tvUserNameNew = view.findViewById(R.id.tvUserNameNew);
        ivSearchIconNew = view.findViewById(R.id.ivSearchIconNew);
        ivNotificationNew = view.findViewById(R.id.ivNotificationNew);
        llChatDoctor = view.findViewById(R.id.llChatDoctor);
        llMenstruation = view.findViewById(R.id.llMenstruation);
//        llBuyMedicine = view.findViewById(R.id.llBuyMedicine);
        llArticlesAction = view.findViewById(R.id.llArticlesAction);
        llHospitals = view.findViewById(R.id.llHospitals);
//        rvMedicineNew = view.findViewById(R.id.rvMedicineNew);
//        tvViewAllMedicine = view.findViewById(R.id.tvViewAllMedicine);
        rvPopularArticles = view.findViewById(R.id.rvPopularArticles);
        tvViewAllArticles = view.findViewById(R.id.tvViewAllArticles);
        shimmerPopularArticles = view.findViewById(R.id.shimmerPopularArticles);
        shimmerPageLoading = view.findViewById(R.id.shimmerPageLoading);
        layoutHomeContent = view.findViewById(R.id.layoutHomeContent);
        
        // Initial state: Shimmer ON, Content OFF
        if (shimmerPageLoading != null) shimmerPageLoading.setVisibility(View.VISIBLE);
        if (layoutHomeContent != null) layoutHomeContent.setVisibility(View.GONE);

        // Header Profile Initialization
        ivProfileHeader = view.findViewById(R.id.ivProfileHeader);
        shimmerProfileHeader = view.findViewById(R.id.shimmerProfileHeader);
        profileImageContainer = view.findViewById(R.id.profileImageContainer);
        
        if (profileImageContainer != null) {
            profileImageContainer.setOnClickListener(v -> navigateToProfile());
        }

        // Initialize scroll to top & quick access
        fabScrollTop = view.findViewById(R.id.fabScrollTop);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        
        // Initialize quick access row
        layoutChatDoctor = view.findViewById(R.id.layoutChatDoctor);
        layoutNews = view.findViewById(R.id.layoutNews);
        layoutChildren = view.findViewById(R.id.layoutChildren);
        
        // Initialize health quote views
        layoutHealthQuote = view.findViewById(R.id.layoutHealthQuote);
        tvHealthQuote = view.findViewById(R.id.tvHealthQuote);
        tvHealthQuoteAuthor = view.findViewById(R.id.tvHealthQuoteAuthor);
        
        // Initialize M3 Search Components
        searchViewHome = view.findViewById(R.id.searchViewHome);
        searchBarHome = view.findViewById(R.id.searchBarHome);
        rvSearchResultsHome = view.findViewById(R.id.rvSearchResultsHome);
        
        if (searchViewHome != null) {
            if (searchBarHome != null) {
                searchViewHome.setupWithSearchBar(searchBarHome);
            }
            searchViewHome.getEditText().setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
            setupSearchFunctionality();
        }
        
        setupNewRecyclerViews();
        setupScrollToTop();
        loadHealthQuotes();
    }


    private void setupObservers() {
        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        
        // Show page shimmer initially, hide when data loads
        if (shimmerPageLoading != null) {
            shimmerPageLoading.setVisibility(View.VISIBLE);
            shimmerPageLoading.startShimmer();
        }
        if (layoutHomeContent != null) {
            layoutHomeContent.setVisibility(View.GONE);
        }

        // Observe Doctors
        viewModel.getDoctors().observe(getViewLifecycleOwner(), doctors -> {
            if (doctors != null) {
                allDoctors = doctors;
                updateDoctorsUI(doctors);
            }
        });

        // Observe Notifications
        viewModel.getNotificationCount(userId, role).observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });

        // Observe Banners
        viewModel.getBanners().observe(getViewLifecycleOwner(), remoteBanners -> {
            if (remoteBanners != null && !remoteBanners.isEmpty()) {
                bannersList.clear();
                bannersList.addAll(remoteBanners);
                if (bannerAdapter != null) {
                    bannerAdapter.notifyDataSetChanged();
                }
                updatePaginationIndicators(0, bannersList.size());
                hidePageShimmer();
            } else if (remoteBanners != null) {
                // Use defaults if empty
                setupDefaultBanners(bannersList);
                if (bannerAdapter != null) {
                    bannerAdapter.notifyDataSetChanged();
                }
                updatePaginationIndicators(0, bannersList.size());
                hidePageShimmer();
            }
        });

        // Observe Popular Articles
        viewModel.getPopularArticles().observe(getViewLifecycleOwner(), articles -> {
            android.util.Log.d("PatientHomeFragment", "Articles received: " + (articles != null ? articles.size() : "null"));
            hidePopularArticlesShimmer();
            if (articles != null && !articles.isEmpty()) {
                fullArticleList = articles;
                if (popularArticleAdapter != null) {
                    android.util.Log.d("PatientHomeFragment", "Setting articles to adapter");
                    popularArticleAdapter.setArticles(articles);
                }
                if (rvPopularArticles != null) {
                    rvPopularArticles.setVisibility(View.VISIBLE);
                    android.util.Log.d("PatientHomeFragment", "RecyclerView now visible");
                }
            } else {
                android.util.Log.d("PatientHomeFragment", "No articles from Firebase, showing sample");
                
                // Add sample articles for testing
                if (popularArticleAdapter != null) {
                    java.util.List<com.haset.hasetapp.database.entities.ArticlePostEntity> sampleArticles = new java.util.ArrayList<>();
                    com.haset.hasetapp.database.entities.ArticlePostEntity sample1 = new com.haset.hasetapp.database.entities.ArticlePostEntity();
                    sample1.setPostId("sample1");
                    sample1.setTitle("Afya ya Kila Siku - Maji ya Kutosha");
                    sample1.setDescription("Kunywa maji ya kutosha kila siku ni muhimu kwa afya yako.");
                    sample1.setViews(1250);
                    sampleArticles.add(sample1);
                    
                    com.haset.hasetapp.database.entities.ArticlePostEntity sample2 = new com.haset.hasetapp.database.entities.ArticlePostEntity();
                    sample2.setPostId("sample2");
                    sample2.setTitle("Mazoezi ya Asubuhi - Faida Zake");
                    sample2.setDescription("Mazoezi ya asubuhi husaidia kupata nishati na kujisikia vizuri.");
                    sample2.setViews(980);
                    sampleArticles.add(sample2);
                    
                    popularArticleAdapter.setArticles(sampleArticles);
                    if (rvPopularArticles != null) {
                        rvPopularArticles.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        
        // Show shimmer initially and hide after 5 seconds as fallback
        showPopularArticlesShimmer();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            hidePopularArticlesShimmer();
        }, 5000);

        // Observe Featured Medicines
        viewModel.getFeaturedMedicines().observe(getViewLifecycleOwner(), medicines -> {
            if (medicines != null && rvMedicineNew != null) {
                updateMedicineUI(medicines);
            }
        });

        // Initial update for message badge
        updateMessageBadge();
    }


    /**
     * Sets up the RecyclerView for categories.
     */
    private void setupCategoriesRecyclerView() {
        if (rvCategories == null) return;

        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        List<CategoryAdapter.Category> catList = new java.util.ArrayList<>();
        catList.add(new CategoryAdapter.Category(R.drawable.haset_logo, getString(R.string.all_articles)));
        catList.add(new CategoryAdapter.Category(R.drawable.ic_play_circle_filled, getString(R.string.afya_class)));
        catList.add(new CategoryAdapter.Category(R.drawable.ic_heart, getString(R.string.childrens)));
        
        // Log the size of the category list for debugging
        Log.d("Categories", "Category list size: " + catList.size());

        categoryAdapter = new CategoryAdapter(catList, cat -> {
            switch(cat.name) {
                case "Articles":
                    startActivity(new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class));
                    break;
                case "Pharmacy":
                    // DISABLED FOR V1 - PHARMACY COMING IN VERSION 2.0
                    showComingSoonDialog(getString(R.string.pharmacy));
                    break;
                case "Darasa la Afya":
                case "Afya Class":
                    showComingSoonDialog(getString(R.string.afya_class));
                    break;
                case "Watoto":
                case "Children's":
                    showComingSoonDialog(getString(R.string.childrens));
                    break;
            }
        });
        rvCategories.setAdapter(categoryAdapter);
    }

    private void showComingSoonDialog(String featureName) {
        if (!isAdded() || getContext() == null) return;
        
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_coming_soon);
        
        // Transparent background for the dialog window itself
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            // Set width to 90% of screen
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        com.google.android.material.button.MaterialButton btnOk = dialog.findViewById(R.id.btnOk);

        if (tvTitle != null) tvTitle.setText(getString(R.string.feature_coming_soon, featureName));
        
        btnOk.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void setupBanner() {
        if (viewPagerBanner == null || layoutPaginationIndicators == null || !isAdded()) {
            return;
        }

        // Initialize with defaults initially
        if (bannersList.isEmpty()) {
            setupDefaultBanners(bannersList);
        }

        // Initialize adapter
        bannerAdapter = new PatientBannerAdapter(bannersList, banner -> {
            Intent intent = null;
            switch (banner.bannerType) {
                case PHARMACY:
                    showComingSoonDialog(getString(R.string.pharmacy));
                    break;
                case MESSAGING:
                    DashboardActivity da = (getActivity() instanceof DashboardActivity)
                            ? (DashboardActivity) getActivity() : null;
                    if (da != null) {
                        da.getBottomNavigation().setSelectedItemId(R.id.nav_chat);
                    }
                    break;
                case APPOINTMENT:
                case DOCTORS:
                    intent = new Intent(requireContext(), DoctorsActivity.class);
                    break;
                case ARTICLE:
                    intent = new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class);
                    break;
                case IMAGE_BANNER:
                    break;
            }
            if (intent != null) {
                startActivity(intent);
            }
        });

        viewPagerBanner.setAdapter(bannerAdapter);

        // Add Premium Page Transformer (Zoom-out / Parallax effect)
        viewPagerBanner.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1.0f - absPos * 0.3f);
            page.setScaleY(0.85f + (1 - absPos) * 0.15f);
            
            // Subtle Parallax for the internal image
            View image = page.findViewById(R.id.ivBannerImage);
            if (image != null) {
                image.setTranslationX(-position * (page.getWidth() / 2f));
            }
        });

        // Setup pagination indicators
        updatePaginationIndicators(0, bannersList.size());

        // Listen to page changes
        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePaginationIndicators(position, bannersList.size());
            }
        });

        // Setup auto-scroll
        startAutoScroll(bannersList.size());
    }

    private void updatePaginationIndicators(int currentPosition, int totalBanners) {
        if (layoutPaginationIndicators == null || !isAdded() || getContext() == null) {
            return;
        }

        layoutPaginationIndicators.removeAllViews();

        for (int i = 0; i < totalBanners; i++) {
            View indicator = new View(getContext());
            int size = 8; // dp
            int margin = 4; // dp
            float density = getContext().getResources().getDisplayMetrics().density;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (size * density),
                    (int) (size * density)
            );
            if (i < totalBanners - 1) {
                params.setMargins(0, 0, (int) (margin * density), 0);
            }
            indicator.setLayoutParams(params);

            if (i == currentPosition) {
                indicator.setBackgroundResource(R.drawable.bg_pagination_indicator_active);
            } else {
                indicator.setBackgroundResource(R.drawable.bg_pagination_indicator_inactive);
            }

            layoutPaginationIndicators.addView(indicator);
        }
    }

    private void setupDefaultBanners(List<PatientBannerAdapter.BannerItem> banners) {
        banners.clear();
        // Pharmacy banner
        banners.add(new PatientBannerAdapter.BannerItem(
                "Up to",
                "50% OFF",
                "Flash Sale",
                "Shop Now",
                R.drawable.placeholder_image,
                PatientBannerAdapter.BannerItem.BannerType.PHARMACY
        ));

        // Messaging banner
        banners.add(new PatientBannerAdapter.BannerItem(
                "Online",
                "Consultation",
                "Live Now",
                "Chat Now",
                R.drawable.doctor_2785482,
                PatientBannerAdapter.BannerItem.BannerType.MESSAGING
        ));

        // Appointment banner
        banners.add(new PatientBannerAdapter.BannerItem(
                "Book Expert",
                "Care Today",
                "Verified",
                "Book Now",
                R.drawable.three_doctors,
                PatientBannerAdapter.BannerItem.BannerType.APPOINTMENT
        ));

        // Pharmacy banner 2 (Home Care)
        banners.add(new PatientBannerAdapter.BannerItem(
                "Premium",
                "Home Care",
                "30% OFF",
                "Explore",
                R.drawable.placeholder_image,
                PatientBannerAdapter.BannerItem.BannerType.PHARMACY
        ));
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
            autoScrollHandler = null;
            autoScrollRunnable = null;
            Log.d("PatientHome", "Auto-scroll stopped and cleaned up");
            MemoryMonitor.logMemoryUsage("PatientHome_stopAutoScroll");
        }
    }

    private void startAutoScroll(int totalBanners) {
        if (viewPagerBanner == null || totalBanners <= 1 || !isAdded()) {
            return;
        }

        // Stop any existing auto-scroll
        stopAutoScroll();
        
        autoScrollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (viewPagerBanner != null && isAdded() && getContext() != null) {
                    int currentItem = viewPagerBanner.getCurrentItem();
                    int nextItem = (currentItem + 1) % totalBanners;
                    viewPagerBanner.setCurrentItem(nextItem, true);
                    
                    // Check memory usage periodically
                    MemoryMonitor.logMemoryUsageThrottled("PatientHome_AutoScroll");
                    
                    autoScrollHandler.postDelayed(this, 8000); // Auto-scroll every 8 seconds
                }
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 8000);
        Log.d("PatientHome", "Auto-scroll started for " + totalBanners + " banners");
    }

    private void setupNewRecyclerViews() {

        if (rvPopularArticles != null) {
            rvPopularArticles.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
            Gson gson = new Gson();
            popularArticleAdapter = new com.haset.hasetapp.adapters.PopularArticleAdapter(new ArrayList<>(), requireContext(), article -> {
                Intent intent = new Intent(requireContext(), com.haset.hasetapp.activities.ArticleActivity.class);
                intent.putExtra(com.haset.hasetapp.activities.ArticleActivity.EXTRA_ARTICLE_ID, article.getPostId());
                startActivity(intent);
            });
            rvPopularArticles.setAdapter(popularArticleAdapter);
        }

        if (rvMedicineNew != null) {
            rvMedicineNew.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        }
    }

    private void showPopularArticlesShimmer() {
        if (shimmerPopularArticles == null) return;
        shimmerPopularArticles.setVisibility(View.VISIBLE);
        com.haset.hasetapp.utils.ShimmerHelper.showListShimmer(
                requireContext(), shimmerPopularArticles, 4, R.layout.shimmer_item_article_list);
        if (rvPopularArticles != null) {
            rvPopularArticles.setVisibility(View.GONE);
        }
    }

    private void hidePopularArticlesShimmer() {
        if (shimmerPopularArticles == null) return;
        com.haset.hasetapp.utils.ShimmerHelper.hideListShimmer(shimmerPopularArticles);
        shimmerPopularArticles.setVisibility(View.GONE);
    }

    private void updateMedicineUI(List<com.haset.hasetapp.models.PharmacyProduct> products) {
        if (!isAdded() || products == null) return;
        
        // Limit to top 5
        List<com.haset.hasetapp.models.PharmacyProduct> displayProducts = new ArrayList<>();
        for (int i = 0; i < Math.min(5, products.size()); i++) {
            displayProducts.add(products.get(i));
        }

        rvMedicineNew.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_home_medicine, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                com.haset.hasetapp.models.PharmacyProduct product = displayProducts.get(position);
                TextView tvName = holder.itemView.findViewById(R.id.tvMedicineName);
                ImageView ivImage = holder.itemView.findViewById(R.id.ivMedicineImage);

                tvName.setText(product.getName());
                // Image loading logic can be added here
                
                holder.itemView.setOnClickListener(v -> {
                    // Navigate to pharmacy product details
                });
            }

            @Override
            public int getItemCount() {
                return displayProducts.size();
            }
        });
    }

    private void refreshHeaderProfile() {
        if (ivProfileHeader == null || preferenceManager == null) return;
        String userId = preferenceManager.getUserId();
        ProfilePhotoHelper.loadProfilePhoto(requireContext(), userId, ivProfileHeader, shimmerProfileHeader, tvUserInitials);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop auto-scroll when view is destroyed
        stopAutoScroll();
        
        // Remove network callback
        if (networkCallback != null) {
            NetworkUtils.removeNetworkCallback(requireContext(), networkCallback);
            networkCallback = null;
        }
        
        // Clear adapters
        if (rvCategories != null) rvCategories.setAdapter(null);
        if (rvMedicineNew != null) rvMedicineNew.setAdapter(null);
        if (rvPopularArticles != null) rvPopularArticles.setAdapter(null);
        if (viewPagerBanner != null) viewPagerBanner.setAdapter(null);
        
        categoryAdapter = null;
        bannerAdapter = null;
        popularArticleAdapter = null;
        
        // Null out view references
        tvUserName = null;
        tvUserInitials = null;
        tvUserNameNew = null;
        etSearch = null;
        ivNotification = null;
        tvNotificationBadge = null;
        ivMessage = null;
        tvMessageBadge = null;
        ivProfile = null;
        shimmerProfile = null;
        ivProfileHeader = null;
        shimmerProfileHeader = null;
        profileImageContainer = null;
        
        ivSearchIconNew = null;
        ivNotificationNew = null;
        
        llChatDoctor = null;
        llMenstruation = null;
        llBuyMedicine = null;
        llArticlesAction = null;
        
        rvCategories = null;
        rvMedicineNew = null;
        rvPopularArticles = null;
        shimmerPopularArticles = null;
        viewPagerBanner = null;
        layoutPaginationIndicators = null;
        
        tvViewAllMedicine = null;
        tvViewAllArticles = null;
        moreSettingsLayout = null;
        
        fabScrollTop = null;
        nestedScrollView = null;
        
        layoutChatDoctor = null;
        layoutNews = null;
        layoutChildren = null;
        
        layoutHealthQuote = null;
        tvHealthQuote = null;
        tvHealthQuoteAuthor = null;
        stopQuoteRotation();
    }

//    public static class SettingsBottomSheet extends BottomSheetDialogFragment {
//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            return inflater.inflate(R.layout.bottom_sheet_settings, container, false);
//        }
//    }
    
    private void navigateToProfile() {
        Intent intent = new Intent(requireContext(), EditProfileActivity.class);
        startActivity(intent);
    }
    
    private void updateDoctorsUI(List<com.haset.hasetapp.models.Doctor> doctors) {
        // Doctors are displayed via the HomeViewModel observers
        // This method can be used for additional UI updates if needed
    }
}
