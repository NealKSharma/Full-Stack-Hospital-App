package backend.pharmacy;

import backend.authentication.JwtUtil;
import backend.logging.ErrorLogger;
import backend.notifications.NotificationEndpoint;
import backend.notifications.NotificationService;
import backend.patient.PatientRepository;
import backend.pharmacy.dto.PlaceOrderRequest;
import backend.pharmacy.dto.UpdateStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/pharmacy/orders")
public class PharmacyOrderController {

    private final PharmacyOrderService service;
    private final JwtUtil jwtUtil;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

    public PharmacyOrderController(
            PharmacyOrderService service,
            JwtUtil jwtUtil,
            PatientRepository patientRepository,
            NotificationService notificationService
    ) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
    }

    @PostMapping("/place")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Place a full pharmacy order (patient only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> place(@RequestBody PlaceOrderRequest req, HttpServletRequest http) {
        try {
            String token = jwtUtil.extractJwtFromRequest(http);
            Long userId = jwtUtil.extractUserId(token);

            var savedOrders = service.placeFullOrder(userId, req);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Order placed successfully");
            response.put("totalCents", req.getTotalCents());
            response.put("ordersCreated", savedOrders.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "View pharmacy orders placed by the authenticated patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to load patient orders")
    })
    public ResponseEntity<?> myOrders(HttpServletRequest http) {
        try {
            String token = jwtUtil.extractJwtFromRequest(http);
            Long userId = jwtUtil.extractUserId(token);

            return ResponseEntity.ok(service.getMine(userId));
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load your orders"));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('PHARMACIST')")
    @Operation(summary = "View all pharmacy orders with patient details (pharmacist only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to load all orders")
    })
    public ResponseEntity<?> all() {
        try {
            var orders = service.getAll();
            List<Map<String, Object>> enriched = new ArrayList<>();

            for (PharmacyOrder o : orders) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", o.getId());
                map.put("userId", o.getUserId());
                map.put("productId", o.getProductId());
                map.put("productName", o.getProductName());
                map.put("productDosage", o.getProductDosage());
                map.put("quantity", o.getQuantity());
                map.put("totalInCents", o.getTotalInCents());
                map.put("status", o.getStatus());
                map.put("createdAt", o.getCreatedAt());

                patientRepository.findById(o.getUserId()).ifPresentOrElse(patient -> {
                    map.put("patientName", patient.getName());
                    map.put("patientMrn", patient.getMrn());
                    map.put("patientGender", patient.getGender());
                }, () -> {
                    map.put("patientName", "Unknown");
                });

                enriched.add(map);
            }

            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load orders"));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('PHARMACIST')")
    @Operation(summary = "Update the status of a pharmacy order (pharmacist only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status update"),
            @ApiResponse(responseCode = "500", description = "Failed to update order status")
    })
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req) {
        try {
            var saved = service.updateStatus(id, req.getStatus());

            if (saved.getStatus() == PharmacyOrderStatus.READY_FOR_PICKUP) {
                String message = "Your order for " + saved.getProductName() + " is ready for pickup.";

                notificationService.saveNotificationByUserId(saved.getUserId(),
                        "Pharmacy Order Ready", message);

                NotificationEndpoint.sendToUser(saved.getUserId(),
                        "ORDER", "Pharmacy Order Ready", message);

            } else if (saved.getStatus() == PharmacyOrderStatus.REJECTED) {
                String message = "Your order for " + saved.getProductName() + " was rejected by the pharmacist.";

                notificationService.saveNotificationByUserId(saved.getUserId(),
                        "Pharmacy Order Rejected", message);

                NotificationEndpoint.sendToUser(saved.getUserId(),
                        "ORDER", "Pharmacy Order Rejected", message);

            } else if (saved.getStatus() == PharmacyOrderStatus.FULFILLED) {
                String message = "Your order for " + saved.getProductName() + " has been picked up successfully.";

                notificationService.saveNotificationByUserId(saved.getUserId(),
                        "Pharmacy Order Completed", message);

                NotificationEndpoint.sendToUser(saved.getUserId(),
                        "ORDER", "Pharmacy Order Completed", message);
            }

            return ResponseEntity.ok(Map.of(
                    "orderId", saved.getId(),
                    "newStatus", saved.getStatus(),
                    "message", "Order status updated successfully"
            ));

        } catch (Exception e) {
            ErrorLogger.logErrorStatic(e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','PATIENT')")
    @Operation(summary = "Get a single pharmacy order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<?> one(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getOne(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
