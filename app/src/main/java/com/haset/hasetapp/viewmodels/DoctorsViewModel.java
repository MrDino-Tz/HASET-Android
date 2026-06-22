package com.haset.hasetapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;

import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.repositories.DoctorRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DoctorsViewModel extends AndroidViewModel {
    private final DoctorRepository repository;
    private final LiveData<List<Doctor>> allDoctors;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> specialtyFilter = new MutableLiveData<>(null);
    private final MutableLiveData<String> sortBy = new MutableLiveData<>("name");
    private final MediatorLiveData<List<String>> specialtiesRaw = new MediatorLiveData<>();
    private final MutableLiveData<List<String>> specialties = new MutableLiveData<>();
    
    private final MediatorLiveData<List<Doctor>> filteredDoctors = new MediatorLiveData<>();

    public DoctorsViewModel(@NonNull Application application) {
        super(application);
        repository = new DoctorRepository();
        allDoctors = repository.getAllDoctors();
        
        filteredDoctors.addSource(allDoctors, doctors -> applyFilters());
        filteredDoctors.addSource(searchQuery, query -> applyFilters());
        filteredDoctors.addSource(specialtyFilter, specialty -> applyFilters());
        filteredDoctors.addSource(sortBy, sort -> applyFilters());

        specialtiesRaw.addSource(allDoctors, doctors -> {
            if (doctors != null) {
                java.util.Set<String> set = new java.util.HashSet<>();
                for (Doctor d : doctors) {
                    if (d.getSpecialty() != null && !d.getSpecialty().isEmpty()) {
                        set.add(d.getSpecialty());
                    }
                }
                List<String> list = new ArrayList<>(set);
                java.util.Collections.sort(list);
                specialties.setValue(list);
            }
        });
    }

    public LiveData<List<Doctor>> getDoctors() {
        return filteredDoctors;
    }

    public LiveData<List<String>> getSpecialties() {
        return specialties;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void setSpecialtyFilter(String specialty) {
        specialtyFilter.setValue(specialty);
    }

    public void setSortBy(String sort) {
        sortBy.setValue(sort);
    }

    private void applyFilters() {
        List<Doctor> doctors = allDoctors.getValue();
        if (doctors == null) return;

        List<Doctor> filtered = new ArrayList<>();
        String query = searchQuery.getValue().toLowerCase().trim();
        String specialty = specialtyFilter.getValue();
        String sortCriterion = sortBy.getValue();

        for (Doctor doctor : doctors) {
            boolean matchSpecialty = (specialty == null || specialty.isEmpty() || specialty.equals(doctor.getSpecialty()));
            boolean matchSearch = query.isEmpty() || 
                    (doctor.getFullName() != null && doctor.getFullName().toLowerCase().contains(query)) ||
                    (doctor.getSpecialty() != null && doctor.getSpecialty().toLowerCase().contains(query)) ||
                    (doctor.getLocation() != null && doctor.getLocation().toLowerCase().contains(query));
            
            if (matchSpecialty && matchSearch) {
                filtered.add(doctor);
            }
        }

        // Apply Sorting
        Collections.sort(filtered, (d1, d2) -> {
            switch (sortCriterion) {
                case "rating":
                    return Float.compare(d2.getRating(), d1.getRating());
                case "experience":
                    return Integer.compare(d2.getExperience(), d1.getExperience());
                case "name":
                default:
                    String n1 = d1.getFullName() != null ? d1.getFullName() : "";
                    String n2 = d2.getFullName() != null ? d2.getFullName() : "";
                    return n1.compareToIgnoreCase(n2);
            }
        });

        filteredDoctors.setValue(filtered);
    }
    
    public void refreshDoctors() {
        // Trigger refresh by re-querying the repository
        // This will automatically update allDoctors LiveData and trigger filters
        repository.refreshDoctors();
    }
}
