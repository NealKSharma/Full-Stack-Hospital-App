package coms309.report;
import coms309.people.Report;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;

/**
 * Controller to create and manage reports
 *
 * @author Devank Uppal
 */

@RestController
public class ReportController {

    HashMap<Integer, Report> reportList = new HashMap<>();

    // list all reports
    @GetMapping("/reports")
    public HashMap<Integer, Report> getAllReports() {
        return reportList;
    }

    // create a new report
    @PostMapping("/reports")
    public String createReport(@RequestBody Report report) {
        reportList.put(report.getId(), report);
        return "Report created for ID " + report.getId();
    }

    // read a report by id
    @GetMapping("/reports/{id}")
    public Report getReport(@PathVariable int id) {
        return reportList.get(id);
    }

    // Update a report by id
    @PutMapping("/reports/{id}")
    public Report updateReport(@PathVariable int id, @RequestBody Report report) {
        reportList.replace(id, report);
        return reportList.get(id);
    }

    // Delete a report by id
    @DeleteMapping("/reports/{id}")
    public HashMap<Integer, Report> deleteReport(@PathVariable int id) {
        reportList.remove(id);
        return reportList;
    }
}
