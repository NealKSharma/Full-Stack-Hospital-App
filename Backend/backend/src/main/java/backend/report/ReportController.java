package backend.report;

import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ErrorLogger errorLogger;

    @GetMapping
    @Operation(summary = "Get all reports")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve reports")
    })
    public List<Report> getAllReports() {
        try {
            return reportRepository.findAll();
        } catch (Exception e) {
            errorLogger.logError(e);
            return List.of();
        }
    }

    @PostMapping
    @Operation(summary = "Create a new report")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report created successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to create report")
    })
    public Report createReport(@RequestBody Report report) {
        try {
            return reportRepository.save(report);
        } catch (Exception e) {
            errorLogger.logError(e);
            return null;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a report by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    public Report getReport(@PathVariable Integer id) {
        try {
            return reportRepository.findById(id).orElse(null);
        } catch (Exception e) {
            errorLogger.logError(e);
            return null;
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing report by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report updated successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found"),
            @ApiResponse(responseCode = "500", description = "Failed to update report")
    })
    public Report updateReport(@PathVariable Integer id, @RequestBody Report newReport) {
        try {
            return reportRepository.findById(id).map(report -> {
                report.setType(newReport.getType());
                report.setResult(newReport.getResult());
                report.setDate(newReport.getDate());
                return reportRepository.save(report);
            }).orElse(null);
        } catch (Exception e) {
            errorLogger.logError(e);
            return null;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a report by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to delete report")
    })
    public void deleteReport(@PathVariable Integer id) {
        try {
            reportRepository.deleteById(id);
        } catch (Exception e) {
            errorLogger.logError(e);
        }
    }

    @GetMapping("/search/{type}")
    @Operation(summary = "Search reports by type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to retrieve reports by type")
    })
    public List<Report> getReportsByType(@PathVariable String type) {
        try {
            return reportRepository.findByTypeIgnoreCase(type);
        } catch (Exception e) {
            errorLogger.logError(e);
            return List.of();
        }
    }
}
