    package com.haset.hasetapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.haset.hasetapp.R;
import android.app.Dialog;
import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.button.MaterialButton;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AppointmentsViewModel;
import com.haset.hasetapp.utils.PreferenceManager;

public class AppointmentsFragment extends Fragment {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ImageView btnMoreOptions;
    private String[] TAB_TITLES;
    private AppointmentsViewModel viewModel;
    private PreferenceManager preferenceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TAB_TITLES = new String[]{
                getString(R.string.upcoming_tab),
                getString(R.string.completed),
                getString(R.string.canceled)
        };
        tabLayout = view.findViewById(R.id.tabs);
        viewPager = view.findViewById(R.id.vpAppointmentTabs);
        btnMoreOptions = view.findViewById(R.id.btnMoreOptions);

        preferenceManager = new PreferenceManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentsViewModel.class);
        
        // Initialize user info so fragments can start loading immediately if they share the same info
        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        viewModel.setUserInfo(userId, role);

        viewPager.setAdapter(new AppointmentTabAdapter(requireActivity()));
        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> tab.setText(TAB_TITLES[pos])).attach();

        btnMoreOptions.setOnClickListener(v -> showMoreOptionsMenu());
    }

    private void exportAppointmentsAsCSV(List<com.haset.hasetapp.models.Appointment> appointments) {
        try {
            if (appointments == null || appointments.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_appointments_to_export, Toast.LENGTH_SHORT).show();
                return;
            }
            
            StringBuilder csv = new StringBuilder();
            csv.append("Doctor,Specialty,Date,Time,Status,Reason\n");
            for (com.haset.hasetapp.models.Appointment a : appointments) {
                // Separate Doctor and Specialty into different columns
                String doctorName = a.getDoctorName() != null ? a.getDoctorName() : getString(R.string.na);
                String specialty = a.getDoctorSpecialty() != null ? a.getDoctorSpecialty() : getString(R.string.na);
                String date = a.getDate() != null ? a.getDate() : getString(R.string.na);
                String time = a.getTime() != null ? a.getTime() : getString(R.string.na);
                String status = a.getStatus() != null ? a.getStatus() : getString(R.string.na);
                String reason = a.getReason() != null ? a.getReason().replace("\"", "'") : "";
                
                csv.append('"').append(doctorName).append('"').append(",")
                   .append('"').append(specialty).append('"').append(",")
                   .append('"').append(date).append('"').append(",")
                   .append('"').append(time).append('"').append(",")
                   .append('"').append(status).append('"').append(",")
                   .append('"').append(reason).append('"')
                   .append("\n");
            }
            
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) {
                Toast.makeText(getContext(), R.string.failed_to_access_storage, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "appointments_" + System.currentTimeMillis() + ".csv");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(csv.toString().getBytes());
            }
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.export_as_csv)));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getString(R.string.export_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    private void exportAppointmentsAsPDF(List<com.haset.hasetapp.models.Appointment> appointments) {
        try {
            if (appointments == null || appointments.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_appointments_to_export, Toast.LENGTH_SHORT).show();
                return;
            }
            
            PdfDocument pdfDocument = new PdfDocument();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();
            Paint textPaint = new Paint();
            Paint borderPaint = new Paint();
            
            titlePaint.setTextSize(24);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setColor(0xFF4CAF50);
            
            textPaint.setTextSize(9);
            textPaint.setColor(0xFF333333);
            
            borderPaint.setColor(0xFFDDDDDD);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1);
            
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 40;
            int yPosition = margin;
            int rowHeight = 22;
            
            String appName = getString(R.string.app_name);
            String reportTitle = "Appointments Report";
            String generatedDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            Bitmap logoBitmap = getLogoBitmap();
            if (logoBitmap != null) {
                int logoHeight = logoBitmap.getHeight();
                canvas.drawBitmap(logoBitmap, margin, yPosition + 5, null);
                canvas.drawText(appName, margin + logoBitmap.getWidth() + 15, yPosition + logoHeight - 5, titlePaint);
                yPosition += logoHeight + 20;
            } else {
                canvas.drawText(appName, margin, yPosition + 20, titlePaint);
                yPosition += 45;
            }
            
            canvas.drawText(reportTitle, margin, yPosition, textPaint);
            yPosition += 18;
            canvas.drawText("Generated: " + generatedDate, margin, yPosition, textPaint);
            yPosition += 15;
            canvas.drawText("Total Appointments: " + appointments.size(), margin, yPosition, textPaint);
            yPosition += 25;
            
            int[] colWidths = {55, 55, 45, 40, 35, 35, 55};
            String[] headers = {"Patient", "Doctor", "Specialty", "Date", "Time", "Status", "Notes"};
            
            headerPaint.setColor(0xFF4CAF50);
            headerPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + rowHeight, headerPaint);
            
            textPaint.setColor(0xFFFFFFFF);
            int xPosition = margin + 3;
            for (int i = 0; i < headers.length; i++) {
                canvas.drawText(headers[i], xPosition, yPosition + 16, textPaint);
                xPosition += colWidths[i];
            }
            yPosition += rowHeight;
            
            textPaint.setColor(0xFF333333);
            
            for (com.haset.hasetapp.models.Appointment apt : appointments) {
                if (yPosition > pageHeight - margin - 30) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = margin;
                }
                
                String[] rowData = {
                    truncateText(apt.getPatientName(), 9),
                    truncateText(apt.getDoctorName(), 9),
                    truncateText(apt.getDoctorSpecialty(), 7),
                    truncateText(apt.getDate(), 6),
                    truncateText(apt.getTime(), 5),
                    truncateText(apt.getStatus(), 5),
                    truncateText(apt.getReason() != null ? apt.getReason() : "", 9)
                };
                
                xPosition = margin + 3;
                for (int i = 0; i < rowData.length; i++) {
                    canvas.drawText(rowData[i], xPosition, yPosition + 16, textPaint);
                    xPosition += colWidths[i];
                }
                
                canvas.drawLine(margin, yPosition + rowHeight, pageWidth - margin, yPosition + rowHeight, borderPaint);
                yPosition += rowHeight;
            }
            
            pdfDocument.finishPage(page);
            
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) {
                Toast.makeText(getContext(), R.string.failed_to_access_storage, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(dir, "appointments_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
            }
            pdfDocument.close();
            
            showExportResultBottomSheet(file, "application/pdf");
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getString(R.string.export_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    private Bitmap getLogoBitmap() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.haset_logo);
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.haset_logo2);
            }
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.haset_logo_icon);
            }
            if (bitmap != null) {
                int maxSize = 50;
                if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                    float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                    int newWidth = Math.round(bitmap.getWidth() * scale);
                    int newHeight = Math.round(bitmap.getHeight() * scale);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }
    
    private void showExportResultBottomSheet(File file, String mimeType) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_export_result, null);
        bottomSheetDialog.setContentView(view);

        TextView tvFileName = view.findViewById(R.id.tvFileName);
        TextView tvFilePath = view.findViewById(R.id.tvFilePath);
        MaterialButton btnOpenLocation = view.findViewById(R.id.btnOpenLocation);
        MaterialButton btnShare = view.findViewById(R.id.btnShare);

        tvFileName.setText(file.getName());
        tvFilePath.setText(file.getAbsolutePath());

        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);

        btnOpenLocation.setOnClickListener(v -> {
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, mimeType);
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(openIntent);
            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.no_app_to_open_file, Toast.LENGTH_SHORT).show();
            }
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
        });

        view.setOnClickListener(v -> bottomSheetDialog.dismiss());
        
        bottomSheetDialog.show();
    }

    private List<com.haset.hasetapp.models.Appointment> getCurrentTabAppointments() {
        int tab = viewPager.getCurrentItem();
        Fragment page = getChildFragmentManager().findFragmentByTag("f" + tab);
        if (page instanceof UpcomingAppointmentsFragment) {
            return ((UpcomingAppointmentsFragment) page).getCurrentAppointments();
        } else if (page instanceof PastAppointmentsFragment) {
            return ((PastAppointmentsFragment) page).getCurrentAppointments();
        } else if (page instanceof CancelledAppointmentsFragment) {
            return ((CancelledAppointmentsFragment) page).getCurrentAppointments();
        }
        return java.util.Collections.emptyList();
    }

    private void showExportOptionsDialog() {
        // Only get completed appointments from ViewModel
        viewModel.getPastAppointments().observe(getViewLifecycleOwner(), completed -> {
            // Log for debugging
            android.util.Log.d("AppointmentsFragment", "Export - Completed: " + (completed != null ? completed.size() : 0));
            
            if (completed == null || completed.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_completed_appointments_export, Toast.LENGTH_SHORT).show();
                return;
            }
            
            ExportAppointmentsBottomSheet sheet = new ExportAppointmentsBottomSheet(completed, selected -> {
                if (selected.isEmpty()) {
                    Toast.makeText(getContext(), R.string.select_at_least_one, Toast.LENGTH_SHORT).show();
                    return;
                }
                exportAppointmentsAsCSV(selected);
            });
            sheet.show(requireActivity().getSupportFragmentManager(), "exportAppointmentsSheet");
            
            // Remove observer after one shot to avoid multiple dialogs if data changes
            viewModel.getPastAppointments().removeObservers(getViewLifecycleOwner());
        });
    }
        private List<com.haset.hasetapp.models.Appointment> getTabAppointments(int tab) {
        // ViewPager2 uses different fragment tag format, so we need to find fragments by type
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (tab == 0 && fragment instanceof UpcomingAppointmentsFragment) {
                return ((UpcomingAppointmentsFragment) fragment).getCurrentAppointments();
            } else if (tab == 1 && fragment instanceof PastAppointmentsFragment) {
                return ((PastAppointmentsFragment) fragment).getCurrentAppointments();
            } else if (tab == 2 && fragment instanceof CancelledAppointmentsFragment) {
                return ((CancelledAppointmentsFragment) fragment).getCurrentAppointments();
            }
        }
        
        // Fallback: Try to get from ViewPager2 adapter item ID
        try {
            if (viewPager != null && viewPager.getAdapter() != null) {
                long itemId = viewPager.getAdapter().getItemId(tab);
                Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + itemId);
                if (fragment != null) {
                    if (tab == 0 && fragment instanceof UpcomingAppointmentsFragment) {
                        return ((UpcomingAppointmentsFragment) fragment).getCurrentAppointments();
                    } else if (tab == 1 && fragment instanceof PastAppointmentsFragment) {
                        return ((PastAppointmentsFragment) fragment).getCurrentAppointments();
                    } else if (tab == 2 && fragment instanceof CancelledAppointmentsFragment) {
                        return ((CancelledAppointmentsFragment) fragment).getCurrentAppointments();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return java.util.Collections.emptyList();
    }

    // BottomSheet dialog for exporting selected appointments
    public static class ExportAppointmentsBottomSheet extends androidx.fragment.app.DialogFragment {
        private List<com.haset.hasetapp.models.Appointment> completedAppointments;
        private final ExportListener exportListener;
        private final HashSet<Integer> selected = new HashSet<>();

        public interface ExportListener {
            void onExport(List<com.haset.hasetapp.models.Appointment> selected);
    }

        public ExportAppointmentsBottomSheet(List<com.haset.hasetapp.models.Appointment> completed, ExportListener listener) {
            // Only accept completed appointments
            this.completedAppointments = completed != null ? completed : new java.util.ArrayList<>();
            this.exportListener = listener;
    }

    @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            BottomSheetDialog dialog = new BottomSheetDialog(getContext(), getTheme());
            View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_export_appointments, null, false);
            RecyclerView rv = sheetView.findViewById(R.id.rvSelectAppointments);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            
            // Ensure completed appointments list is not null
            if (completedAppointments == null) {
                completedAppointments = new java.util.ArrayList<>();
            }
            
            // Create a mutable reference to the adapter
            final AppointmentSelectAdapter[] adapterRef = new AppointmentSelectAdapter[1];
            
            // Create the listener that uses the adapter reference
            AppointmentSelectAdapter.OnCheckedListener checkedListener = pos -> {
                if (selected.contains(pos)) {
                    selected.remove(pos);
                } else {
                    selected.add(pos);
                }
                if (adapterRef[0] != null) {
                    adapterRef[0].notifyItemChanged(pos);
                }
            };
            
            // Create the adapter with completed appointments only
            AppointmentSelectAdapter adapter = new AppointmentSelectAdapter(completedAppointments, checkedListener, selected);
            adapterRef[0] = adapter;
            rv.setAdapter(adapter);
            
            MaterialButton btnExport = sheetView.findViewById(R.id.btnExportSelected);
            MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancelExport);
            
            btnExport.setOnClickListener(v -> {
                List<com.haset.hasetapp.models.Appointment> chosen = new java.util.ArrayList<>();
                for (int i : selected) {
                    if (i >= 0 && i < completedAppointments.size()) {
                        chosen.add(completedAppointments.get(i));
                    }
                }
                exportListener.onExport(chosen);
                dialog.dismiss();
            });
            
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            
            dialog.setContentView(sheetView);
            return dialog;
        }

        // Simple Adapter for selecting appointments
        static class AppointmentSelectAdapter extends RecyclerView.Adapter<AppointmentSelectAdapter.VH> {
            private List<com.haset.hasetapp.models.Appointment> data;
            private final OnCheckedListener checkedListener;
            private final HashSet<Integer> selected;
            public interface OnCheckedListener { void onChecked(int pos); }
            public AppointmentSelectAdapter(List<com.haset.hasetapp.models.Appointment> data, OnCheckedListener listener, HashSet<Integer> selected) {
                this.data = data; this.checkedListener = listener; this.selected = selected;
            }
            static class VH extends RecyclerView.ViewHolder {
                CheckBox checkBox;
                TextView tvName, tvDetails, tvStatus;
                VH(View v) {
                    super(v);
                    checkBox = v.findViewById(R.id.chkSelectAppt);
                    tvName = v.findViewById(R.id.tvApptMain);
                    tvDetails = v.findViewById(R.id.tvApptDetails);
                    tvStatus = v.findViewById(R.id.tvApptStatus);
                }
            }
    @Override
            public VH onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_appointment, parent, false);
                return new VH(v);
    }
            @Override
            public void onBindViewHolder(VH holder, int pos) {
                if (data == null || pos < 0 || pos >= data.size()) {
                    return;
                }
                com.haset.hasetapp.models.Appointment a = data.get(pos);
                if (a == null) {
                    return;
                }
                
                String doctorName = a.getDoctorName() != null ? a.getDoctorName() : holder.itemView.getContext().getString(R.string.unknown);
                String specialty = a.getDoctorSpecialty() != null ? a.getDoctorSpecialty() : "";
                String date = a.getDate() != null ? a.getDate() : "";
                String time = a.getTime() != null ? a.getTime() : "";
                String status = a.getStatus() != null ? a.getStatus().toUpperCase() : "";
                
                holder.tvName.setText(doctorName + (specialty.isEmpty() ? "" : " • " + specialty));
                holder.tvDetails.setText(date + " " + time);
                holder.tvStatus.setText(status);

                holder.itemView.setOnClickListener(v -> { 
                    checkedListener.onChecked(pos); 
                    notifyItemChanged(pos); 
                });

                // Remove previous listener to avoid conflicts
                holder.checkBox.setOnCheckedChangeListener(null);
                holder.checkBox.setChecked(selected.contains(pos));
                holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> { 
                    checkedListener.onChecked(pos); 
                    notifyItemChanged(pos); 
                });
            }
            @Override
            public int getItemCount() { 
                return data != null ? data.size() : 0; 
            }
            public void setData(List<com.haset.hasetapp.models.Appointment> d) { this.data = d; notifyDataSetChanged(); }
        }
    }

    private void showMoreOptionsMenu() {
        if (getContext() == null || btnMoreOptions == null) return;
        PopupMenu popup = new PopupMenu(getContext(), btnMoreOptions);
        popup.getMenuInflater().inflate(R.menu.menu_appointment_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export) {
                showExportFormatDialog();
                return true;
            } else if (id == R.id.action_settings) {
                Toast.makeText(getContext(), R.string.appointment_settings_clicked, Toast.LENGTH_SHORT).show();
                // TODO: Launch settings
                return true;
            } else if (id == R.id.action_refresh) {
                refreshAllTabs();
                return true;
            }
            return false;
        });
        popup.show();
    }
    
    private void showExportFormatDialog() {
        String[] formats = {"CSV", "PDF"};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_export_format)
                .setItems(formats, (dialog, which) -> {
                    if (which == 0) {
                        // CSV export
                        showExportOptionsDialog();
                    } else if (which == 1) {
                        // PDF export
                        showExportOptionsDialogForPDF();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showExportOptionsDialogForPDF() {
        // Only get completed appointments from ViewModel
        viewModel.getPastAppointments().observe(getViewLifecycleOwner(), completed -> {
            if (completed == null || completed.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_completed_appointments_to_export, Toast.LENGTH_SHORT).show();
                return;
            }
            
            ExportAppointmentsBottomSheet sheet = new ExportAppointmentsBottomSheet(completed, selected -> {
                if (selected.isEmpty()) {
                    Toast.makeText(getContext(), R.string.select_at_least_one, Toast.LENGTH_SHORT).show();
                    return;
                }
                exportAppointmentsAsPDF(selected);
            });
            sheet.show(requireActivity().getSupportFragmentManager(), "exportAppointmentsSheet");
            
            // Remove observer
            viewModel.getPastAppointments().removeObservers(getViewLifecycleOwner());
        });
    }
    
    private void refreshAllTabs() {
        // Just reset user info in ViewModel, it will trigger refresh in all observers
        String userId = preferenceManager.getUserId();
        String role = preferenceManager.getUserRole();
        viewModel.setUserInfo(userId, role);
        
        Toast.makeText(getContext(), R.string.refreshing_appointments, Toast.LENGTH_SHORT).show();
    }

    static class AppointmentTabAdapter extends FragmentStateAdapter {
        public AppointmentTabAdapter(FragmentActivity fa) { super(fa); }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new UpcomingAppointmentsFragment();
                case 1: return new PastAppointmentsFragment();
                case 2: return new CancelledAppointmentsFragment();
                default: return new UpcomingAppointmentsFragment();
            }
        }
        @Override
        public int getItemCount() { return 3; }
    }
    @Override
    public void onDestroyView() {
        // Clear ViewPager adapter safely - post to avoid FragmentManager conflict
        if (viewPager != null) {
            viewPager.post(() -> {
                if (viewPager != null) {
                    viewPager.setAdapter(null);
                }
            });
        }
        
        // Null out view references
        viewPager = null;
        tabLayout = null;
        btnMoreOptions = null;
        
        super.onDestroyView();
    }
}
