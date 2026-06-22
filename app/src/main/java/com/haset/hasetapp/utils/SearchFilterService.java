package com.haset.hasetapp.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.haset.hasetapp.models.Doctor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchFilterService {
    
    private static final String TAG = "SearchFilterService";
    private Context context;
    
    public SearchFilterService(Context context) {
        this.context = context;
    }
    
    public List<Doctor> filterDoctors(List<Doctor> doctors, SearchFilters filters) {
        List<Doctor> filteredList = new ArrayList<>();
        
        for (Doctor doctor : doctors) {
            if (matchesAllFilters(doctor, filters)) {
                filteredList.add(doctor);
            }
        }
        
        // Apply sorting
        sortDoctors(filteredList, filters);
        
        Log.d(TAG, "Filtered " + doctors.size() + " doctors down to " + filteredList.size());
        return filteredList;
    }
    
    private boolean matchesAllFilters(Doctor doctor, SearchFilters filters) {
        return matchesQueryFilter(doctor, filters) &&
               matchesSpecialtyFilter(doctor, filters) &&
               matchesGenderFilter(doctor, filters) &&
               matchesExperienceFilter(doctor, filters) &&
               matchesAvailabilityFilter(doctor, filters) &&
               matchesPriceFilter(doctor, filters) &&
               matchesLocationFilter(doctor, filters) &&
               matchesLanguageFilter(doctor, filters) &&
               matchesInsuranceFilter(doctor, filters) &&
               matchesRatingFilter(doctor, filters);
    }
    
    private boolean matchesQueryFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getSearchQuery() == null || filters.getSearchQuery().trim().isEmpty()) {
            return true;
        }
        
        String query = filters.getSearchQuery().toLowerCase().trim();
        
        // Search in name, specialty, about, and location
        return (doctor.getFullName() != null && doctor.getFullName().toLowerCase().contains(query)) ||
               (doctor.getSpecialty() != null && doctor.getSpecialty().toLowerCase().contains(query)) ||
               (doctor.getAbout() != null && doctor.getAbout().toLowerCase().contains(query)) ||
               (doctor.getLocation() != null && doctor.getLocation().toLowerCase().contains(query)) ||
               (doctor.getEducation() != null && doctor.getEducation().toLowerCase().contains(query)) ||
               containsInList(doctor.getLanguages(), query) ||
               containsInList(doctor.getCertifications(), query);
    }
    
    private boolean matchesSpecialtyFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getSpecialties() == null || filters.getSpecialties().isEmpty()) {
            return true;
        }
        
        return filters.getSpecialties().contains(doctor.getSpecialty());
    }
    
    private boolean matchesGenderFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getGenders() == null || filters.getGenders().isEmpty()) {
            return true;
        }
        
        if (filters.getGenders().contains("No Preference")) {
            return true;
        }
        
        String doctorGender = doctor.getGender();
        if (doctorGender == null) return true;
        
        return (filters.getGenders().contains("Male Doctors") && doctorGender.equalsIgnoreCase("male")) ||
               (filters.getGenders().contains("Female Doctors") && doctorGender.equalsIgnoreCase("female"));
    }
    
    private boolean matchesExperienceFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getExperienceRanges() == null || filters.getExperienceRanges().isEmpty()) {
            return true;
        }
        
        int experience = doctor.getExperience();
        
        for (String range : filters.getExperienceRanges()) {
            switch (range) {
                case "0-5 Years":
                    if (experience >= 0 && experience <= 5) return true;
                    break;
                case "5-10 Years":
                    if (experience >= 5 && experience <= 10) return true;
                    break;
                case "10-15 Years":
                    if (experience >= 10 && experience <= 15) return true;
                    break;
                case "15+ Years":
                    if (experience >= 15) return true;
                    break;
            }
        }
        
        return false;
    }
    
    private boolean matchesAvailabilityFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getAvailabilityOptions() == null || filters.getAvailabilityOptions().isEmpty()) {
            return true;
        }
        
        Calendar now = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        
        for (String option : filters.getAvailabilityOptions()) {
            switch (option) {
                case "Available Today":
                    if (isDoctorAvailableOnDate(doctor, now)) return true;
                    break;
                case "Available Tomorrow":
                    if (isDoctorAvailableOnDate(doctor, tomorrow)) return true;
                    break;
                case "This Week":
                    if (isDoctorAvailableThisWeek(doctor)) return true;
                    break;
                case "Weekend Only":
                    if (isDoctorAvailableOnWeekend(doctor)) return true;
                    break;
            }
        }
        
        return false;
    }
    
    private boolean matchesPriceFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getMinPrice() == null && filters.getMaxPrice() == null) {
            return true;
        }
        
        double price = doctor.getConsultationFee();
        
        if (filters.getMinPrice() != null && price < filters.getMinPrice()) {
            return false;
        }
        
        if (filters.getMaxPrice() != null && price > filters.getMaxPrice()) {
            return false;
        }
        
        return true;
    }
    
    private boolean matchesLocationFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getMaxDistance() == null || filters.getUserLocation() == null) {
            return true;
        }
        
        if (doctor.getLatitude() == 0 || doctor.getLongitude() == 0) {
            return true; // Can't filter if location is not available
        }
        
        float[] results = new float[1];
        Location.distanceBetween(
                filters.getUserLocation().getLatitude(),
                filters.getUserLocation().getLongitude(),
                doctor.getLatitude(),
                doctor.getLongitude(),
                results
        );
        
        float distanceInMeters = results[0];
        float distanceInKm = distanceInMeters / 1000;
        
        return distanceInKm <= filters.getMaxDistance();
    }
    
    private boolean matchesLanguageFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getLanguages() == null || filters.getLanguages().isEmpty()) {
            return true;
        }
        
        if (doctor.getLanguages() == null || doctor.getLanguages().isEmpty()) {
            return false;
        }
        
        for (String language : filters.getLanguages()) {
            if (containsInList(doctor.getLanguages(), language.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean matchesInsuranceFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getInsuranceProviders() == null || filters.getInsuranceProviders().isEmpty()) {
            return true;
        }
        
        if (doctor.getInsuranceProviders() == null || doctor.getInsuranceProviders().isEmpty()) {
            return false;
        }
        
        for (String insurance : filters.getInsuranceProviders()) {
            if (containsInList(doctor.getInsuranceProviders(), insurance.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean matchesRatingFilter(Doctor doctor, SearchFilters filters) {
        if (filters.getMinRating() == null) {
            return true;
        }
        
        return doctor.getRating() >= filters.getMinRating();
    }
    
    private void sortDoctors(List<Doctor> doctors, SearchFilters filters) {
        if (filters.getSortBy() == null) {
            return;
        }
        
        Comparator<Doctor> comparator;
        
        switch (filters.getSortBy()) {
            case "name":
                comparator = Comparator.comparing(Doctor::getFullName, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "rating":
                comparator = Comparator.comparing(Doctor::getRating, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "experience":
                comparator = Comparator.comparingInt(Doctor::getExperience);
                break;
            case "price":
                comparator = Comparator.comparingDouble(Doctor::getConsultationFee);
                break;
            case "patients_treated":
                comparator = Comparator.comparingInt(Doctor::getPatientsTreated);
                break;
            case "response_time":
                comparator = Comparator.comparing(Doctor::getResponseTime, 
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(Doctor::getFullName);
                break;
        }
        
        if ("desc".equals(filters.getSortOrder())) {
            comparator = comparator.reversed();
        }
        
        Collections.sort(doctors, comparator);
    }
    
    // Helper methods
    private boolean containsInList(List<String> list, String query) {
        if (list == null || list.isEmpty()) return false;
        
        for (String item : list) {
            if (item != null && item.toLowerCase().contains(query)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isDoctorAvailableOnDate(Doctor doctor, Calendar date) {
        if (doctor.getAvailableDays() == null || doctor.getAvailableDays().isEmpty()) {
            return false;
        }
        
        String dayOfWeek = getDayOfWeek(date);
        return doctor.getAvailableDays().contains(dayOfWeek);
    }
    
    private boolean isDoctorAvailableThisWeek(Doctor doctor) {
        Calendar now = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            Calendar checkDate = (Calendar) now.clone();
            checkDate.add(Calendar.DAY_OF_YEAR, i);
            if (isDoctorAvailableOnDate(doctor, checkDate)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isDoctorAvailableOnWeekend(Doctor doctor) {
        Calendar saturday = Calendar.getInstance();
        saturday.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        
        Calendar sunday = Calendar.getInstance();
        sunday.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        return isDoctorAvailableOnDate(doctor, saturday) || isDoctorAvailableOnDate(doctor, sunday);
    }
    
    private String getDayOfWeek(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "";
        }
    }
    
    // SearchFilters class to hold all filter criteria
    public static class SearchFilters {
        private String searchQuery;
        private List<String> specialties;
        private List<String> genders;
        private List<String> experienceRanges;
        private List<String> availabilityOptions;
        private Double minPrice;
        private Double maxPrice;
        private Double maxDistance;
        private Location userLocation;
        private List<String> languages;
        private List<String> insuranceProviders;
        private Float minRating;
        private String sortBy;
        private String sortOrder;
        
        // Getters and Setters
        public String getSearchQuery() { return searchQuery; }
        public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
        
        public List<String> getSpecialties() { return specialties; }
        public void setSpecialties(List<String> specialties) { this.specialties = specialties; }
        
        public List<String> getGenders() { return genders; }
        public void setGenders(List<String> genders) { this.genders = genders; }
        
        public List<String> getExperienceRanges() { return experienceRanges; }
        public void setExperienceRanges(List<String> experienceRanges) { this.experienceRanges = experienceRanges; }
        
        public List<String> getAvailabilityOptions() { return availabilityOptions; }
        public void setAvailabilityOptions(List<String> availabilityOptions) { this.availabilityOptions = availabilityOptions; }
        
        public Double getMinPrice() { return minPrice; }
        public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
        
        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
        
        public Double getMaxDistance() { return maxDistance; }
        public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }
        
        public Location getUserLocation() { return userLocation; }
        public void setUserLocation(Location userLocation) { this.userLocation = userLocation; }
        
        public List<String> getLanguages() { return languages; }
        public void setLanguages(List<String> languages) { this.languages = languages; }
        
        public List<String> getInsuranceProviders() { return insuranceProviders; }
        public void setInsuranceProviders(List<String> insuranceProviders) { this.insuranceProviders = insuranceProviders; }
        
        public Float getMinRating() { return minRating; }
        public void setMinRating(Float minRating) { this.minRating = minRating; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    }
}
