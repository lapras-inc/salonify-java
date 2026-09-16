package com.example.salonify.view;

import com.example.salonify.entity.Report;
import com.example.salonify.entity.Thread;

/** admin/reports.html の通報行用の view model。 */
public record ReportRow(Report report, String reporter, Thread thread) {
}
