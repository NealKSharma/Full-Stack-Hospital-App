package coms309.people;
import coms309.people.Report_adv;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller to manage reports
 */
@RestController
@RequestMapping("/reports")
public class ReportController_adv {

    private Map<String, coms309.people.Report_adv> reportMap = new HashMap<>();

    // Create a new report
    @PostMapping
    public String createReport(@RequestBody Report_adv report) {
        String key = String.valueOf(report.getId());
        reportMap.put(key, report);
        return "Report created: " + report.getType() + " on " + report.getDate();
    }

    // Get all reports
    @GetMapping
    public Collection<Report_adv> getAllReports() {
        return reportMap.values();
    }

    // Get a report by ID
    @GetMapping("/{id}")
    public Report_adv getReportById(@PathVariable int id) {
        return reportMap.get(String.valueOf(id));
    }

    // Get reports by type
    @GetMapping("/type")
    public List<Report_adv> getReportsByType(@RequestParam String type) {
        List<Report_adv> result = new ArrayList<>();
        for (Report_adv r : reportMap.values()) {
            if (r.getType().equalsIgnoreCase(type)) {
                result.add(r);
            }
        }
        return result;
    }

    // Delete a report by ID
    @DeleteMapping("/{id}")
    public String deleteReport(@PathVariable int id) {
        Report_adv removed = reportMap.remove(String.valueOf(id));
        return removed != null ? "Deleted report " + id : "Report not found";
    }

    // Sort reports by date (simple string comparison)
    @GetMapping("/sorted")
    public List<Report_adv> getReportsSorted() {
        List<Report_adv> sorted = new ArrayList<>(reportMap.values());
        sorted.sort(Comparator.comparing(Report_adv::getDate));
        return sorted;
    }
}
