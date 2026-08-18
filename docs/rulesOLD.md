## Full rules JSON

```json
{
  "rules": {
    ".read": false,
    ".write": false,
    "users": {
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      "$uid": {
        ".read": "auth != null && ($uid === auth.uid || root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
        ".write": "auth != null && (($uid === auth.uid && ((!data.exists() && (!newData.child('role').exists() || newData.child('role').val() === 'patient') && !newData.child('adminRole').exists() && !newData.child('admin_role').exists()) || (data.exists() && newData.child('role').val() === data.child('role').val() && newData.child('adminRole').val() === data.child('adminRole').val() && newData.child('admin_role').val() === data.child('admin_role').val()))) || root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
      }
    },
    "doctors": {
      ".read": true,
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
    },
    "appointments": {
      ".read": "auth != null",
      ".write": false,
      ".indexOn": ["createdAt", "patientId", "patient_id", "user_id", "doctorId", "doctor_id"],
      "$appointmentId": {
        ".write": "auth != null"
      }
    },
    "departments": {
      ".read": true,
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
    },
    "promotional_banners": {
      ".read": true,
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
    },
    "article_posts": {
      ".read": true,
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
    },
    "article_post_drafts": {
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
    },
    "health_quotes": {
      ".read": true,
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".indexOn": ["createdAt", "updatedAt"],
      "$tipId": {
        ".validate": "!newData.exists() || (newData.child('text').isString() && newData.child('text').val().length > 0 && newData.child('text').val().length <= 600 && newData.child('author').isString() && newData.child('author').val().length <= 80 && newData.child('enabled').isBoolean() && newData.child('createdAt').isNumber())"
      }
    },
    "app_config": {
      ".read": true,
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      "doctorRegistrationFee": {
        ".validate": "newData.isNumber() && newData.val() >= 0"
      }
    },
    "notifications": {
      "$uid": {
        ".read": "auth != null && ($uid === auth.uid || root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
        ".write": "auth != null && ($uid === auth.uid || root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
      }
    },
    "support_tickets": {
      ".read": "auth != null",
      ".write": false,
      ".indexOn": ["timestamp", "createdAt", "userId", "user_id", "uid"],
      "$ticketId": {
        ".write": "auth != null"
      }
    },
    "doctor_wallets": {
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')"
    },
    "withdrawal_requests": {
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".indexOn": ["createdAt", "created_at", "status"]
    },
    "payout_destination_requests": {
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".indexOn": ["createdAt", "created_at", "status"]
    },
    "audit_logs": {
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".write": "auth != null && (root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('adminRole').val() === 'super_admin' || root.child('users').child(auth.uid).child('admin_role').val() === 'super_admin')",
      ".indexOn": ["createdAt", "created_at", "action"]
    }
  }
}


```
