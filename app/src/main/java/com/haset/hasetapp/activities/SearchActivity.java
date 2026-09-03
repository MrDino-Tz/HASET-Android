package com.haset.hasetapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.SearchResultAdapter;
import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.fragments.DoctorDetailsBottomSheet;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.models.PharmacyProduct;
import com.haset.hasetapp.repositories.ArticleRepository;
import com.haset.hasetapp.repositories.DoctorRepository;
import com.haset.hasetapp.repositories.PharmacyRepository;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity implements SearchResultAdapter.OnSearchResultClickListener {

    private ImageView btnBack;
    private TextInputEditText etSearch;
    private ProgressBar progressBar;
    private RecyclerView rvSearchResults;
    private View noResultsLayout;
    private TextView tvNoResultsSubtitle;
    
    private SearchResultAdapter searchAdapter;
    private LocalStorageHelper storageHelper;
    private List<Doctor> allDoctors;
    private List<com.haset.hasetapp.database.entities.ArticlePostEntity> allArticles;
    private List<PharmacyProduct> allDrugs;
    private List<SearchResultAdapter.SearchResult> searchResults;
    
    private PharmacyRepository pharmacyRepository;
    private DoctorRepository doctorRepository;
    private ArticleRepository articleRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        overridePendingTransition(R.anim.anim_slide_up, 0);

        initViews();
        setupRecyclerView();
        setupSearchListener();
        loadInitialData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        noResultsLayout = findViewById(R.id.noResultsLayout);
        tvNoResultsSubtitle = noResultsLayout.findViewById(R.id.tvNoResultsSubtitle);

        storageHelper = LocalStorageHelper.getInstance(this);
        pharmacyRepository = new PharmacyRepository();
        doctorRepository = new DoctorRepository();
        articleRepository = new ArticleRepository(getApplication());
        searchResults = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        searchAdapter = new SearchResultAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(searchAdapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadInitialData() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Load Doctors from Firebase via Repository
        doctorRepository.getAllDoctors().observe(this, doctors -> {
            allDoctors = doctors;
            checkDataLoaded();
        });

        // Load Articles from Firebase/Local via Repository
        articleRepository.getPublishedArticles().observe(this, articles -> {
            allArticles = articles;
            checkDataLoaded();
        });

        // Load Drugs from Firebase via Repository
        pharmacyRepository.getAllProducts().observe(this, products -> {
            allDrugs = products;
            checkDataLoaded();
        });
    }

    private int loadCount = 0;
    private void checkDataLoaded() {
        loadCount++;
        // We have 3 data sources
        if (loadCount >= 3) {
            progressBar.setVisibility(View.GONE);
            showRecentSearches();
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            showRecentSearches();
            return;
        }

        query = query.toLowerCase().trim();
        searchResults.clear();

        // 1. Search App Sections (Universal Navigation)
        String[] sectionTitles = {"Doctors Center", "Health Articles", "Book Appointment"};
        String[] sectionTags = {"Doctors", "Articles", "Appointments"};
        
        for (int i = 0; i < sectionTitles.length; i++) {
            if (sectionTitles[i].toLowerCase().contains(query)) {
                searchResults.add(new SearchResultAdapter.SearchResult(
                    SearchResultAdapter.TYPE_SUGGESTION,
                    sectionTitles[i],
                    "App Section",
                    sectionTags[i]
                ));
            }
        }

        // 2. Search doctors
        if (allDoctors != null) {
            for (Doctor doctor : allDoctors) {
                String doctorName = doctor.getFullName();
                String doctorSpecialty = doctor.getSpecialty();
                String doctorLocation = doctor.getLocation();
                
                // Safe null checks before toLowerCase()
                boolean nameMatch = doctorName != null && doctorName.toLowerCase().contains(query);
                boolean specialtyMatch = doctorSpecialty != null && doctorSpecialty.toLowerCase().contains(query);
                boolean locationMatch = doctorLocation != null && doctorLocation.toLowerCase().contains(query);
                
                if (nameMatch || specialtyMatch || locationMatch) {
                    SearchResultAdapter.SearchResult result = new SearchResultAdapter.SearchResult(
                        SearchResultAdapter.TYPE_DOCTOR,
                        doctorName != null ? doctorName : "Unknown Doctor",
                        doctorSpecialty != null ? doctorSpecialty : "Unknown Specialty",
                        doctor
                    );
                    searchResults.add(result);
                }
            }
        }

        // 3. Search articles
        if (allArticles != null) {
            for (com.haset.hasetapp.database.entities.ArticlePostEntity article : allArticles) {
                String title = article.getTitle();
                String desc = article.getDescription();
                
                if ((title != null && title.toLowerCase().contains(query)) || 
                    (desc != null && desc.toLowerCase().contains(query))) {
                    searchResults.add(new SearchResultAdapter.SearchResult(
                        SearchResultAdapter.TYPE_ARTICLE,
                        title != null ? title : "Health Article",
                        "By " + (article.getProfileName() != null ? article.getProfileName() : "Haset Health"),
                        article
                    ));
                }
            }
        }

        // 4. Search drugs
        if (allDrugs != null) {
            for (PharmacyProduct drug : allDrugs) {
                String name = drug.getName();
                String man = drug.getManufacturer();
                
                if ((name != null && name.toLowerCase().contains(query)) || 
                    (man != null && man.toLowerCase().contains(query))) {
                    searchResults.add(new SearchResultAdapter.SearchResult(
                        SearchResultAdapter.TYPE_DRUG,
                        name != null ? name : "Medicine",
                        man != null ? man : "Haset pharmacy",
                        drug
                    ));
                }
            }
        }
        
        updateSearchResults();
    }

    private void showRecentSearches() {
        searchResults.clear();
        
        // Universal categories as starting points
        String[] cats = {"Doctors Center", "Health Articles", "Book Appointment"};
        String[] subs = {"Find qualified specialists", "Read expert health tips", "Schedule your visit"};
        String[] tags = {"Doctors", "Articles", "Appointments"};
        
        for (int i = 0; i < cats.length; i++) {
            searchResults.add(new SearchResultAdapter.SearchResult(
                SearchResultAdapter.TYPE_SUGGESTION,
                cats[i],
                subs[i],
                tags[i]
            ));
        }

        updateSearchResults();
    }

    private void updateSearchResults() {
        androidx.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) rvSearchResults.getParent());
        searchAdapter.setSearchResults(searchResults);
        
        if (searchResults.isEmpty()) {
            noResultsLayout.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
            
            // Update subtitle with search query if available
            String currentQuery = etSearch.getText().toString().trim();
            if (!currentQuery.isEmpty()) {
                tvNoResultsSubtitle.setText("No results found for \"" + currentQuery + "\"\nTry searching with different keywords");
            } else {
                tvNoResultsSubtitle.setText(R.string.try_different_keywords);
            }
        } else {
            noResultsLayout.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSearchResultClick(SearchResultAdapter.SearchResult result) {
        switch (result.getType()) {
            case SearchResultAdapter.TYPE_DOCTOR:
                if (result.getData() instanceof Doctor) {
                    Doctor doctor = (Doctor) result.getData();
                    DoctorDetailsBottomSheet bottomSheet = DoctorDetailsBottomSheet.newInstance(doctor);
                    bottomSheet.show(getSupportFragmentManager(), "doctor_details_bottom_sheet");
                }
                break;
            case SearchResultAdapter.TYPE_DRUG:
                // DISABLED FOR V1 - PHARMACY COMING IN VERSION 2.0
                Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.pharmacy)), Toast.LENGTH_SHORT).show();
                break;
            case SearchResultAdapter.TYPE_ARTICLE:
                if (result.getData() instanceof com.haset.hasetapp.database.entities.ArticlePostEntity) {
                    Intent articleIntent = new Intent(this, ArticleActivity.class);
                    articleIntent.putExtra("post_id", ((com.haset.hasetapp.database.entities.ArticlePostEntity) result.getData()).getPostId());
                    startActivity(articleIntent);
                }
                break;
            case SearchResultAdapter.TYPE_SUGGESTION:
                if (result.getData() instanceof String) {
                    String tag = (String) result.getData();
                    handleCategoryNavigation(tag);
                } else {
                    etSearch.setText(result.getTitle());
                    etSearch.setSelection(etSearch.getText().length());
                }
                break;
        }
    }

    private void handleCategoryNavigation(String tag) {
        if (tag == null) return;
        
        Intent intent = null;
        switch (tag) {
            case "Doctors":
            case "Appointments":
                intent = new Intent(this, DoctorsActivity.class);
                break;
            case "Articles":
                intent = new Intent(this, ArticleActivity.class);
                break;
            case "Pharmacy":
                // DISABLED FOR V1 - PHARMACY COMING IN VERSION 2.0
                Toast.makeText(this, getString(R.string.feature_coming_soon, getString(R.string.pharmacy)), Toast.LENGTH_SHORT).show();
                break;
        }
        
        if (intent != null) {
            startActivity(intent);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Null out large data structures to help GC
        if (allDoctors != null) {
            allDoctors.clear();
            allDoctors = null;
        }
        if (allArticles != null) {
            allArticles.clear();
            allArticles = null;
        }
        if (allDrugs != null) {
            allDrugs.clear();
            allDrugs = null;
        }
        if (searchResults != null) {
            searchResults.clear();
            searchResults = null;
        }
        if (rvSearchResults != null) {
            rvSearchResults.setAdapter(null);
        }
        searchAdapter = null;
        
        // Null out view references
        etSearch = null;
        progressBar = null;
        rvSearchResults = null;
        noResultsLayout = null;
        tvNoResultsSubtitle = null;
        btnBack = null;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_slide_down);
    }
}
