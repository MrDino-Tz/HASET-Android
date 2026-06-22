package com.haset.hasetapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.haset.hasetapp.R;
import com.haset.hasetapp.adapters.ReportTypeAdapter;
// import com.haset.hasetapp.database.LocalStorageHelper;
import com.haset.hasetapp.database.entities.UserEntity;
import com.haset.hasetapp.models.Appointment;
import com.haset.hasetapp.models.AuditLog;
import com.haset.hasetapp.utils.AuditLogger;
import com.haset.hasetapp.utils.PreferenceManager;
import androidx.lifecycle.ViewModelProvider;
import com.haset.hasetapp.viewmodels.AdminReportViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReportActivity extends AppCompatActivity {
    private ImageView btnBack;
    private RecyclerView rvReportTypes;
    private MaterialButton btnGenerateReport;
    private ReportTypeAdapter reportTypeAdapter;
    private PreferenceManager preferenceManager;
    private String selectedReportType;
    private String selectedFormat;
    private AdminReportViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        
        viewModel = new ViewModelProvider(this).get(AdminReportViewModel.class);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        rvReportTypes = findViewById(R.id.rvReportTypes);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        
        preferenceManager = new PreferenceManager(this);
        
        btnBack.setOnClickListener(v -> finish());
        btnGenerateReport.setOnClickListener(v -> showFormatSelectionDialog());
    }

    private void setupRecyclerView() {
        List<ReportTypeAdapter.ReportType> reportTypes = new ArrayList<>();
        reportTypes.add(new ReportTypeAdapter.ReportType("Users List", "Export list of all users with details", R.drawable.ic_people));
        reportTypes.add(new ReportTypeAdapter.ReportType("App Statistics", "Export statistical data of app usage", R.drawable.ic_chart));
        reportTypes.add(new ReportTypeAdapter.ReportType("Appointments", "Export all appointments data", R.drawable.ic_calendar));
        reportTypes.add(new ReportTypeAdapter.ReportType("Audit Logs", "Export system audit logs", R.drawable.ic_history));
        
        reportTypeAdapter = new ReportTypeAdapter(reportTypes, reportType -> {
            selectedReportType = reportType.getTitle();
            btnGenerateReport.setEnabled(true);
        });
        
        rvReportTypes.setLayoutManager(new LinearLayoutManager(this));
        rvReportTypes.setAdapter(reportTypeAdapter);
    }

    private void showFormatSelectionDialog() {
        if (selectedReportType == null) {
            Toast.makeText(this, R.string.please_select_report_type, Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Export Format");
        String[] formats = {"CSV", "XLS", "PDF"};
        builder.setItems(formats, (dialog, which) -> {
            selectedFormat = formats[which];
            generateReport(selectedReportType, selectedFormat);
        });
        builder.show();
    }

    private void generateReport(String reportType, String format) {
        switch (reportType) {
            case "Users List":
                generateUsersReport(format);
                break;
            case "App Statistics":
                generateStatisticsReport(format);
                break;
            case "Appointments":
                generateAppointmentsReport(format);
                break;
            case "Audit Logs":
                generateAuditLogsReport(format);
                break;
        }
    }

    private void generateUsersReport(String format) {
        viewModel.getAllUsers().observe(this, users -> {
            if (users != null) {
                if (format.equals("CSV")) {
                    exportUsersAsCSV(users);
                } else if (format.equals("XLS")) {
                    exportUsersAsXLS(users);
                } else if (format.equals("PDF")) {
                    exportUsersAsPDF(users);
                }
                AuditLogger.getInstance(AdminReportActivity.this).logDataExport("Users", users.size());
            } else {
                Toast.makeText(AdminReportActivity.this, R.string.failed_to_load_users, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateStatisticsReport(String format) {
        viewModel.getAllUsers().observe(this, users -> {
            if (users != null) {
                viewModel.getAllAppointments().observe(this, appointmentEntities -> {
                    if (appointmentEntities != null) {
                        List<Appointment> appointments = new ArrayList<>();
                        for (com.haset.hasetapp.database.entities.AppointmentEntity entity : appointmentEntities) {
                            appointments.add(new Appointment(entity));
                        }
                        
                        if (format.equals("CSV")) {
                            exportStatisticsAsCSV(users, appointments);
                        } else if (format.equals("XLS")) {
                            exportStatisticsAsXLS(users, appointments);
                        } else if (format.equals("PDF")) {
                            exportStatisticsAsPDF(users, appointments);
                        }
                        AuditLogger.getInstance(AdminReportActivity.this).logDataExport("Statistics", 1);
                    } else {
                        Toast.makeText(AdminReportActivity.this, R.string.failed_to_load_appointments, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(AdminReportActivity.this, R.string.failed_to_load_users, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateAppointmentsReport(String format) {
        viewModel.getAllAppointments().observe(this, appointmentEntities -> {
            if (appointmentEntities != null) {
                List<Appointment> appointments = new ArrayList<>();
                for (com.haset.hasetapp.database.entities.AppointmentEntity entity : appointmentEntities) {
                    appointments.add(new Appointment(entity));
                }
                
                if (format.equals("CSV")) {
                    exportAppointmentsAsCSV(appointments);
                } else if (format.equals("XLS")) {
                    exportAppointmentsAsXLS(appointments);
                } else if (format.equals("PDF")) {
                    exportAppointmentsAsPDF(appointments);
                }
                AuditLogger.getInstance(AdminReportActivity.this).logDataExport("Appointments", appointments.size());
            } else {
                Toast.makeText(AdminReportActivity.this, R.string.failed_to_load_appointments, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateAuditLogsReport(String format) {
        viewModel.getAuditLogs().observe(this, auditLogEntities -> {
            if (auditLogEntities != null) {
                List<AuditLog> auditLogs = new ArrayList<>();
                for (com.haset.hasetapp.database.entities.AuditLogEntity entity : auditLogEntities) {
                    AuditLog log = new AuditLog();
                    log.setLogId(entity.getLogId());
                    log.setUserId(entity.getUserId());
                    log.setUserName(entity.getUserName());
                    log.setUserRole(entity.getUserRole());
                    log.setAction(entity.getAction());
                    log.setDescription(entity.getDescription());
                    log.setEntityType(entity.getEntityType());
                    log.setEntityId(entity.getEntityId());
                    log.setTimestamp(entity.getTimestamp());
                    log.setIpAddress(entity.getIpAddress());
                    log.setDeviceInfo(entity.getDeviceInfo());
                    auditLogs.add(log);
                }
                
                if (format.equals("CSV")) {
                    exportAuditLogsAsCSV(auditLogs);
                } else if (format.equals("XLS")) {
                    exportAuditLogsAsXLS(auditLogs);
                } else if (format.equals("PDF")) {
                    exportAuditLogsAsPDF(auditLogs);
                }
                AuditLogger.getInstance(AdminReportActivity.this).logDataExport("Audit Logs", auditLogs.size());
            } else {
                Toast.makeText(AdminReportActivity.this, R.string.failed_to_load_audit_logs, Toast.LENGTH_LONG).show();
            }
        });
    }

    // CSV Export Methods
    private void exportUsersAsCSV(List<UserEntity> users) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("User ID,Name,Email,Phone,Role,Created Date\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (UserEntity user : users) {
                csv.append('"').append(user.getUserId()).append('"').append(",")
                   .append('"').append(user.getFullName()).append('"').append(",")
                   .append('"').append(user.getEmail()).append('"').append(",")
                   .append('"').append(user.getPhone() != null ? user.getPhone() : "").append('"').append(",")
                   .append('"').append(user.getRole()).append('"').append(",")
                   .append('"').append(sdf.format(new Date(user.getCreatedAt()))).append('"')
                   .append("\n");
            }
            shareFile(csv.toString(), "users_report.csv", "text/csv");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportStatisticsAsCSV(List<UserEntity> users, List<Appointment> appointments) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Report Type,Value\n");
            csv.append("Total Users,").append(users.size()).append("\n");
            
            int doctors = 0, patients = 0;
            for (UserEntity user : users) {
                if ("doctor".equals(user.getRole())) doctors++;
                else if ("patient".equals(user.getRole())) patients++;
            }
            csv.append("Total Doctors,").append(doctors).append("\n");
            csv.append("Total Patients,").append(patients).append("\n");
            csv.append("Total Appointments,").append(appointments.size()).append("\n");
            
            int pending = 0, approved = 0, declined = 0, completed = 0;
            for (Appointment apt : appointments) {
                switch (apt.getStatus()) {
                    case "pending": pending++; break;
                    case "approved": approved++; break;
                    case "declined": declined++; break;
                    case "completed": completed++; break;
                }
            }
            csv.append("Pending Appointments,").append(pending).append("\n");
            csv.append("Approved Appointments,").append(approved).append("\n");
            csv.append("Declined Appointments,").append(declined).append("\n");
            csv.append("Completed Appointments,").append(completed).append("\n");
            csv.append("Report Generated,").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            
            shareFile(csv.toString(), "statistics_report.csv", "text/csv");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportAppointmentsAsCSV(List<Appointment> appointments) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Appointment ID,Patient Name,Doctor Name,Date,Time,Status,Reason\n");
            for (Appointment apt : appointments) {
                csv.append('"').append(apt.getAppointmentId()).append('"').append(",")
                   .append('"').append(apt.getPatientName()).append('"').append(",")
                   .append('"').append(apt.getDoctorName()).append('"').append(",")
                   .append('"').append(apt.getDate()).append('"').append(",")
                   .append('"').append(apt.getTime()).append('"').append(",")
                   .append('"').append(apt.getStatus()).append('"').append(",")
                   .append('"').append(apt.getReason() != null ? apt.getReason().replace("\"", "'") : "").append('"')
                   .append("\n");
            }
            shareFile(csv.toString(), "appointments_report.csv", "text/csv");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportAuditLogsAsCSV(List<AuditLog> auditLogs) {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Log ID,User Name,User Role,Action,Description,Entity Type,Timestamp\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (AuditLog log : auditLogs) {
                csv.append('"').append(log.getLogId()).append('"').append(",")
                   .append('"').append(log.getUserName()).append('"').append(",")
                   .append('"').append(log.getUserRole()).append('"').append(",")
                   .append('"').append(log.getAction()).append('"').append(",")
                   .append('"').append(log.getDescription().replace("\"", "'")).append('"').append(",")
                   .append('"').append(log.getEntityType() != null ? log.getEntityType() : "").append('"').append(",")
                   .append('"').append(sdf.format(new Date(log.getTimestamp()))).append('"')
                   .append("\n");
            }
            shareFile(csv.toString(), "audit_logs_report.csv", "text/csv");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // XLS Export Methods (Excel XML format)
    private void exportUsersAsXLS(List<UserEntity> users) {
        try {
            StringBuilder xls = new StringBuilder();
            xls.append("<?xml version=\"1.0\"?>\n");
            xls.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xls.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xls.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
            xls.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
            xls.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            xls.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");
            xls.append("<Worksheet ss:Name=\"Users\">\n");
            xls.append("<Table>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">User ID</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Name</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Email</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Phone</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Role</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Created Date</Data></Cell></Row>\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (UserEntity user : users) {
                xls.append("<Row>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(user.getUserId()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(user.getFullName()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(user.getEmail()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(user.getPhone() != null ? user.getPhone() : "").append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(user.getRole()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(sdf.format(new Date(user.getCreatedAt()))).append("</Data></Cell>");
                xls.append("</Row>\n");
            }
            xls.append("</Table></Worksheet></Workbook>");
            shareFile(xls.toString(), "users_report.xls", "application/vnd.ms-excel");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportStatisticsAsXLS(List<UserEntity> users, List<Appointment> appointments) {
        try {
            StringBuilder xls = new StringBuilder();
            xls.append("<?xml version=\"1.0\"?>\n");
            xls.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xls.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
            xls.append("<Worksheet ss:Name=\"Statistics\">\n");
            xls.append("<Table>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Metric</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Value</Data></Cell></Row>\n");
            
            int doctors = 0, patients = 0;
            for (UserEntity user : users) {
                if ("doctor".equals(user.getRole())) doctors++;
                else if ("patient".equals(user.getRole())) patients++;
            }
            
            int pending = 0, approved = 0, declined = 0, completed = 0;
            for (Appointment apt : appointments) {
                switch (apt.getStatus()) {
                    case "pending": pending++; break;
                    case "approved": approved++; break;
                    case "declined": declined++; break;
                    case "completed": completed++; break;
                }
            }
            
            xls.append("<Row><Cell><Data ss:Type=\"String\">Total Users</Data></Cell><Cell><Data ss:Type=\"Number\">").append(users.size()).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Total Doctors</Data></Cell><Cell><Data ss:Type=\"Number\">").append(doctors).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Total Patients</Data></Cell><Cell><Data ss:Type=\"Number\">").append(patients).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Total Appointments</Data></Cell><Cell><Data ss:Type=\"Number\">").append(appointments.size()).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Pending Appointments</Data></Cell><Cell><Data ss:Type=\"Number\">").append(pending).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Approved Appointments</Data></Cell><Cell><Data ss:Type=\"Number\">").append(approved).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Declined Appointments</Data></Cell><Cell><Data ss:Type=\"Number\">").append(declined).append("</Data></Cell></Row>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Completed Appointments</Data></Cell><Cell><Data ss:Type=\"Number\">").append(completed).append("</Data></Cell></Row>\n");
            xls.append("</Table></Worksheet></Workbook>");
            shareFile(xls.toString(), "statistics_report.xls", "application/vnd.ms-excel");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportAppointmentsAsXLS(List<Appointment> appointments) {
        try {
            StringBuilder xls = new StringBuilder();
            xls.append("<?xml version=\"1.0\"?>\n");
            xls.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xls.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
            xls.append("<Worksheet ss:Name=\"Appointments\">\n");
            xls.append("<Table>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">Patient</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Doctor</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Date</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Time</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Status</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Reason</Data></Cell></Row>\n");
            for (Appointment apt : appointments) {
                xls.append("<Row>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(apt.getPatientName()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(apt.getDoctorName()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(apt.getDate()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(apt.getTime()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(apt.getStatus()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(apt.getReason() != null ? apt.getReason() : "").append("</Data></Cell>");
                xls.append("</Row>\n");
            }
            xls.append("</Table></Worksheet></Workbook>");
            shareFile(xls.toString(), "appointments_report.xls", "application/vnd.ms-excel");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportAuditLogsAsXLS(List<AuditLog> auditLogs) {
        try {
            StringBuilder xls = new StringBuilder();
            xls.append("<?xml version=\"1.0\"?>\n");
            xls.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
            xls.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
            xls.append("<Worksheet ss:Name=\"Audit Logs\">\n");
            xls.append("<Table>\n");
            xls.append("<Row><Cell><Data ss:Type=\"String\">User</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Role</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Action</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Description</Data></Cell>");
            xls.append("<Cell><Data ss:Type=\"String\">Timestamp</Data></Cell></Row>\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            for (AuditLog log : auditLogs) {
                xls.append("<Row>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(log.getUserName()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(log.getUserRole()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(log.getAction()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(log.getDescription()).append("</Data></Cell>");
                xls.append("<Cell><Data ss:Type=\"String\">").append(sdf.format(new Date(log.getTimestamp()))).append("</Data></Cell>");
                xls.append("</Row>\n");
            }
            xls.append("</Table></Worksheet></Workbook>");
            shareFile(xls.toString(), "audit_logs_report.xls", "application/vnd.ms-excel");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // PDF Export Methods - Using Android's PdfDocument API for true PDF generation
    private void exportUsersAsPDF(List<UserEntity> users) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();
            Paint textPaint = new Paint();
            Paint borderPaint = new Paint();
            
            titlePaint.setTextSize(24);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setColor(0xFF4CAF50);
            
            headerPaint.setTextSize(12);
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setColor(0xFFFFFFFF);
            headerPaint.setStyle(Paint.Style.FILL);
            
            textPaint.setTextSize(10);
            textPaint.setColor(0xFF333333);
            
            borderPaint.setColor(0xFFDDDDDD);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1);
            
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 40;
            int yPosition = margin;
            int rowHeight = 25;
            
            String appName = getString(R.string.app_name);
            String reportTitle = "Users Report";
            String generatedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            Bitmap logoBitmap = getLogoBitmap();
            int logoWidth = 0;
            if (logoBitmap != null) {
                logoWidth = logoBitmap.getWidth();
                int logoHeight = logoBitmap.getHeight();
                int logoX = margin;
                int logoY = yPosition + 5;
                canvas.drawBitmap(logoBitmap, logoX, logoY, null);
                canvas.drawText(appName, margin + logoWidth + 15, yPosition + logoHeight - 10, titlePaint);
                yPosition += logoHeight + 20;
            } else {
                canvas.drawText(appName, margin, yPosition + 20, titlePaint);
                yPosition += 45;
            }
            canvas.drawText(reportTitle, margin, yPosition, textPaint);
            yPosition += 18;
            canvas.drawText("Generated: " + generatedDate, margin, yPosition, textPaint);
            yPosition += 30;
            
            int[] colWidths = {80, 90, 120, 80, 50, 80};
            String[] headers = {"User ID", "Name", "Email", "Phone", "Role", "Created Date"};
            
            headerPaint.setColor(0xFF4CAF50);
            canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + rowHeight, headerPaint);
            
            int xPosition = margin + 5;
            for (int i = 0; i < headers.length; i++) {
                canvas.drawText(headers[i], xPosition, yPosition + 17, textPaint);
                xPosition += colWidths[i];
            }
            yPosition += rowHeight;
            
            headerPaint.setColor(0xFFFFFFFF);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            for (UserEntity user : users) {
                if (yPosition > pageHeight - margin - 30) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = margin;
                }
                
                String[] rowData = {
                    truncateText(user.getUserId(), 12),
                    truncateText(user.getFullName(), 14),
                    truncateText(user.getEmail(), 18),
                    truncateText(user.getPhone() != null ? user.getPhone() : "", 12),
                    truncateText(user.getRole(), 8),
                    sdf.format(new Date(user.getCreatedAt()))
                };
                
                xPosition = margin + 5;
                for (int i = 0; i < rowData.length; i++) {
                    canvas.drawText(rowData[i], xPosition, yPosition + 17, textPaint);
                    xPosition += colWidths[i];
                }
                
                canvas.drawLine(margin, yPosition + rowHeight, pageWidth - margin, yPosition + rowHeight, borderPaint);
                yPosition += rowHeight;
            }
            
            pdfDocument.finishPage(page);
            
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null || !dir.exists()) {
                dir = new File(getFilesDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
            }
            
            File file = new File(dir, "users_report.pdf");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
            }
            pdfDocument.close();
            
            sharePdfFile(file, "Users Report");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportStatisticsAsPDF(List<UserEntity> users, List<Appointment> appointments) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();
            Paint textPaint = new Paint();
            
            titlePaint.setTextSize(24);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setColor(0xFF4CAF50);
            
            headerPaint.setTextSize(12);
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setColor(0xFFFFFFFF);
            headerPaint.setStyle(Paint.Style.FILL);
            
            textPaint.setTextSize(11);
            textPaint.setColor(0xFF333333);
            
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 40;
            int yPosition = margin;
            int rowHeight = 25;
            
            String appName = getString(R.string.app_name);
            String reportTitle = "App Statistics Report";
            String generatedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            Bitmap logoBitmap = getLogoBitmap();
            int logoWidth = 0;
            if (logoBitmap != null) {
                logoWidth = logoBitmap.getWidth();
                int logoHeight = logoBitmap.getHeight();
                int logoX = margin;
                int logoY = yPosition + 5;
                canvas.drawBitmap(logoBitmap, logoX, logoY, null);
                canvas.drawText(appName, margin + logoWidth + 15, yPosition + logoHeight - 10, titlePaint);
                yPosition += logoHeight + 20;
            } else {
                canvas.drawText(appName, margin, yPosition + 20, titlePaint);
                yPosition += 45;
            }
            canvas.drawText(reportTitle, margin, yPosition, textPaint);
            yPosition += 18;
            canvas.drawText("Generated: " + generatedDate, margin, yPosition, textPaint);
            yPosition += 30;
            
            int doctors = 0, patients = 0;
            for (UserEntity user : users) {
                if ("doctor".equals(user.getRole())) doctors++;
                else if ("patient".equals(user.getRole())) patients++;
            }
            
            int pending = 0, approved = 0, declined = 0, completed = 0;
            for (Appointment apt : appointments) {
                switch (apt.getStatus()) {
                    case "pending": pending++; break;
                    case "approved": approved++; break;
                    case "declined": declined++; break;
                    case "completed": completed++; break;
                }
            }
            
            headerPaint.setColor(0xFF4CAF50);
            canvas.drawRect(margin, yPosition, margin + 200, yPosition + rowHeight, headerPaint);
            canvas.drawText("Metric", margin + 5, yPosition + 17, textPaint);
            canvas.drawText("Value", margin + 120, yPosition + 17, textPaint);
            yPosition += rowHeight;
            
            String[][] stats = {
                {"Total Users", String.valueOf(users.size())},
                {"Total Doctors", String.valueOf(doctors)},
                {"Total Patients", String.valueOf(patients)},
                {"Total Appointments", String.valueOf(appointments.size())},
                {"Pending Appointments", String.valueOf(pending)},
                {"Approved Appointments", String.valueOf(approved)},
                {"Declined Appointments", String.valueOf(declined)},
                {"Completed Appointments", String.valueOf(completed)}
            };
            
            for (String[] stat : stats) {
                canvas.drawText(stat[0], margin + 5, yPosition + 17, textPaint);
                canvas.drawText(stat[1], margin + 120, yPosition + 17, textPaint);
                yPosition += rowHeight;
            }
            
            pdfDocument.finishPage(page);
            
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null || !dir.exists()) {
                dir = new File(getFilesDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
            }
            
            File file = new File(dir, "statistics_report.pdf");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
            }
            pdfDocument.close();
            
            sharePdfFile(file, "Statistics Report");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportAppointmentsAsPDF(List<Appointment> appointments) {
        try {
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
            String generatedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            Bitmap logoBitmap = getLogoBitmap();
            int logoWidth = 0;
            if (logoBitmap != null) {
                logoWidth = logoBitmap.getWidth();
                int logoHeight = logoBitmap.getHeight();
                int logoX = margin;
                int logoY = yPosition + 5;
                canvas.drawBitmap(logoBitmap, logoX, logoY, null);
                canvas.drawText(appName, margin + logoWidth + 15, yPosition + logoHeight - 10, titlePaint);
                yPosition += logoHeight + 20;
            } else {
                canvas.drawText(appName, margin, yPosition + 20, titlePaint);
                yPosition += 45;
            }
            canvas.drawText(reportTitle, margin, yPosition, textPaint);
            yPosition += 18;
            canvas.drawText("Generated: " + generatedDate, margin, yPosition, textPaint);
            yPosition += 25;
            
            int[] colWidths = {70, 70, 55, 40, 50, 80};
            String[] headers = {"Patient", "Doctor", "Date", "Time", "Status", "Reason"};
            
            headerPaint.setColor(0xFF4CAF50);
            canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + rowHeight, headerPaint);
            
            int xPosition = margin + 3;
            for (int i = 0; i < headers.length; i++) {
                canvas.drawText(headers[i], xPosition, yPosition + 16, textPaint);
                xPosition += colWidths[i];
            }
            yPosition += rowHeight;
            
            for (Appointment apt : appointments) {
                if (yPosition > pageHeight - margin - 30) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = margin;
                }
                
                String[] rowData = {
                    truncateText(apt.getPatientName(), 11),
                    truncateText(apt.getDoctorName(), 11),
                    truncateText(apt.getDate(), 9),
                    truncateText(apt.getTime(), 6),
                    truncateText(apt.getStatus(), 8),
                    truncateText(apt.getReason() != null ? apt.getReason() : "", 13)
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
            
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null || !dir.exists()) {
                dir = new File(getFilesDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
            }
            
            File file = new File(dir, "appointments_report.pdf");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
            }
            pdfDocument.close();
            
            sharePdfFile(file, "Appointments Report");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportAuditLogsAsPDF(List<AuditLog> auditLogs) {
        try {
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
            String reportTitle = "Audit Logs Report";
            String generatedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            Bitmap logoBitmap = getLogoBitmap();
            int logoWidth = 0;
            if (logoBitmap != null) {
                logoWidth = logoBitmap.getWidth();
                int logoHeight = logoBitmap.getHeight();
                int logoX = margin;
                int logoY = yPosition + 5;
                canvas.drawBitmap(logoBitmap, logoX, logoY, null);
                canvas.drawText(appName, margin + logoWidth + 15, yPosition + logoHeight - 10, titlePaint);
                yPosition += logoHeight + 20;
            } else {
                canvas.drawText(appName, margin, yPosition + 20, titlePaint);
                yPosition += 45;
            }
            canvas.drawText(reportTitle, margin, yPosition, textPaint);
            yPosition += 18;
            canvas.drawText("Generated: " + generatedDate, margin, yPosition, textPaint);
            yPosition += 25;
            
            int[] colWidths = {60, 50, 60, 100, 80};
            String[] headers = {"User", "Role", "Action", "Description", "Timestamp"};
            
            headerPaint.setColor(0xFF4CAF50);
            canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + rowHeight, headerPaint);
            
            int xPosition = margin + 3;
            for (int i = 0; i < headers.length; i++) {
                canvas.drawText(headers[i], xPosition, yPosition + 16, textPaint);
                xPosition += colWidths[i];
            }
            yPosition += rowHeight;
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            for (AuditLog log : auditLogs) {
                if (yPosition > pageHeight - margin - 30) {
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = margin;
                }
                
                String[] rowData = {
                    truncateText(log.getUserName(), 9),
                    truncateText(log.getUserRole(), 8),
                    truncateText(log.getAction(), 9),
                    truncateText(log.getDescription() != null ? log.getDescription() : "", 15),
                    truncateText(sdf.format(new Date(log.getTimestamp())), 12)
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
            
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null || !dir.exists()) {
                dir = new File(getFilesDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
            }
            
            File file = new File(dir, "audit_logs_report.pdf");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
            }
            pdfDocument.close();
            
            sharePdfFile(file, "Audit Logs Report");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void sharePdfFile(File file, String reportTitle) {
        showExportResultBottomSheet(file, "application/pdf");
    }

    private void shareFile(File file, String mimeType) {
        showExportResultBottomSheet(file, mimeType);
    }

    private void showExportResultBottomSheet(File file, String mimeType) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_export_result, null);
        bottomSheetDialog.setContentView(view);

        TextView tvFileName = view.findViewById(R.id.tvFileName);
        TextView tvFilePath = view.findViewById(R.id.tvFilePath);
        MaterialButton btnOpenLocation = view.findViewById(R.id.btnOpenLocation);
        MaterialButton btnShare = view.findViewById(R.id.btnShare);

        tvFileName.setText(file.getName());
        tvFilePath.setText(file.getAbsolutePath());

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        btnOpenLocation.setOnClickListener(v -> {
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, mimeType);
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(openIntent);
            } catch (Exception e) {
                Toast.makeText(this, R.string.no_app_to_open_file, Toast.LENGTH_SHORT).show();
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
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }

    private void shareFile(String content, String fileName, String mimeType) {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null || !dir.exists()) {
                dir = new File(getFilesDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
            }
            
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes());
            }
            
            showExportResultBottomSheet(file, mimeType);
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveAndShareHTML(String htmlContent, String fileName, String reportTitle) {
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null || !dir.exists()) {
                dir = new File(getFilesDir(), "reports");
                if (!dir.exists()) dir.mkdirs();
            }
            
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(htmlContent.getBytes("UTF-8"));
            }
            
            showExportResultBottomSheet(file, "text/html");
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private String getReportHeader(String reportTitle) {
        try {
            String logoBase64 = getLogoAsBase64();
            String appName = getString(R.string.app_name);
            String generatedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            
            StringBuilder header = new StringBuilder();
            header.append("<div class='header'>");
            if (logoBase64 != null && !logoBase64.isEmpty()) {
                header.append("<img src='data:image/png;base64,").append(logoBase64).append("' class='logo' alt='HASET Logo' />");
            }
            header.append("<div class='header-text'>");
            header.append("<h1 class='app-name'>").append(escapeHtml(appName)).append("</h1>");
            header.append("<p class='report-title'>").append(escapeHtml(reportTitle)).append("</p>");
            header.append("</div>");
            header.append("</div>");
            header.append("<p><strong>Generated:</strong> ").append(generatedDate).append("</p>");
            return header.toString();
        } catch (Exception e) {
            // Fallback if logo loading fails
            String appName = getString(R.string.app_name);
            String generatedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            return "<div class='header'><div class='header-text'><h1 class='app-name'>" + escapeHtml(appName) + "</h1><p class='report-title'>" + escapeHtml(reportTitle) + "</p></div></div><p><strong>Generated:</strong> " + generatedDate + "</p>";
        }
    }

    private String getLogoAsBase64() {
        try {
            // Try to load the logo drawable
            Drawable drawable = getResources().getDrawable(R.drawable.haset_logo2, null);
            if (drawable == null) {
                drawable = getResources().getDrawable(R.drawable.haset_logo, null);
            }
            
            if (drawable != null) {
                // Convert drawable to bitmap
                Bitmap bitmap = null;
                if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                    bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                } else {
                    // For vector drawables or other types, create a bitmap
                    int width = drawable.getIntrinsicWidth();
                    int height = drawable.getIntrinsicHeight();
                    if (width <= 0) width = 200;
                    if (height <= 0) height = 200;
                    
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                }
                
                if (bitmap != null) {
                    // Resize if too large (max 200x200 for HTML)
                    if (bitmap.getWidth() > 200 || bitmap.getHeight() > 200) {
                        int newWidth = Math.min(200, bitmap.getWidth());
                        int newHeight = Math.min(200, bitmap.getHeight());
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    }
                    
                    // Convert to base64
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    byte[] byteArray = outputStream.toByteArray();
                    return Base64.encodeToString(byteArray, Base64.NO_WRAP);
                }
            }
            
            // Fallback: try to load from raw resources
            try {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.haset_logo2);
                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.haset_logo);
                }
                if (bitmap != null) {
                    // Resize if needed
                    if (bitmap.getWidth() > 200 || bitmap.getHeight() > 200) {
                        int newWidth = Math.min(200, bitmap.getWidth());
                        int newHeight = Math.min(200, bitmap.getHeight());
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    }
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    byte[] byteArray = outputStream.toByteArray();
                    return Base64.encodeToString(byteArray, Base64.NO_WRAP);
                }
            } catch (Exception e) {
                // Ignore
            }
        } catch (Exception e) {
            // Return null if logo cannot be loaded
        }
        return null;
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
                int maxSize = 60;
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
}

