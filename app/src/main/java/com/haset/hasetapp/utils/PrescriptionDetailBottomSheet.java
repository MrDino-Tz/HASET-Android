package com.haset.hasetapp.utils;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.print.PrintHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.MedicineAdapter;
import com.haset.hasetapp.models.Prescription;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrescriptionDetailBottomSheet extends BottomSheetDialogFragment {

    // ---------------------------------------------------------------------------
    // Threading — bitmap capture must stay on the main thread (View.draw()
    // requirement); all disk I/O runs on pdfExecutor to keep the UI responsive.
    // ---------------------------------------------------------------------------
    private final ExecutorService pdfExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Save paths (mirrors PrescriptionDetailFragment constants)
    private static final String PRESCRIPTION_FOLDER_Q      = "Download/HASET/Prescriptions";
    private static final String PRESCRIPTION_FOLDER_LEGACY = "Documents/HASET/Prescriptions";

    private String prescriptionId;
    private Prescription prescription;
    private PrescriptionHelper prescriptionHelper;

    private ImageView ivDoctorAvatar, ivPrescriptionImage, ivClose;
    private TextView tvPrescriptionId, tvDate, tvPatientName, tvDoctorName, tvDoctorSpecialty, tvInstructions;
    private RecyclerView rvMedicines;
    private View prescriptionContent;
    private View cardImage;
    private MaterialButton btnExportPdf, btnPrint;

    // -------------------------------------------------------------------------

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

        // Block screenshots for prescription details (sensitive — medical data).
        // Note: FLAG_SECURE only blocks OS-level screenshots/screen recording.
        // Direct View.draw() to our own Canvas (used for PDF/print) is unaffected.
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_prescription_details, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null) return;
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;
        int targetHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.92f);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = targetHeight;
        bottomSheet.setLayoutParams(params);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setPeekHeight(targetHeight);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        loadPrescriptionData();
    }

    private void initializeViews(View view) {
        ivDoctorAvatar       = view.findViewById(R.id.ivDoctorAvatar);
        ivPrescriptionImage  = view.findViewById(R.id.ivPrescriptionImage);
        ivClose              = view.findViewById(R.id.ivClose);
        tvPrescriptionId     = view.findViewById(R.id.tvPrescriptionId);
        tvDate               = view.findViewById(R.id.tvDate);
        tvPatientName        = view.findViewById(R.id.tvPatientName);
        tvDoctorName         = view.findViewById(R.id.tvDoctorName);
        tvDoctorSpecialty    = view.findViewById(R.id.tvDoctorSpecialty);
        tvInstructions       = view.findViewById(R.id.tvInstructions);
        rvMedicines          = view.findViewById(R.id.rvMedicines);
        prescriptionContent  = view.findViewById(R.id.prescriptionContent);
        cardImage            = view.findViewById(R.id.cardImage);
        btnExportPdf         = view.findViewById(R.id.btnExportPdf);
        btnPrint             = view.findViewById(R.id.btnPrint);

        ivClose.setOnClickListener(v -> dismiss());
        btnExportPdf.setOnClickListener(v -> exportAsPdf());
        btnPrint.setOnClickListener(v -> printPrescription());

        prescriptionHelper = new PrescriptionHelper(requireContext());
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

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
            tvPrescriptionId.setText(prescription.getPrescriptionId()
                    .substring(0, Math.min(12, prescription.getPrescriptionId().length()))
                    .toUpperCase(Locale.ROOT));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(prescription.getCreatedAt())));

        String patientName = prescription.getPatientName();
        tvPatientName.setText(patientName == null || patientName.trim().isEmpty()
                ? "Patient" : patientName.trim());

        tvDoctorName.setText(prescription.getDoctorName() != null ? prescription.getDoctorName() : "Doctor");
        tvDoctorSpecialty.setText("Prescribing clinician");

        tvInstructions.setText(prescription.getInstructions() != null
                ? prescription.getInstructions() : "No special instructions.");

        if (prescription.getImageUrl() != null && !prescription.getImageUrl().isEmpty()) {
            cardImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(prescription.getImageUrl())
                    .fitCenter()
                    .into(ivPrescriptionImage);
        } else {
            cardImage.setVisibility(View.GONE);
        }

        // IMPORTANT: use an auto-measuring LinearLayoutManager so ALL medicine rows
        // are laid out upfront — even those not visible in the scroll area.
        // This prevents blank rows appearing in the PDF bitmap capture.
        if (prescription.getMedicines() != null && !prescription.getMedicines().isEmpty()) {
            LinearLayoutManager llm = new LinearLayoutManager(requireContext()) {
                @Override
                public boolean isAutoMeasureEnabled() {
                    return true;
                }
            };
            rvMedicines.setLayoutManager(llm);
            rvMedicines.setNestedScrollingEnabled(false);
            rvMedicines.setAdapter(new MedicineAdapter(prescription.getMedicines()));
        }
    }

    // -------------------------------------------------------------------------
    // PDF Export
    //
    // Thread model:
    //   Main thread  → bitmap capture (View.draw() is not thread-safe)
    //   pdfExecutor  → paginate bitmap + disk I/O
    //   mainHandler  → post Snackbar result back to UI
    // -------------------------------------------------------------------------

    private void exportAsPdf() {
        if (prescription == null || prescriptionContent == null) return;

        if (prescriptionContent.getWidth() <= 0 || prescriptionContent.getHeight() <= 0) {
            showSnackbar("Layout not ready — please try again.");
            return;
        }

        showSnackbar("Generating PDF…");

        // Step 1 — capture bitmap on the main thread.
        prescriptionContent.post(() -> {
            View viewToCapture = isDarkModeActive() ? getPrintReadyView() : prescriptionContent;
            Bitmap bitmap = captureViewBitmap(viewToCapture);
            if (bitmap == null) {
                mainHandler.post(() -> {
                    if (isAdded()) showSnackbar("Failed to capture prescription content.");
                });
                return;
            }
            // Step 2 — all disk I/O on the background thread.
            pdfExecutor.execute(() -> writePdfToDisk(bitmap));
        });
    }

    /**
     * Renders the given view into a Bitmap, reading the full measured height
     * so off-screen content (e.g. long medicine lists) is not clipped.
     */
    private Bitmap captureViewBitmap(View view) {
        try {
            int width  = view.getWidth();
            int height = view.getMeasuredHeight(); // full content height, not just visible
            if (width <= 0 || height <= 0) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            view.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * Paginates the bitmap into A4 PDF pages and saves to the dedicated
     * HASET/Prescriptions folder (mirrors PrescriptionDetailFragment behaviour).
     *
     * Save paths:
     *   Android 10+  →  Download/HASET/Prescriptions/<file>.pdf  (MediaStore)
     *   Android 9-   →  /sdcard/HASET/Prescriptions/<file>.pdf
     *
     * Runs on pdfExecutor (background thread).
     */
    private void writePdfToDisk(Bitmap bitmap) {
        final int pageWidth     = 595;  // A4 @ 72 dpi
        final int pageHeight    = 842;
        final int margin        = 32;
        final int contentWidth  = pageWidth  - margin * 2;
        final int contentHeight = pageHeight - margin * 2;

        float scale        = contentWidth / (float) bitmap.getWidth();
        float scaledHeight = bitmap.getHeight() * scale;
        int   pageCount    = Math.max(1, (int) Math.ceil(scaledHeight / contentHeight));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        PdfDocument document = new PdfDocument();

        for (int i = 0; i < pageCount; i++) {
            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
            PdfDocument.Page page = document.startPage(info);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            canvas.save();
            canvas.clipRect(margin, margin, pageWidth - margin, pageHeight - margin);
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postTranslate(margin, margin - (i * (float) contentHeight));
            canvas.drawBitmap(bitmap, matrix, paint);
            canvas.restore();
            document.finishPage(page);
        }

        String fileName = "Prescription_" + prescriptionId + ".pdf";
        boolean saved   = false;
        Uri     savedUri = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — scoped storage via MediaStore.
            // RELATIVE_PATH must start with "Download/" — custom sub-folders are allowed inside it.
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, PRESCRIPTION_FOLDER_Q);
            savedUri = requireContext().getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (savedUri != null) {
                try (OutputStream os = requireContext().getContentResolver().openOutputStream(savedUri)) {
                    if (os != null) {
                        document.writeTo(os);
                        saved = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Android 9 and below: use app-private external files, not public /sdcard.
            File baseDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File dir = new File(baseDir != null ? baseDir : requireContext().getFilesDir(), "HASET/Prescriptions");
            if (!dir.exists() && !dir.mkdirs()) {
                dir = new File(requireContext().getFilesDir(), "prescriptions");
                dir.mkdirs();
            }
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                document.writeTo(fos);
                savedUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file);
                saved = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        document.close();
        bitmap.recycle();

        // Post result back to the UI thread.
        final boolean    finalSaved = saved;
        final Uri        finalUri   = savedUri;
        final String     displayPath = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? PRESCRIPTION_FOLDER_Q + "/" + fileName
                : PRESCRIPTION_FOLDER_LEGACY + "/" + fileName;

        mainHandler.post(() -> {
            if (!isAdded()) return;
            if (finalSaved && finalUri != null) {
                Snackbar.make(requireView(),
                        "Saved: " + displayPath,
                        Snackbar.LENGTH_LONG)
                        .setAction("OPEN", v -> {
                            Intent open = new Intent(Intent.ACTION_VIEW);
                            open.setDataAndType(finalUri, "application/pdf");
                            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(open, "Open PDF"));
                        })
                        .show();
            } else {
                showSnackbar("Failed to save PDF.");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Print
    //
    // PrintHelper.printBitmap() launches the Android system print dialog which
    // lets the user choose any available printer (physical, cloud, PDF).
    // Both the bitmap capture and the PrintHelper call must stay on the main
    // thread — PrintHelper internally posts to the UI handler.
    // -------------------------------------------------------------------------

    private void printPrescription() {
        if (prescriptionContent == null) return;

        View viewToCapture = isDarkModeActive() ? getPrintReadyView() : prescriptionContent;
        Bitmap bitmap = captureViewBitmap(viewToCapture);
        if (bitmap == null) {
            showSnackbar("Failed to capture prescription for printing.");
            return;
        }

        try {
            PrintHelper printHelper = new PrintHelper(requireActivity());
            // SCALE_MODE_FIT preserves aspect ratio within the page margins
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            // This triggers the Android print dialog:
            // — Google Cloud Print / network printers
            // — Save as PDF (built-in on Android 4.4+)
            // — Any printer app installed on the device (HP, Epson, Canon, etc.)
            printHelper.printBitmap("Prescription_" + prescriptionId, bitmap);
        } catch (Exception e) {
            // Fallback for devices where PrintHelper is unavailable:
            // share as image so the user can print from any app.
            shareBitmapAsImage(bitmap);
        }
    }

    /**
     * Fallback for devices where PrintHelper is unavailable — shares the
     * prescription image so the user can print from any third-party app.
     */
    private void shareBitmapAsImage(Bitmap bitmap) {
        try {
            File cacheDir = new File(requireContext().getCacheDir(), "prescription_previews");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File file = new File(cacheDir, "prescription_" + prescriptionId + ".png");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Print Prescription via…"));
        } catch (Exception ex) {
            showSnackbar("Could not open print options.");
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Shut down executor to prevent thread/memory leaks if the sheet is
        // dismissed while a PDF is still being written to disk.
        pdfExecutor.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isDarkModeActive() {
        if (getContext() == null) return false;
        int currentNightMode = getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private View getPrintReadyView() {
        if (prescription == null || getContext() == null) return prescriptionContent;

        try {
            // Force a light theme context wrapper
            Context context = requireContext();
            ContextThemeWrapper lightThemeContext = new ContextThemeWrapper(context, R.style.Theme_HASETApp);
            Configuration config = new Configuration(lightThemeContext.getResources().getConfiguration());
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
            Context lightModeContext = lightThemeContext.createConfigurationContext(config);

            // Inflate the exact same layout file under the light context
            View printView = LayoutInflater.from(lightModeContext).inflate(R.layout.bottom_sheet_prescription_details, null);
            View printContent = printView.findViewById(R.id.prescriptionContent);

            // Bind data to printContent views
            TextView tvPrescId = printView.findViewById(R.id.tvPrescriptionId);
            TextView tvDt = printView.findViewById(R.id.tvDate);
            TextView tvPatName = printView.findViewById(R.id.tvPatientName);
            TextView tvDocName = printView.findViewById(R.id.tvDoctorName);
            TextView tvDocSpec = printView.findViewById(R.id.tvDoctorSpecialty);
            TextView tvInst = printView.findViewById(R.id.tvInstructions);
            View cImage = printView.findViewById(R.id.cardImage);
            ImageView ivPrescImg = printView.findViewById(R.id.ivPrescriptionImage);
            RecyclerView rvMeds = printView.findViewById(R.id.rvMedicines);

            if (prescription.getPrescriptionId() != null) {
                tvPrescId.setText(prescription.getPrescriptionId()
                        .substring(0, Math.min(12, prescription.getPrescriptionId().length()))
                        .toUpperCase(Locale.ROOT));
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            tvDt.setText(sdf.format(new Date(prescription.getCreatedAt())));

            String patientName = prescription.getPatientName();
            tvPatName.setText(patientName == null || patientName.trim().isEmpty() ? "Patient" : patientName.trim());

            tvDocName.setText(prescription.getDoctorName() != null ? prescription.getDoctorName() : "Doctor");
            tvDocSpec.setText("Prescribing clinician");

            tvInst.setText(prescription.getInstructions() != null ? prescription.getInstructions() : "No special instructions.");

            if (cardImage != null && cardImage.getVisibility() == View.VISIBLE) {
                cImage.setVisibility(View.VISIBLE);
                if (ivPrescriptionImage != null) {
                    Drawable drawable = ivPrescriptionImage.getDrawable();
                    if (drawable != null) {
                        ivPrescImg.setImageDrawable(drawable);
                    }
                }
            } else {
                cImage.setVisibility(View.GONE);
            }

            if (prescription.getMedicines() != null && !prescription.getMedicines().isEmpty()) {
                LinearLayoutManager llm = new LinearLayoutManager(lightModeContext) {
                    @Override
                    public boolean isAutoMeasureEnabled() {
                        return true;
                    }
                };
                rvMeds.setLayoutManager(llm);
                rvMeds.setNestedScrollingEnabled(false);
                rvMeds.setAdapter(new MedicineAdapter(prescription.getMedicines()));
            }

            // Measure and layout the printContent view
            int width = prescriptionContent.getWidth();
            if (width <= 0) {
                width = Resources.getSystem().getDisplayMetrics().widthPixels;
            }

            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            printContent.measure(widthSpec, heightSpec);
            printContent.layout(0, 0, printContent.getMeasuredWidth(), printContent.getMeasuredHeight());

            return printContent;
        } catch (Exception e) {
            e.printStackTrace();
            return prescriptionContent; // Fallback to current view on any exception
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        } else if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
