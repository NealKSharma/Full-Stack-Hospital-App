package backend.pharmacy;

import backend.logging.ErrorLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/admin/products")
public class AdminPharmacyController {

    @Autowired
    private PharmacyRepository productRepository;

    @Autowired
    private ErrorLogger errorLogger;

    @PostMapping
    @Operation(summary = "Add a new pharmacy product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "500", description = "Error adding product")
    })
    public ResponseEntity<?> addProduct(@RequestBody PharmacyProduct product) {
        try {
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(error("Product name is required"));
            }
            if (product.getGenericName() == null || product.getGenericName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(error("Generic name is required"));
            }
            if (product.getDosage() == null || product.getDosage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(error("Dosage is required"));
            }
            if (product.getPriceInCents() == null || product.getPriceInCents() <= 0) {
                return ResponseEntity.badRequest().body(error("Product price must be greater than 0"));
            }

            if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
                product.setImageUrl(null);
            }

            if (product.getIsProtected() == null) {
                product.setIsProtected(false);
            }

            productRepository.save(product);
            return ResponseEntity.ok(success("Product added successfully"));

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while adding product"));
        }
    }

    @GetMapping
    @Operation(summary = "Get all pharmacy products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error fetching products")
    })
    public ResponseEntity<?> getAllProducts() {
        try {
            List<PharmacyProduct> products = productRepository.findAll();
            return ResponseEntity.ok(products);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while fetching products"));
        }
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get a product by its name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Error fetching product")
    })
    public ResponseEntity<?> getProductByName(@PathVariable String name) {
        try {
            PharmacyProduct product = productRepository.findByName(name);
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("Product with name '" + name + "' not found"));
            }
            return ResponseEntity.ok(product);

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while fetching product"));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing pharmacy product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Error updating product")
    })
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            Optional<PharmacyProduct> existingOpt = productRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("Product with ID " + id + " not found"));
            }

            PharmacyProduct product = existingOpt.get();

            if (updates.containsKey("name")) {
                String name = (String) updates.get("name");
                if (name != null && !name.trim().isEmpty())
                    product.setName(name);
            }

            if (updates.containsKey("genericName")) {
                String genericName = (String) updates.get("genericName");
                if (genericName != null && !genericName.trim().isEmpty())
                    product.setGenericName(genericName);
            }

            if (updates.containsKey("dosage")) {
                String dosage = (String) updates.get("dosage");
                if (dosage != null && !dosage.trim().isEmpty())
                    product.setDosage(dosage);
            }

            if (updates.containsKey("priceInCents")) {
                Integer price = (Integer) updates.get("priceInCents");
                if (price != null && price > 0)
                    product.setPriceInCents(price);
            }

            if (updates.containsKey("imageUrl")) {
                String imageUrl = (String) updates.get("imageUrl");
                product.setImageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl : null);
            }

            if (updates.containsKey("isProtected")) {
                Boolean isProtected = (Boolean) updates.get("isProtected");
                product.setIsProtected(isProtected != null ? isProtected : false);
            }

            productRepository.save(product);
            return ResponseEntity.ok(success("Product updated successfully: " + product.getName()));

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while updating product"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pharmacy product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Error deleting product")
    })
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            if (!productRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("Product with ID " + id + " not found"));
            }

            productRepository.deleteById(id);
            return ResponseEntity.ok(success("Product deleted successfully (ID: " + id + ")"));

        } catch (Exception e) {
            errorLogger.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An error occurred while deleting product"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "error");
        res.put("message", message);
        return res;
    }

    private Map<String, String> success(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("status", "success");
        res.put("message", message);
        return res;
    }
}
