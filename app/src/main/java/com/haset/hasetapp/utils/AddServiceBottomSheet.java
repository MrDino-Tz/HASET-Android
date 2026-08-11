package com.haset.hasetapp.utils;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Service;
import com.haset.hasetapp.utils.PreferenceManager;

import java.text.NumberFormat;
import java.util.Locale;

public class AddServiceBottomSheet extends BottomSheetDialogFragment {
    
    private TextInputEditText etServiceName, etAppointmentFee;
    private Slider sliderPercentage;
    private TextView tvPercentage, tvCalculatedAmount;
    private MaterialButton btnCancel, btnSend;
    
    private String patientId, patientName, doctorId, doctorName;
    private OnServiceCreatedListener listener;
    
    public interface OnServiceCreatedListener {
        void onServiceCreated(Service service);
    }
    
    public void setOnServiceCreatedListener(OnServiceCreatedListener listener) {
        this.listener = listener;
    }
    
    public static AddServiceBottomSheet newInstance(String patientId, String patientName, 
                                                    String doctorId, String doctorName) {
        AddServiceBottomSheet fragment = new AddServiceBottomSheet();
        Bundle args = new Bundle();
        args.putString("patientId", patientId);
        args.putString("patientName", patientName);
        args.putString("doctorId", doctorId);
        args.putString("doctorName", doctorName);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            patientId = getArguments().getString("patientId");
            patientName = getArguments().getString("patientName");
            doctorId = getArguments().getString("doctorId");
            doctorName = getArguments().getString("doctorName");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_service, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
    }
    
    private void initViews(View view) {
        etServiceName = view.findViewById(R.id.etServiceName);
        etAppointmentFee = view.findViewById(R.id.etAppointmentFee);
        sliderPercentage = view.findViewById(R.id.sliderPercentage);
        tvPercentage = view.findViewById(R.id.tvPercentage);
        tvCalculatedAmount = view.findViewById(R.id.tvCalculatedAmount);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSend = view.findViewById(R.id.btnSend);
    }
    
    private void setupListeners() {
        // Slider change listener
        sliderPercentage.addOnChangeListener((slider, value, fromUser) -> {
            int percentage = (int) value;
            tvPercentage.setText(percentage + "%");
            updateCalculatedAmount();
        });

        etAppointmentFee.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCalculatedAmount();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dismiss());
        
        // Send button
        btnSend.setOnClickListener(v -> sendService());
        updateCalculatedAmount();
    }
    
    private void updateCalculatedAmount() {
        try {
            double fee = parseFee();
            int percentage = (int) sliderPercentage.getValue();
            
            double calculatedAmount = (fee * percentage) / 100.0;
            tvCalculatedAmount.setText(formatCurrency(calculatedAmount) + " TZS");
        } catch (NumberFormatException e) {
            tvCalculatedAmount.setText("0 TZS");
        }
    }
    
    private void sendService() {
        String serviceName = etServiceName.getText() != null ? 
            etServiceName.getText().toString().trim() : "";
        String feeStr = etAppointmentFee.getText() != null ? 
            etAppointmentFee.getText().toString().trim() : "";
        
        // Validation
        if (serviceName.isEmpty()) {
            etServiceName.setError(requireContext().getString(R.string.error_service_name_required));
            etServiceName.requestFocus();
            return;
        }
        
        if (feeStr.isEmpty()) {
            etAppointmentFee.setError(requireContext().getString(R.string.error_fee_required));
            etAppointmentFee.requestFocus();
            return;
        }
        
        double fee;
        try {
            fee = parseFee();
        } catch (NumberFormatException e) {
            etAppointmentFee.setError(requireContext().getString(R.string.error_invalid_fee));
            etAppointmentFee.requestFocus();
            return;
        }
        
        if (fee <= 0) {
            etAppointmentFee.setError(requireContext().getString(R.string.error_fee_greater_than_zero));
            etAppointmentFee.requestFocus();
            return;
        }

        double patientAmount = (fee * sliderPercentage.getValue()) / 100.0;
        if (patientAmount < Constants.MIN_PAYMENT_AMOUNT) {
            etAppointmentFee.setError(requireContext().getString(
                    R.string.error_service_payment_minimum,
                    formatCurrency(Constants.MIN_PAYMENT_AMOUNT)));
            etAppointmentFee.requestFocus();
            return;
        }
        
        // Create service object
        Service service = new Service();
        service.setServiceName(serviceName);
        service.setAppointmentFee(fee);
        service.setPatientPercentage((int) sliderPercentage.getValue());
        service.setPatientPayAmount(patientAmount);
        service.setDoctorId(doctorId);
        service.setPatientId(patientId);
        
        // Notify listener
        if (listener != null) {
            listener.onServiceCreated(service);
        }
        
        dismiss();
    }
    
    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        return formatter.format(amount);
    }

    private double parseFee() {
        String raw = etAppointmentFee.getText() == null
                ? ""
                : etAppointmentFee.getText().toString().trim();
        String numeric = raw.replace(",", "").replaceAll("[^0-9.]", "");
        if (numeric.isEmpty()) return 0.0;
        return Double.parseDouble(numeric);
    }
}
