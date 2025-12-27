package backend.pharmacy;

import backend.authentication.JwtUtil;
import backend.logging.ErrorLogger;
import backend.prescriptions.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/products")
public class PharmacyController {

    @Autowired
    private PharmacyRepository productRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ErrorLogger errorLogger;

    // ---- CREATE ----
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    @Operation(summary = "Add a new pharmacy product (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "500", description = "Error adding product")
    })
    public String addProduct(@RequestBody PharmacyProduct product) {
        try {
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                return "Error: Product name cannot be empty";
            }
            if (product.getPriceInCents() == null || product.getPriceInCents() <= 0) {
                return "Error: Product price must be greater than 0";
            }

            if (product.getIsProtected() == null) {
                product.setIsProtected(false);
            }

            productRepository.save(product);
            return "Product added successfully: " + product.getName();
        } catch (Exception e) {
            errorLogger.logError(e);
            return "Error: An error occurred while adding the product";
        }
    }

    // ---- READ ALL ----
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @GetMapping
    @Operation(summary = "Get all visible pharmacy products (filtered for patients)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving products")
    })
    public List<PharmacyProduct> getAllProducts(Authentication authentication) {
        try {
            List<PharmacyProduct> products = productRepository.findAll();

            boolean hasSpecialPermissions = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                            a.getAuthority().equals("ROLE_DOCTOR"));

            if (hasSpecialPermissions) {
                return products;
            }

            Long patientId = Long.parseLong(authentication.getName());

            return products.stream()
                    .filter(product -> {
                        if (product.getIsProtected() == null || !product.getIsProtected()) {
                            return true;
                        }

                        return prescriptionRepository.existsByPatientIdAndMedication(patientId, product.getName()) ||
                                prescriptionRepository.existsByPatientIdAndMedication(patientId, product.getGenericName());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            errorLogger.logError(e);
            return List.of();
        }
    }

    // ---- READ ONE ----
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @GetMapping("/{id}")
    @Operation(summary = "Get a pharmacy product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied for protected product"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Error retrieving product")
    })
    public Object getProductById(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<PharmacyProduct> product = productRepository.findById(id);
            if (product.isEmpty()) {
                return "Error: Product with ID " + id + " not found";
            }

            PharmacyProduct p = product.get();

            if (p.getIsProtected() != null && p.getIsProtected()) {
                boolean hasSpecialPermissions = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                                a.getAuthority().equals("ROLE_DOCTOR"));

                if (!hasSpecialPermissions) {
                    return "Error: You don't have permission to view this product";
                }
            }

            return p;
        } catch (Exception e) {
            errorLogger.logError(e);
            return "Error: An error occurred while fetching the product";
        }
    }

    // ---- UPDATE ----
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update a pharmacy product (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Error updating product")
    })
    public String updateProduct(@PathVariable Long id, @RequestBody PharmacyProduct updated) {
        try {
            Optional<PharmacyProduct> existingOpt = productRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return "Error: Product with ID " + id + " not found";
            }

            PharmacyProduct product = existingOpt.get();

            if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
                product.setName(updated.getName());
            }
            if (updated.getGenericName() != null) {
                product.setGenericName(updated.getGenericName());
            }
            if (updated.getDosage() != null) {
                product.setDosage(updated.getDosage());
            }
            if (updated.getPriceInCents() != null && updated.getPriceInCents() > 0) {
                product.setPriceInCents(updated.getPriceInCents());
            }
            if (updated.getImageUrl() != null) {
                product.setImageUrl(updated.getImageUrl());
            }
            if (updated.getIsProtected() != null) {
                product.setIsProtected(updated.getIsProtected());
            }

            productRepository.save(product);
            return "Product updated successfully: " + product.getName();

        } catch (Exception e) {
            errorLogger.logError(e);
            return "Error: An error occurred while updating the product";
        }
    }

    // ---- DELETE ----
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pharmacy product by ID (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Error deleting product")
    })
    public String deleteProduct(@PathVariable Long id) {
        try {
            if (!productRepository.existsById(id)) {
                return "Error: Product with ID " + id + " not found";
            }

            productRepository.deleteById(id);
            return "Product deleted successfully (ID: " + id + ")";

        } catch (Exception e) {
            errorLogger.logError(e);
            return "Error: An error occurred while deleting the product";
        }
    }
}
