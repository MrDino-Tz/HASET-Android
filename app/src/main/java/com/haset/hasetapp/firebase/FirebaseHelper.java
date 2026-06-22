package com.haset.hasetapp.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.haset.hasetapp.utils.Constants;

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    public FirebaseHelper() {
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }
    
    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }
    
    public FirebaseAuth getAuth() {
        return firebaseAuth;
    }
    
    public DatabaseReference getUsersRef() {
        return databaseReference.child(Constants.USERS_PATH);
    }
    
    public DatabaseReference getDoctorsRef() {
        return databaseReference.child(Constants.DOCTORS_PATH);
    }
    
    public DatabaseReference getAppointmentsRef() {
        return databaseReference.child(Constants.APPOINTMENTS_PATH);
    }
    
    public DatabaseReference getMessagesRef() {
        return databaseReference.child(Constants.MESSAGES_PATH);
    }
    
    public DatabaseReference getPrescriptionsRef() {
        return databaseReference.child(Constants.PRESCRIPTIONS_PATH);
    }
    
    public DatabaseReference getNotificationsRef() {
        return databaseReference.child(Constants.NOTIFICATIONS_PATH);
    }
    
    public DatabaseReference getArticlesRef() {
        return databaseReference.child(Constants.ARTICLES_PATH);
    }

    public DatabaseReference getAuditLogsRef() {
        return databaseReference.child(Constants.AUDIT_LOGS_PATH);
    }

    public DatabaseReference getUserConversationsRef() {
        return databaseReference.child(Constants.USER_CONVERSATIONS_PATH);
    }
    
    public String getCurrentUserId() {
        return firebaseAuth.getCurrentUser() != null ? 
               firebaseAuth.getCurrentUser().getUid() : null;
    }
    
    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    public void signOut() {
        firebaseAuth.signOut();
    }
    
    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }
}
