package backend.notifications;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class NotificationConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfigurator.class);

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return context.getBean(endpointClass);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        try {
            Map<String, List<String>> headers = request.getHeaders();
            String token = extractFromAuthorization(headers);

            if (token == null) {
                token = extractFromQuery(request.getParameterMap());
            }

            if (token == null) {
                token = extractFromSubprotocol(headers);
            }

            if (token != null && !token.isBlank()) {
                config.getUserProperties().put("token", token.trim());
            } else {
                log.warn("Notification WS handshake missing token. headerKeys={} queryKeys={}",
                        headers != null ? headers.keySet() : Set.of(),
                        request.getParameterMap() != null ? request.getParameterMap().keySet() : Set.of());
            }

            super.modifyHandshake(config, request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractFromAuthorization(Map<String, List<String>> headers) {
        if (headers == null) return null;

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Authorization".equalsIgnoreCase(entry.getKey())) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    String header = values.get(0);
                    String lower = header.toLowerCase();
                    if (lower.startsWith("bearer ")) {
                        return header.substring(7).trim();
                    }
                    return header.trim();
                }
            }
        }
        return null;
    }

    private String extractFromQuery(Map<String, List<String>> params) {
        if (params == null) return null;

        for (String key : List.of("token", "jwt", "access_token", "auth")) {
            List<String> values = params.get(key);
            if (values != null) {
                for (String v : values) {
                    if (v != null && !v.isBlank()) {
                        return v.trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractFromSubprotocol(Map<String, List<String>> headers) {
        if (headers == null) return null;

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("Sec-WebSocket-Protocol".equalsIgnoreCase(entry.getKey())) {
                List<String> protocols = entry.getValue();
                if (protocols == null || protocols.isEmpty()) {
                    continue;
                }

                for (String protoHeader : protocols) {
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
