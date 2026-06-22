# 📊 HASET App — Export Reports System: Complete Guide
> *Combined from: EXPORT_REPORTS_DOCUMENTATION.md*

---

## 📑 Table of Contents
1. [System Overview](#1-system-overview)
2. [Report Types](#2-report-types)
3. [Export Formats](#3-export-formats)
4. [Export Flow](#4-export-flow)
5. [File Storage & Naming](#5-file-storage--naming)
6. [Report Branding & Header](#6-report-branding--header)
7. [Technical Implementation](#7-technical-implementation)
8. [Format Structures](#8-format-structures)
9. [Error Handling](#9-error-handling)
10. [Troubleshooting](#10-troubleshooting)
11. [Future Enhancements](#11-future-enhancements)

---

## 1. System Overview

The HASET export system allows **Admins** to generate professionally branded reports from live app data and export them in multiple formats for offline viewing, analysis, or sharing.

```mermaid
graph TD
    ADMIN[🛡️ Admin] --> ARA[AdminReportActivity]
    ARA --> RT[Select Report Type]
    RT --> GEN[Generate Report Button]
    GEN --> FMT[Select Export Format]

    FMT --> CSV[CSV\nComma-Separated]
    FMT --> XLS[XLS\nExcel XML]
    FMT --> HTML[HTML/PDF]

    CSV --> LSH[LocalStorageHelper\nGet Data]
    XLS --> LSH
    HTML --> LSH

    LSH --> DB[(SQLite\nRoom Database)]
    DB --> DATA[Report Data]

    DATA --> FILE[Write to File\nPrivate Storage]
    FILE --> FP[FileProvider\nSecure Share]
    FP --> CHOOSER[App Chooser]
    CHOOSER --> APP[External App\nExcel / Browser / Gmail...]
```

---

## 2. Report Types

```mermaid
mindmap
  root((HASET Reports))
    Users List
      User ID
      Full Name
      Email
      Phone
      Role
      Created Date
    App Statistics
      Total Users
      Total Doctors
      Total Patients
      Total Appointments
      Pending / Approved / Declined / Completed
      Timestamp
    Appointments
      Patient Name
      Doctor Name
      Date & Time
      Status
      Reason/Notes
    Audit Logs
      User Name
      User Role
      Action Performed
      Description
      Timestamp
```

### Report Details

#### 1. Users List Report
| Column | Source |
|--------|--------|
| User ID | `users.userId` |
| Full Name | `users.fullName` |
| Email | `users.email` |
| Phone | `users.phone` |
| Role | `users.role` |
| Created Date | `users.createdAt` |

#### 2. App Statistics Report
| Metric | Source |
|--------|--------|
| Total Users | COUNT all users |
| Total Doctors | COUNT users WHERE role = doctor |
| Total Patients | COUNT users WHERE role = patient |
| Total Appointments | COUNT all appointments |
| Pending / Approved / Declined / Completed | COUNT by status |
| Generated At | `System.currentTimeMillis()` |

#### 3. Appointments Report
| Column | Source |
|--------|--------|
| Patient Name | `appointments.patientName` |
| Doctor Name | `appointments.doctorName` |
| Date | `appointments.date` |
| Time | `appointments.time` |
| Status | `appointments.status` |
| Reason | `appointments.reason` |

#### 4. Audit Logs Report
| Column | Source |
|--------|--------|
| User Name | `users.fullName` (joined) |
| User Role | `users.role` |
| Action | `audit_logs.action` |
| Description | `audit_logs.details` |
| Timestamp | `audit_logs.timestamp` |

---

## 3. Export Formats

| Format | MIME Type | Extension | Best For | Compatible Apps |
|--------|-----------|-----------|----------|----------------|
| **CSV** | `text/csv` | `.csv` | Data analysis, spreadsheet import | Excel, Google Sheets, WPS, LibreOffice |
| **XLS** | `application/vnd.ms-excel` | `.xls` | Excel-native formatting | Excel, Google Sheets, WPS, LibreOffice |
| **HTML/PDF** | `text/html` | `.html` | Viewing, printing, PDF conversion | Chrome, Firefox, Edge, any browser |

---

## 4. Export Flow

```mermaid
flowchart TD
    A([Admin on AdminReportActivity]) --> B[Select Report Type\nUsers / Stats / Appointments / Audit Logs]
    B --> C[Tap Generate Report]
    C --> D[Format Dialog\nCSV / XLS / PDF HTML]

    D -->|CSV| E1[exportXxxAsCSV]
    D -->|XLS| E2[exportXxxAsXLS]
    D -->|HTML| E3[exportXxxAsPDF]

    E1 --> F[LocalStorageHelper\nFetch data async]
    E2 --> F
    E3 --> F

    F --> G[AuditLogger\nfor audit log data]
    F --> H[Build report content\nCSV string / XML string / HTML string]

    H --> I[Add header with logo, app name, date]
    I --> J{Primary storage\navailable?}
    J -->|Yes| K[Write to getExternalFilesDir\nDOCUMENTS/]
    J -->|No| L[Write to getFilesDir\nreports/]

    K --> M[FileProvider.getUriForFile]
    L --> M
    M --> N[Intent.ACTION_VIEW\nwith file URI]
    N --> O[Android App Chooser\nshows compatible apps]
    O --> P([User opens file\nin chosen app])

    N -->|No app available| Q[Intent.ACTION_SEND\nShare fallback]
```

---

## 5. File Storage & Naming

### Storage Location
- **Primary:** `getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)/`
- **Fallback:** `getFilesDir()/reports/`
- **Authority:** `com.haset.hasetapp.provider`

### File Naming Convention

| Report | File Name |
|--------|-----------|
| Users List | `users_report.[csv|xls|html]` |
| App Statistics | `statistics_report.[csv|xls|html]` |
| Appointments | `appointments_report.[csv|xls|html]` |
| Audit Logs | `audit_logs_report.[csv|xls|html]` |

### FileProvider Paths (`file_paths.xml`)
```xml
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-files-path name="images" path="Pictures/" />
    <files-path name="profile_photos" path="profile_photos/" />
    <files-path name="reports" path="reports/" />
    <cache-path name="cache" path="." />
</paths>
```

---

## 6. Report Branding & Header

All exported reports include a professional HASET-branded header.

```mermaid
graph LR
    HEADER[Report Header] --> LOGO[HASET Logo\nBase64-encoded PNG\n60×60px]
    HEADER --> TITLE[App Name: HASET\nBold, 24px, green #4CAF50]
    HEADER --> RTITLE[Report Title\n18px, gray #666]
    HEADER --> DATE[Generated: yyyy-MM-dd HH:mm:ss]
    HEADER --> BORDER[Green border bottom\n2px solid #4CAF50]
```

### HTML Header Structure
```html
<div class='header'>
  <img src='data:image/png;base64,...' class='logo' style='width:60px;height:60px;' />
  <div class='header-text'>
    <h1 class='app-name' style='color:#4CAF50;font-size:24px;font-weight:bold;'>HASET</h1>
    <p class='report-title' style='color:#666;font-size:18px;'>Users List Report</p>
  </div>
</div>
<p><strong>Generated:</strong> 2026-02-22 04:00:00</p>
```

### Logo Handling
```java
// Converts drawable to Base64 for HTML embedding
private String getLogoAsBase64() { ... }
// Falls back gracefully if logo drawable is missing
```

---

## 7. Technical Implementation

### Key Classes

| Class | Role |
|-------|------|
| `AdminReportActivity` | Main UI, format selection, export trigger |
| `ReportTypeAdapter` | RecyclerView adapter for report type cards |
| `LocalStorageHelper` | Fetches users, appointments, stats from Room DB |
| `AuditLogger` | Provides audit log records for export |

### Export Method Matrix

| Report | CSV | XLS | HTML |
|--------|-----|-----|------|
| Users | `exportUsersAsCSV()` | `exportUsersAsXLS()` | `exportUsersAsPDF()` |
| Statistics | `exportStatisticsAsCSV()` | `exportStatisticsAsXLS()` | `exportStatisticsAsPDF()` |
| Appointments | `exportAppointmentsAsCSV()` | `exportAppointmentsAsXLS()` | `exportAppointmentsAsPDF()` |
| Audit Logs | `exportAuditLogsAsCSV()` | `exportAuditLogsAsXLS()` | `exportAuditLogsAsPDF()` |

### Helper Methods
```java
shareFile(File file, String mimeType)     // Share CSV/XLS via intent
saveAndShareHTML(String htmlContent)       // Save HTML + launch browser
getReportHeader(String reportTitle)        // Generate branded HTML header
getLogoAsBase64(Context context)           // Encode logo for HTML embedding
escapeHtml(String text)                    // Sanitize data for HTML output
```

---

## 8. Format Structures

### CSV Format
```
Encoding: UTF-8
Separator: comma
Values: double-quoted

Example:
"User ID","Name","Email","Phone","Role","Created Date"
"user123","John Doe","john@example.com","0712345678","patient","2024-01-01 10:00:00"
```

### XLS Format (Excel XML)
```xml
<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet">
  <Worksheet ss:Name="Sheet1">
    <Table>
      <Row>
        <Cell><Data ss:Type="String">User ID</Data></Cell>
        <Cell><Data ss:Type="String">Name</Data></Cell>
      </Row>
      <Row>
        <Cell><Data ss:Type="String">user123</Data></Cell>
        <Cell><Data ss:Type="String">John Doe</Data></Cell>
      </Row>
    </Table>
  </Worksheet>
</Workbook>
```

### HTML/PDF Structure
```html
<!DOCTYPE html>
<html>
<head>
  <meta charset='UTF-8'>
  <meta name='viewport' content='width=device-width, initial-scale=1.0'>
  <style>
    @media print { /* Print-friendly CSS for PDF */ }
    .header { display: flex; border-bottom: 2px solid #4CAF50; }
    table { border-collapse: collapse; width: 100%; }
    th { background: #4CAF50; color: white; }
    tr:nth-child(even) { background: #f2f2f2; }
  </style>
</head>
<body>
  <!-- Branded header -->
  <!-- Generated date -->
  <!-- Data table -->
</body>
</html>
```

---

## 9. Error Handling

| Error | Message Shown | Recovery |
|-------|--------------|---------|
| File write failure | "Export failed: [message]" | Check storage space / permissions |
| Data load failure | "Failed to load [data]: [error]" | Retry; check DB connectivity |
| Logo missing | Header shown without logo | Graceful fallback, no crash |
| No app to open file | Shows share fallback | User can share to email/Drive |

---

## 10. Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| App chooser not appearing | No compatible app installed | Install Excel, Sheets, or browser |
| Logo not in HTML header | Drawable missing | Add `haset_logo.png` to `res/drawable/` |
| File cannot be opened | FileProvider misconfiguration | Verify `AndroidManifest.xml` + `file_paths.xml` |
| Large file takes long | Big dataset | Normal — consider date range filtering (future) |
| File export fails | Storage permission denied | Handled via FileProvider — no manual permission needed in API 29+ |

---

## 11. Future Enhancements

```mermaid
timeline
    title Planned Export Improvements
    V2 : Direct PDF generation (no HTML conversion needed)
       : Date range filter for reports
    V3 : Scheduled automatic report generation
       : Email report delivery
    V4 : Cloud storage integration (Google Drive, Dropbox)
       : Custom report templates
    V5 : Multi-format batch export
       : Data filtering by role / status / date
```

---

*Last Updated: 2026-02-22 | HASET App — Reports Module | Admin Only Feature*
