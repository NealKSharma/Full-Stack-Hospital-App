package backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String SERVICE_ACCOUNT_PATH = "/etc/keys/saintcyshospital-firebase-adminsdk.json";

    @PostConstruct
    public void initFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                logger.info("FirebaseApp already initialized, skipping.");
                return;
            }

            logger.info("Initializing FirebaseApp using {}", SERVICE_ACCOUNT_PATH);

            try (FileInputStream serviceAccount = new FileInputStream(SERVICE_ACCOUNT_PATH)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                logger.info("FirebaseApp initialized successfully");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize FirebaseApp", e);
        }
    }
}
