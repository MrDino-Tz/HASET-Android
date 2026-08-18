# Test Credentials for HASETApp App

## 🔐 Pre-configured Test Users

The app automatically creates test users on first launch. Use these credentials to login:

---

### **Patient Account**
```
Email:    patient@test.com
Password: password123
Name:     John Patient
Role:     Patient
```

**Use this account to:**
- Browse doctors
- Book appointments
- View appointment history
- Chat with doctors

---

### **Doctor Account #1**
```
Email:    doctor@test.com
Password: password123
Name:     Dr. Sarah Smith
Role:     Doctor
```

**Use this account to:**
- View pending appointments
- Approve/decline appointments
- Manage patient list
- Chat with patients

---

### **Doctor Account #2**
```
Email:    doctor2@test.com
Password: password123
Name:     Dr. Michael Johnson
Role:     Doctor
```

---

## 📝 Notes

1. **First Launch**: Test users are created automatically when you first install/run the app
2. **Password**: All test accounts use the same password: `password123`
3. **Hashing**: Passwords are stored as SHA-256 hashes in the database
4. **Custom Accounts**: You can also create your own accounts using the registration flow

---

## 🧪 Testing Scenarios

### **Scenario 1: Patient Books Appointment**
1. Login as `patient@test.com`
2. Browse doctors (you'll see Dr. Sarah Smith and Dr. Michael Johnson)
3. Click "Book Appointment" on any doctor
4. Select date and time
5. Submit appointment

### **Scenario 2: Doctor Manages Appointments**
1. Login as `doctor@test.com`
2. View "Pending Appointments" tab
3. See appointment from patient
4. Click "Approve" or "Decline"
5. View "Approved Appointments" tab

### **Scenario 3: Create New Account**
1. Click "Register" on login screen
2. Select role (Patient or Doctor)
3. Fill in details
4. Create account
5. Login with new credentials

---

## 🔧 Development Tips

### **Reset Database**
To clear all data and start fresh:
1. Uninstall the app
2. Reinstall/run again
3. Test users will be recreated

### **Add More Test Users**
Edit `DatabaseSeeder.java` to add more test accounts

### **Disable Auto-Seeding**
Comment out the `seedTestUsers()` call in `HASETApplication.java`

---

## ⚠️ Security Note

**These are TEST credentials only!**
- Do NOT use in production
- Change passwords before deployment
- Implement proper authentication for production use

---

**Quick Start**: Just launch the app and login with `patient@test.com` / `password123` 🚀
