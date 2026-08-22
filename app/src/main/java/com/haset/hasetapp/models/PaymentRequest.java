package com.haset.hasetapp.models;

public class PaymentRequest {
    private String user_id;
    private String doctor_id;
    private String consultation_id;
    private long amount;
    private String payment_method;
    private String provider;
    private String payment_account;
    private String redirect_url;
    private String cancel_url;
    private CardCustomer customer;

    public static class CardCustomer {
        private String name;
        private String firstname;
        private String lastname;
        private String email;
        private String address;
        private String city;
        private String state;
        private String postcode;
        private String country;
        private String phone;

        public CardCustomer(String firstname, String lastname, String email, String phone) {
            this.firstname = safeString(firstname, "HASET");
            this.lastname = safeString(lastname, "Customer");
            this.name = this.firstname + " " + this.lastname;
            this.email = safeString(email, "support@hasethospital.or.tz");
            this.phone = phone == null || phone.trim().isEmpty() ? null : phone.trim();
            this.address = "HASET Hospital";
            this.city = "Dar es Salaam";
            this.state = "Dar es Salaam";
            this.postcode = "14101";
            this.country = "TZ";
        }

        private static String safeString(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }
    }

    public PaymentRequest(String userId, String doctorId, String consultationId, double amount,
                         String provider, String paymentAccount) {
        this(userId, doctorId, consultationId, amount, "mobile_money", provider, paymentAccount,
                null, null, null, null);
    }

    public PaymentRequest(String userId, String doctorId, String consultationId, double amount,
                          String paymentMethod, String provider, String paymentAccount,
                          String buyerEmail, String buyerName, String buyerPhone,
                          String redirectBaseUrl) {
        this.user_id = safeString(userId, "unknown_user");
        this.doctor_id = safeString(doctorId, "unknown_doctor");
        this.consultation_id = safeString(consultationId, "unknown_consultation");
        this.amount = Math.round(amount);
        String requestedMethod = safeString(paymentMethod, "mobile_money");
        // Card and checkout payments are both collected through a Snippe hosted session.
        this.payment_method = requestedMethod;
        if ("mobile_money".equals(this.payment_method)) {
            this.provider = safeString(provider, "unknown");
            this.payment_account = safeString(paymentAccount, "unknown");
        } else {
            // Card payments prohibit direct mobile-money provider/account fields.
            this.provider = null;
            this.payment_account = null;
        }
        if ("card".equals(requestedMethod) || "checkout".equals(requestedMethod)) {
            String safeName = buyerName == null || buyerName.trim().isEmpty() ? "HASET Customer" : buyerName.trim();
            String[] parts = safeName.split("\\s+", 2);
            String first = parts[0];
            String last = parts.length > 1 ? parts[1] : "Customer";
            this.customer = new CardCustomer(first, last,
                    buyerEmail == null ? "support@hasethospital.or.tz" : buyerEmail,
                    buyerPhone);
            String base = redirectBaseUrl == null
                    ? "https://hasethospital.or.tz/payment"
                    : redirectBaseUrl;
            this.redirect_url = base + "/success";
            this.cancel_url = null;
        }
    }

    private static String safeString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    // Getters
    public String getUserId() { return user_id; }
    public String getDoctorId() { return doctor_id; }
    public String getConsultationId() { return consultation_id; }
    public long getAmount() { return amount; }
    public String getPaymentMethod() { return payment_method; }
    public String getProvider() { return provider; }
    public String getPaymentAccount() { return payment_account; }

    // Setters
    public void setUserId(String userId) { this.user_id = userId; }
    public void setDoctorId(String doctorId) { this.doctor_id = doctorId; }
    public void setConsultationId(String consultationId) { this.consultation_id = consultationId; }
    public void setAmount(double amount) { this.amount = Math.round(amount); }
    public void setPaymentMethod(String paymentMethod) { this.payment_method = paymentMethod; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setPaymentAccount(String paymentAccount) { this.payment_account = paymentAccount; }
}
