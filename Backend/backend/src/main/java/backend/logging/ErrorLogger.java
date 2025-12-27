package backend.logging;

import backend.notifications.NotificationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.PrintWriter;
import java.io.StringWriter;

@Service
public class ErrorLogger {

    private static ErrorLogger instance;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @Autowired
    private NotificationService notificationService;

    @PostConstruct
    public void init() {
        ErrorLogger.instance = this;
    }

    // Static method for use in static contexts
    public static void logErrorStatic(Exception ex) {
        if (instance != null) {
            instance.logError(ex);
        } else {
            System.err.println("ErrorLogger not initialized: " + ex.getMessage());
        }
    }

    // Instance method for use in non-static contexts (autowired)
    public void logError(Exception ex) {
        ErrorLog errorLog = null;
        try {
            errorLog = new ErrorLog();

            // Set error message
            errorLog.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "");

            // Find code in the stack trace
            StackTraceElement yourCode = findYourCode(ex.getStackTrace());

            if (yourCode != null) {
                errorLog.setErrorFileName(yourCode.getFileName());
                errorLog.setMethodName(yourCode.getMethodName());
                errorLog.setLineNumber(yourCode.getLineNumber());
            } else {
                errorLog.setErrorFileName("");
                errorLog.setMethodName("");
                errorLog.setLineNumber(-1);
            }

            // Convert full stack trace to string
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            errorLog.setStackTrace(sw.toString());

            // Save to database
            errorLogRepository.save(errorLog);

        } catch (Exception e) {
            System.err.println("Failed to log error: " + e.getMessage());
        }

        // Notify admins
        if (errorLog != null) {
            try {
                notificationService.notifyAdminsOfError(errorLog);
            } catch (Exception e) {
                System.err.println("Failed to notify admins of error: " + e.getMessage());
            }
        }
    }

    // Find the first stack trace element from code
    private StackTraceElement findYourCode(StackTraceElement[] stackTrace) {
        if (stackTrace == null) return null;

        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("backend")) {
                return element;
            }
        }
        return null;
    }
}