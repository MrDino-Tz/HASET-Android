package com.haset.hasetapp.api;

import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.models.Doctor;
import com.haset.hasetapp.models.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API Service Interface for REST API calls
 * This is a placeholder - replace with your actual API endpoints
 */
public interface ApiService {
    
    // Authentication
    @POST("auth/login")
    Call<ApiResponse<User>> login(@Body LoginRequest request);
    
    @POST("auth/register")
    Call<ApiResponse<User>> register(@Body RegisterRequest request);
    
    // Doctors
    @GET("doctors")
    Call<ApiResponse<List<Doctor>>> getDoctors();
    
    @GET("doctors/search")
    Call<ApiResponse<List<Doctor>>> searchDoctors(@Query("specialty") String specialty);
    
    @GET("doctors/{id}")
    Call<ApiResponse<Doctor>> getDoctorById(@Path("id") String doctorId);
    
    // Appointments
    @POST("appointments")
    Call<ApiResponse<Appointment>> bookAppointment(@Body Appointment appointment);
    
    @GET("appointments/patient/{patientId}")
    Call<ApiResponse<List<Appointment>>> getPatientAppointments(@Path("patientId") String patientId);
    
    @GET("appointments/doctor/{doctorId}")
    Call<ApiResponse<List<Appointment>>> getDoctorAppointments(@Path("doctorId") String doctorId);
    
    @PUT("appointments/{id}/status")
    Call<ApiResponse<Appointment>> updateAppointmentStatus(
        @Path("id") String appointmentId,
        @Body StatusUpdateRequest request
    );
    
    // Inner classes for requests
    class LoginRequest {
        public String email;
        public String password;
        
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
    
    class RegisterRequest {
        public String email;
        public String password;
        public String fullName;
        public String phone;
        public String role;
        
        public RegisterRequest(String email, String password, String fullName, String phone, String role) {
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.phone = phone;
            this.role = role;
        }
    }
    
    class StatusUpdateRequest {
        public String status;
        
        public StatusUpdateRequest(String status) {
            this.status = status;
        }
    }
}
