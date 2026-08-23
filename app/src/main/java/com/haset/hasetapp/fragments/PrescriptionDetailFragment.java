package com.haset.hasetapp.fragments;

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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.haset.hasetapp.R;
import com.haset.hasetapp.models.Prescription;
import com.haset.hasetapp.utils.PreferenceManager;
import com.haset.hasetapp.utils.PrescriptionHelper;
import com.haset.hasetapp.viewmodels.PrescriptionViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment to display prescription details
 */
public class PrescriptionDetailFragment extends Fragment {

    // Single background thread dedicated to PDF generation.
    // Keeps disk I/O off the main thread without the overhead of a full thread pool.
    private final ExecutorService pdfExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler  = new Handler(Looper.getMainLooper());
    private ImageView ivPrescriptionImage;
    private TextView tvDoctorName, tvDate, tvInstructions;
    private RecyclerView recyclerViewMedicines;
    private MaterialButton btnDelete;
    
    private Prescription prescription;
    private PrescriptionViewModel viewModel;
    private PreferenceManager preferenceManager;
    private String prescriptionId;

    public static PrescriptionDetailFragment newInstance(String prescriptionId) {
        PrescriptionDetailFragment fragment = new PrescriptionDetailFragment();
        Bundle args = new Bundle();
        args.putString("prescription_id", prescriptionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prescription_detail, container, false);
        
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(PrescriptionViewModel.class);
        updateToolbar();
        initializeViews(view);
        loadPrescriptionData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        ivPrescriptionImage = view.findViewById(R.id.ivPrescriptionImage);
        tvDoctorName = view.findViewById(R.id.tvDoctorName);
        tvDate = view.findViewById(R.id.tvDate);
        tvInstructions = view.findViewById(R.id.tvInstructions);
        recyclerViewMedicines = view.findViewById(R.id.recyclerViewMedicines);
        btnDelete = view.findViewById(R.id.btnDelete);
        
        preferenceManager = new PreferenceManager(requireContext());
        
        // Get prescription from arguments
        if (getArguments() != null) {
            prescription = (Prescription) getArguments().getSerializable("prescription");
            prescriptionId = getArguments().getString("prescription_id");
            if (prescription != null && prescriptionId == null) {
                prescriptionId = prescription.getPrescriptionId();
            }
        }
        
        // Show delete button only for doctors
        String userRole = preferenceManager.getUserRole();
        if ("doctor".equalsIgnoreCase(userRole)) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        }
    }
    
    
    private void loadPrescriptionData() {
        if (prescriptionId == null) return;
        
        // Load from repository to get latest updates if any
        viewModel.getPrescriptionById(prescriptionId).observe(getViewLifecycleOwner(), p -> {
            if (p != null) {
                this.prescription = p;
                displayPrescription(p);
                updateToolbar();
            }
        });
    }

    private void displayPrescription(Prescription prescription) {
        // Load image
        if (prescription.getImageUrl() != null && !prescription.getImageUrl().isEmpty()) {
            viewImage(true);
            Glide.with(this)
                .load(prescription.getImageUrl())
                .placeholder(R.drawable.ic_prescription)
                .fitCenter()
                .into(ivPrescriptionImage);
        } else {
            viewImage(false);
        }
        
        // Set doctor name
        tvDoctorName.setText(prescription.getDoctorName());
        
        // Format and set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(new Date(prescription.getCreatedAt()));
        tvDate.setText(formattedDate);
        
        // Set instructions
        if (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty()) {
            tvInstructions.setText(prescription.getInstructions());
        } else {
            tvInstructions.setText(R.string.na);
        }
        
        // Setup medicines RecyclerView.
        // IMPORTANT: Use a LinearLayoutManager that auto-measures all children so that
        // every medicine row is fully laid out — even rows that are off-screen.
        // This prevents blank rows appearing in the PDF bitmap capture.
        if (prescription.getMedicines() != null && !prescription.getMedicines().isEmpty()) {
            LinearLayoutManager llm = new LinearLayoutManager(requireContext()) {
                @Override
                public boolean isAutoMeasureEnabled() {
                    return true; // Forces RecyclerView to measure ALL children upfront
                }
            };
            recyclerViewMedicines.setLayoutManager(llm);
            recyclerViewMedicines.setNestedScrollingEnabled(false); // Let parent ScrollView handle scrolling
            com.haset.hasetapp.adapters.MedicineAdapter adapter =
                    new com.haset.hasetapp.adapters.MedicineAdapter(prescription.getMedicines());
            recyclerViewMedicines.setAdapter(adapter);
        }
    }

    private void viewImage(boolean visible) {
        View parent = (View) ivPrescriptionImage.getParent();
        parent.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    
    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_prescription)
            .setMessage(R.string.confirm_delete_prescription)
            .setPositiveButton(R.string.delete, (dialog, which) -> deletePrescription())
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void deletePrescription() {
        if (prescription == null) return;
        
        viewModel.deletePrescription(prescription.getPrescriptionId(), new com.haset.hasetapp.utils.FirebaseHelper.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    com.google.android.material.snackbar.Snackbar.make(requireView(), R.string.prescription_deleted, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }
            }
            
            @Override
            public void onError(String error) {
                if (isAdded()) {
                    com.haset.hasetapp.utils.ErrorDisplay.report(requireView(), error);
                }
            }
        });
    }

    private void updateToolbar() {
        if (requireActivity() instanceof com.haset.hasetapp.activities.PrescriptionActivity) {
            com.haset.hasetapp.activities.PrescriptionActivity activity = (com.haset.hasetapp.activities.PrescriptionActivity) requireActivity();
            activity.setToolbarTitle(getString(R.string.prescription_details));
            // Always show the download button — generates a full PDF regardless of image
            activity.setDownloadButtonVisible(true, v -> generateAndDownloadPdf());
        }
    }

    // ---------------------------------------------------------------------------
    // PDF generation
    //
    // Thread split:
    //   Main thread  → bitmap capture (View.draw() is not thread-safe)
    //   pdfExecutor  → paginate bitmap + disk I/O (the expensive work)
    //   mainHandler  → post Snackbar result back to UI
    // ---------------------------------------------------------------------------
    private void generateAndDownloadPdf() {
        if (prescription == null) return;

        View rootView = requireView();
        if (rootView.getWidth() <= 0 || rootView.getHeight() <= 0) {
            com.google.android.material.snackbar.Snackbar.make(rootView,
                    "Layout not ready, please try again.",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Show an immediate "working" indicator on the UI thread.
        com.google.android.material.snackbar.Snackbar.make(rootView,
                "Generating PDF…",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();

        // Step 1 — capture bitmap on the main thread (View.draw() requirement).
        rootView.post(() -> {
            Bitmap bitmap = createBitmapFromScrollView(rootView);
            if (bitmap == null) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to capture prescription layout.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            // Step 2 — hand off bitmap to the background thread for all disk I/O.
            pdfExecutor.execute(() -> writePdfFromBitmap(bitmap));
        });
    }

    /** Captures the entire scrollable content of the given view into a Bitmap. */
    private Bitmap createBitmapFromScrollView(View view) {
        try {
            // If it's a ScrollView we need the full scrollable height.
            int width = view.getWidth();
            int height;
            if (view instanceof ScrollView) {
                ScrollView sv = (ScrollView) view;
                View child = sv.getChildAt(0);
                height = child != null ? child.getMeasuredHeight() : view.getHeight();
            } else {
                height = view.getHeight();
            }
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
     * Paginates the bitmap into A4-sized PDF pages.
     *
     * Save paths:
     *   Android 10+  → Internal Storage/HASET/Prescriptions/<filename>.pdf  (MediaStore)
     *   Android 9-   → app-private Documents/HASET/Prescriptions/<filename>.pdf
     */
    // Android 10+: MediaStore.Downloads enforces "Download/" as the required root.
    // The branded subfolder lives inside it: Download/HASET/Prescriptions/
    private static final String PRESCRIPTION_FOLDER_Q      = "Download/HASET/Prescriptions";
    private static final String PRESCRIPTION_FOLDER_LEGACY = "Documents/HASET/Prescriptions";

    private void writePdfFromBitmap(Bitmap bitmap) {
        final int pageWidth     = 595;   // A4 @ 72 dpi
        final int pageHeight    = 842;
        final int margin        = 32;
        final int contentWidth  = pageWidth  - margin * 2;
        final int contentHeight = pageHeight - margin * 2;

        float scale        = contentWidth / (float) bitmap.getWidth();
        float scaledHeight = bitmap.getHeight() * scale;
        int pageCount      = Math.max(1, (int) Math.ceil(scaledHeight / contentHeight));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        PdfDocument document = new PdfDocument();

        for (int i = 0; i < pageCount; i++) {
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
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

        String fileName = "Prescription_" + prescription.getPrescriptionId() + ".pdf";
        boolean saved   = false;
        Uri     savedUri = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Scoped storage via MediaStore.
            // RELATIVE_PATH creates the folder structure automatically if it doesn't exist.
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
                savedUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", file);
                saved = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        document.close();
        bitmap.recycle();

        // Post UI updates back to the main thread — this method runs on pdfExecutor.
        if (saved && savedUri != null) {
            final Uri finalUri = savedUri;
            final String savedFileName = fileName;
            // Build a human-readable path for the Snackbar message
            final String displayPath = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    ? PRESCRIPTION_FOLDER_Q + "/" + savedFileName
                    : PRESCRIPTION_FOLDER_LEGACY + "/" + savedFileName;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                com.google.android.material.snackbar.Snackbar
                        .make(requireView(),
                                "Saved: " + displayPath,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("OPEN", v -> {
                            Intent open = new Intent(Intent.ACTION_VIEW);
                            open.setDataAndType(finalUri, "application/pdf");
                            open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(open, "Open PDF"));
                        })
                        .show();
            });
        } else {
            mainHandler.post(() -> {
                if (!isAdded()) return;
                com.google.android.material.snackbar.Snackbar
                        .make(requireView(), "Failed to save PDF.",
                                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Shut down the PDF executor to prevent memory/thread leaks when the
        // fragment is destroyed (e.g. user navigates back mid-generation).
        pdfExecutor.shutdownNow();
        // Hide download button when leaving this fragment
        if (requireActivity() instanceof com.haset.hasetapp.activities.PrescriptionActivity) {
            ((com.haset.hasetapp.activities.PrescriptionActivity) requireActivity()).setDownloadButtonVisible(false, null);
            ((com.haset.hasetapp.activities.PrescriptionActivity) requireActivity()).setToolbarTitle(getString(R.string.prescriptions));
        }
    }
}
