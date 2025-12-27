package backend.messages;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import backend.logging.ErrorLogger;

/**
 * Custom configurator to enable Spring dependency injection in WebSocket endpoints
 * and to extract JWT token from Authorization header during handshake
 */
@Component
public class CustomConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(CustomConfigurator.class);

    private static ApplicationContext applicationContext;
    private static ErrorLogger errorLogger;

    @Autowired
    public void setErrorLogger(ErrorLogger errorLogger) {
        CustomConfigurator.errorLogger = errorLogger;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        CustomConfigurator.applicationContext = applicationContext;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return applicationContext.getBean(endpointClass);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        try {
            Map<String, List<String>> headers = request.getHeaders();

            String token = extractFromAuthorization(headers);
            if (token == null) token = extractFromQuery(request.getParameterMap());
            if (token == null) token = extractFromSubprotocol(headers);

            if (token != null && !token.isBlank()) {
                config.getUserProperties().put("token", token.trim());
            } else {
                log.warn("Chat WS handshake missing token. headerKeys={} queryKeys={}",
                        headers != null ? headers.keySet() : Set.of(),
                        request.getParameterMap() != null ? request.getParameterMap().keySet() : Set.of());
            }

            super.modifyHandshake(config, request, response);
        }
        catch (Exception e) {
            // Only log if error logger is available
            if (errorLogger != null) {
                try {
                    errorLogger.logError(e);
                } catch (Exception ignored) {
                    // Ignore errors during shutdown
                    System.err.println("Failed to log error: " + e.getMessage());
                }
            } else {
                System.err.println("Error in handshake: " + e.getMessage());
            }
        }
    }

    private String extractFromAuthorization(Map<String, List<String>> headers) {
        if (headers == null) return null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("Authorization")) {
                List<String> authHeaders = entry.getValue();
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    String authHeader = authHeaders.get(0);
                    if (authHeader.toLowerCase().startsWith("bearer ")) {
                        return authHeader.substring(7).trim();
                    }
                    if (!authHeader.isBlank()) {
                        return authHeader.trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractFromQuery(Map<String, List<String>> params) {
        if (params == null) return null;
        for (String key : List.of("token", "jwt", "access_token", "auth")) {
            List<String> values = params.get(key);
            if (values == null) continue;
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
        }
        return null;
    }

    private String extractFromSubprotocol(Map<String, List<String>> headers) {
        if (headers == null) return null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Sec-WebSocket-Protocol".equalsIgnoreCase(entry.getKey())) {
                List<String> protos = entry.getValue();
                if (protos == null) continue;
                for (String protoHeader : protos) {
                    if (protoHeader == null) continue;
                    for (String part : protoHeader.split(",")) {
                        String candidate = part.trim();
                        if (candidate.toLowerCase().startsWith("bearer ")) {
                            return candidate.substring(7).trim();
                        }
                    }
                }
            }
        }
        return null;
    }
}
