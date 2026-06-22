package com.haset.hasetapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.print.PrintHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.MedicineAdapter;
import com.haset.hasetapp.models.Prescription;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrescriptionDetailBottomSheet extends BottomSheetDialogFragment {

    private String prescriptionId;
    private Prescription prescription;
    private PrescriptionHelper prescriptionHelper;

    private ImageView ivDoctorAvatar, ivPrescriptionImage, ivClose;
    private TextView tvPrescriptionId, tvDate, tvDoctorName, tvDoctorSpecialty, tvInstructions;
    private RecyclerView rvMedicines;
    private View prescriptionContent;
    private View cardImage;
    private MaterialButton btnExportPdf, btnPrint;

    public static PrescriptionDetailBottomSheet newInstance(String prescriptionId) {
        PrescriptionDetailBottomSheet fragment = new PrescriptionDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString("prescription_id", prescriptionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        if (getArguments() != null) {
            prescriptionId = getArguments().getString("prescription_id");
        }
        
        // Block screenshots for prescription details (sensitive - medical data)
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_prescription_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        loadPrescriptionData();
    }

    private void initializeViews(View view) {
        ivDoctorAvatar = view.findViewById(R.id.ivDoctorAvatar);
        ivPrescriptionImage = view.findViewById(R.id.ivPrescriptionImage);
        ivClose = view.findViewById(R.id.ivClose);
        tvPrescriptionId = view.findViewById(R.id.tvPrescriptionId);
        tvDate = view.findViewById(R.id.tvDate);
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvDoctorSpecialty = view.findViewById(R.id.tvDoctorSpecialty);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        rvMedicines = view.findViewById(R.id.rvMedicines);
        prescriptionContent = view.findViewById(R.id.prescriptionContent);
        cardImage = view.findViewById(R.id.cardImage);
        btnExportPdf = view.findViewById(R.id.btnExportPdf);
        btnPrint = view.findViewById(R.id.btnPrint);

        ivClose.setOnClickListener(v -> dismiss());

        btnExportPdf.setOnClickListener(v -> exportAsPdf());
        btnPrint.setOnClickListener(v -> printPrescription());
        
        prescriptionHelper = new PrescriptionHelper(requireContext());
    }

    private void loadPrescriptionData() {
        if (prescriptionId == null) return;

        prescriptionHelper.getPrescriptionById(prescriptionId, new PrescriptionHelper.PrescriptionCallback() {
            @Override
            public void onSuccess(Prescription result) {
                prescription = result;
                if (isAdded()) {
                    displayPrescription();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayPrescription() {
        if (prescription.getPrescriptionId() != null) {
            tvPrescriptionId.setText("ID: #" + prescription.getPrescriptionId().substring(0, Math.min(10, prescription.getPrescriptionId().length())));
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvDate.setText("Issued on " + sdf.format(new Date(prescription.getCreatedAt())));
        
        tvDoctorName.setText(prescription.getDoctorName() != null ? prescription.getDoctorName() : "Doctor");
        tvDoctorSpecialty.setText("Consultant Physician"); 

        tvInstructions.setText(prescription.getInstructions() != null ? prescription.getInstructions() : "No special instructions.");

        if (prescription.getImageUrl() != null && !prescription.getImageUrl().isEmpty()) {
            cardImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(prescription.getImageUrl()).into(ivPrescriptionImage);
        } else {
            cardImage.setVisibility(View.GONE);
        }

        if (prescription.getMedicines() != null) {
            rvMedicines.setLayoutManager(new LinearLayoutManager(requireContext()));
            MedicineAdapter adapter = new MedicineAdapter(prescription.getMedicines());
            rvMedicines.setAdapter(adapter);
        }
    }

    private void printPrescription() {
        if (prescriptionContent == null) return;

        Bitmap bitmap = createBitmapFromView(prescriptionContent);
        if (bitmap == null) return;

        try {
            Activity activity = requireActivity();
            PrintHelper printHelper = new PrintHelper(activity);
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("Prescription_" + prescriptionId, bitmap);
        } catch (Exception e) {
            // Fallback: share the bitmap so user can print from any app
            shareBitmapFallback(bitmap);
        }
    }

    private void shareBitmapFallback(Bitmap bitmap) {
        try {
            File cacheDir = getContext() != null ? getContext().getCacheDir() : requireActivity().getCacheDir();
            File file = new File(cacheDir, "prescription_" + (prescriptionId != null ? prescriptionId : "temp") + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Prescription"));
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Could not print or share prescription", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportAsPdf() {
        if (prescription == null) return;
        
        Bitmap bitmap = createBitmapFromView(prescriptionContent);
        if (bitmap == null) return;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        File pdfFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Prescription_" + prescriptionId + ".pdf");
        
        try {
            document.writeTo(new FileOutputStream(pdfFile));
            document.close();
            shareFile(pdfFile, "application/pdf");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            document.close();
        }
    }

    private void shareFile(File file, String type) {
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Prescription"));
    }

    private Bitmap createBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }
}
