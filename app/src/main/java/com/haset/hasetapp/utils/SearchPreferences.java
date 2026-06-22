package com.haset.hasetapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.haset.hasetapp.utils.SearchFilterService.SearchFilters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchPreferences {
    
    private static final String PREF_NAME = "search_preferences";
    private static final String KEY_LAST_SEARCH_QUERY = "last_search_query";
    private static final String KEY_SELECTED_SPECIALTIES = "selected_specialties";
    private static final String KEY_SELECTED_GENDERS = "selected_genders";
    private static final String KEY_SELECTED_EXPERIENCE = "selected_experience";
    private static final String KEY_SELECTED_AVAILABILITY = "selected_availability";
    private static final String KEY_MIN_PRICE = "min_price";
    private static final String KEY_MAX_PRICE = "max_price";
    private static final String KEY_MAX_DISTANCE = "max_distance";
    private static final String KEY_SELECTED_LANGUAGES = "selected_languages";
    private static final String KEY_SELECTED_INSURANCE = "selected_insurance";
    private static final String KEY_MIN_RATING = "min_rating";
    private static final String KEY_SORT_BY = "sort_by";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final String KEY_FAVORITE_FILTERS = "favorite_filters";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    
    private SharedPreferences preferences;
    private Gson gson;
    
    public SearchPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    /**
     * Save current search filters
     */
    public void saveSearchFilters(SearchFilters filters) {
        SharedPreferences.Editor editor = preferences.edit();
        
        // Save basic filters
        editor.putString(KEY_LAST_SEARCH_QUERY, filters.getSearchQuery());
        editor.putString(KEY_SORT_BY, filters.getSortBy());
        editor.putString(KEY_SORT_ORDER, filters.getSortOrder());
        
        // Save lists as JSON
        if (filters.getSpecialties() != null) {
            editor.putString(KEY_SELECTED_SPECIALTIES, gson.toJson(filters.getSpecialties()));
        }
        
        if (filters.getGenders() != null) {
            editor.putString(KEY_SELECTED_GENDERS, gson.toJson(filters.getGenders()));
        }
        
        if (filters.getExperienceRanges() != null) {
            editor.putString(KEY_SELECTED_EXPERIENCE, gson.toJson(filters.getExperienceRanges()));
        }
        
        if (filters.getAvailabilityOptions() != null) {
            editor.putString(KEY_SELECTED_AVAILABILITY, gson.toJson(filters.getAvailabilityOptions()));
        }
        
        if (filters.getLanguages() != null) {
            editor.putString(KEY_SELECTED_LANGUAGES, gson.toJson(filters.getLanguages()));
        }
        
        if (filters.getInsuranceProviders() != null) {
            editor.putString(KEY_SELECTED_INSURANCE, gson.toJson(filters.getInsuranceProviders()));
        }
        
        // Save numeric values
        if (filters.getMinPrice() != null) {
            editor.putFloat(KEY_MIN_PRICE, filters.getMinPrice().floatValue());
        }
        
        if (filters.getMaxPrice() != null) {
            editor.putFloat(KEY_MAX_PRICE, filters.getMaxPrice().floatValue());
        }
        
        if (filters.getMaxDistance() != null) {
            editor.putFloat(KEY_MAX_DISTANCE, filters.getMaxDistance().floatValue());
        }
        
        if (filters.getMinRating() != null) {
            editor.putFloat(KEY_MIN_RATING, filters.getMinRating().floatValue());
        }
        
        editor.apply();
    }
    
    /**
     * Load saved search filters
     */
    public SearchFilters loadSearchFilters() {
        SearchFilters filters = new SearchFilters();
        
        // Load basic filters
        filters.setSearchQuery(preferences.getString(KEY_LAST_SEARCH_QUERY, ""));
        filters.setSortBy(preferences.getString(KEY_SORT_BY, "name"));
        filters.setSortOrder(preferences.getString(KEY_SORT_ORDER, "asc"));
        
        // Load lists from JSON
        Type listType = new TypeToken<List<String>>() {}.getType();
        
        String specialtiesJson = preferences.getString(KEY_SELECTED_SPECIALTIES, null);
        if (specialtiesJson != null) {
            filters.setSpecialties(gson.fromJson(specialtiesJson, listType));
        }
        
        String gendersJson = preferences.getString(KEY_SELECTED_GENDERS, null);
        if (gendersJson != null) {
            filters.setGenders(gson.fromJson(gendersJson, listType));
        }
        
        String experienceJson = preferences.getString(KEY_SELECTED_EXPERIENCE, null);
        if (experienceJson != null) {
            filters.setExperienceRanges(gson.fromJson(experienceJson, listType));
        }
        
        String availabilityJson = preferences.getString(KEY_SELECTED_AVAILABILITY, null);
        if (availabilityJson != null) {
            filters.setAvailabilityOptions(gson.fromJson(availabilityJson, listType));
        }
        
        String languagesJson = preferences.getString(KEY_SELECTED_LANGUAGES, null);
        if (languagesJson != null) {
            filters.setLanguages(gson.fromJson(languagesJson, listType));
        }
        
        String insuranceJson = preferences.getString(KEY_SELECTED_INSURANCE, null);
        if (insuranceJson != null) {
            filters.setInsuranceProviders(gson.fromJson(insuranceJson, listType));
        }
        
        // Load numeric values
        if (preferences.contains(KEY_MIN_PRICE)) {
            filters.setMinPrice((double) preferences.getFloat(KEY_MIN_PRICE, 0));
        }
        
        if (preferences.contains(KEY_MAX_PRICE)) {
            filters.setMaxPrice((double) preferences.getFloat(KEY_MAX_PRICE, 0));
        }
        
        if (preferences.contains(KEY_MAX_DISTANCE)) {
            filters.setMaxDistance((double) preferences.getFloat(KEY_MAX_DISTANCE, 0));
        }
        
        if (preferences.contains(KEY_MIN_RATING)) {
            filters.setMinRating(preferences.getFloat(KEY_MIN_RATING, 0));
        }
        
        return filters;
    }
    
    /**
     * Add search to recent searches
     */
    public void addToRecentSearches(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return;
        }
        
        List<String> recentSearches = getRecentSearches();
        
        // Remove if already exists to move to top
        recentSearches.remove(searchQuery);
        
        // Add to beginning
        recentSearches.add(0, searchQuery);
        
        // Keep only last 10 searches
        if (recentSearches.size() > 10) {
            recentSearches = recentSearches.subList(0, 10);
        }
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_RECENT_SEARCHES, gson.toJson(recentSearches));
        editor.apply();
    }
    
    /**
     * Get recent searches
     */
    public List<String> getRecentSearches() {
        String recentSearchesJson = preferences.getString(KEY_RECENT_SEARCHES, "[]");
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(recentSearchesJson, listType);
    }
    
    /**
     * Clear recent searches
     */
    public void clearRecentSearches() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_RECENT_SEARCHES);
        editor.apply();
    }
    
    /**
     * Save current filters as favorite
     */
    public void saveFavoriteFilter(String name, SearchFilters filters) {
        List<FavoriteFilter> favoriteFilters = getFavoriteFilters();
        
        // Remove if exists with same name
        favoriteFilters.removeIf(filter -> filter.getName().equals(name));
        
        // Add new favorite
        favoriteFilters.add(new FavoriteFilter(name, filters));
        
        // Keep only last 10 favorites
        if (favoriteFilters.size() > 10) {
            favoriteFilters = favoriteFilters.subList(0, 10);
        }
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_FAVORITE_FILTERS, gson.toJson(favoriteFilters));
        editor.apply();
    }
    
    /**
     * Get favorite filters
     */
    public List<FavoriteFilter> getFavoriteFilters() {
        String favoriteFiltersJson = preferences.getString(KEY_FAVORITE_FILTERS, "[]");
        Type listType = new TypeToken<List<FavoriteFilter>>() {}.getType();
        return gson.fromJson(favoriteFiltersJson, listType);
    }
    
    /**
     * Delete favorite filter
     */
    public void deleteFavoriteFilter(String name) {
        List<FavoriteFilter> favoriteFilters = getFavoriteFilters();
        favoriteFilters.removeIf(filter -> filter.getName().equals(name));
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_FAVORITE_FILTERS, gson.toJson(favoriteFilters));
        editor.apply();
    }
    
    /**
     * Add to search history
     */
    public void addToSearchHistory(String searchQuery, int resultCount) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return;
        }
        
        List<SearchHistoryItem> searchHistory = getSearchHistory();
        
        // Create new history item
        SearchHistoryItem historyItem = new SearchHistoryItem(searchQuery, resultCount, System.currentTimeMillis());
        
        // Remove if already exists
        searchHistory.removeIf(item -> item.getQuery().equals(searchQuery));
        
        // Add to beginning
        searchHistory.add(0, historyItem);
        
        // Keep only last 50 searches
        if (searchHistory.size() > 50) {
            searchHistory = searchHistory.subList(0, 50);
        }
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SEARCH_HISTORY, gson.toJson(searchHistory));
        editor.apply();
    }
    
    /**
     * Get search history
     */
    public List<SearchHistoryItem> getSearchHistory() {
        String searchHistoryJson = preferences.getString(KEY_SEARCH_HISTORY, "[]");
        Type listType = new TypeToken<List<SearchHistoryItem>>() {}.getType();
        return gson.fromJson(searchHistoryJson, listType);
    }
    
    /**
     * Clear search history
     */
    public void clearSearchHistory() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_SEARCH_HISTORY);
        editor.apply();
    }
    
    /**
     * Clear all search preferences
     */
    public void clearAllSearchPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * Favorite filter model
     */
    public static class FavoriteFilter {
        private String name;
        private SearchFilters filters;
        private long createdAt;
        
        public FavoriteFilter() {
            this.createdAt = System.currentTimeMillis();
        }
        
        public FavoriteFilter(String name, SearchFilters filters) {
            this.name = name;
            this.filters = filters;
            this.createdAt = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public SearchFilters getFilters() { return filters; }
        public void setFilters(SearchFilters filters) { this.filters = filters; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * Search history item model
     */
    public static class SearchHistoryItem {
        private String query;
        private int resultCount;
        private long timestamp;
        
        public SearchHistoryItem() {}
        
        public SearchHistoryItem(String query, int resultCount, long timestamp) {
            this.query = query;
            this.resultCount = resultCount;
            this.timestamp = timestamp;
        }
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public int getResultCount() { return resultCount; }
        public void setResultCount(int resultCount) { this.resultCount = resultCount; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
