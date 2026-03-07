package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.Inventory.SupplierCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierDTO;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/staff/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> getAll() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }


    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STOREKEEPER')")
    public ResponseEntity<SupplierDTO> createSupplier(
            @RequestPart("data") SupplierCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {

        return ResponseEntity.ok(
                supplierService.createSupplier(request, image)
        );
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STOREKEEPER')")
    public ResponseEntity<SupplierDTO> update(
            @PathVariable Long id,
            @RequestPart("data") SupplierCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        return ResponseEntity.ok(
                supplierService.updateSupplier(id, request, image)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STOREKEEPER')")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(new MessageResponse("Supplier deleted"));
    }
}
