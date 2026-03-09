package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.SupplierCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierDTO;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierResponse;
import co.edu.uniquindio.servly.DTO.Inventory.PaginatedSupplierResponse;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.Supplier;
import co.edu.uniquindio.servly.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ImageService imageService;

    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public SupplierDTO getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));
    }

    public SupplierDTO createSupplier(SupplierCreateRequest request, MultipartFile image) {

        String logoUrl = null;

        try {
            if (image != null && !image.isEmpty()) {
                Map uploadResult = imageService.upload(image);
                logoUrl = uploadResult.get("secure_url").toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error uploading image");
        }

        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .description(request.getDescription())
                .contactNumber(request.getContactNumber())
                .email(request.getEmail())
                .logoUrl(logoUrl)
                .build();

        supplier = supplierRepository.save(supplier);

        return toDTO(supplier);
    }

    public SupplierDTO updateSupplier(Long id, SupplierCreateRequest request, MultipartFile image) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));

        supplier.setName(request.getName());
        supplier.setDescription(request.getDescription());
        supplier.setContactNumber(request.getContactNumber());
        supplier.setEmail(request.getEmail());

        try {
            if (image != null && !image.isEmpty()) {
                Map uploadResult = imageService.upload(image);
                String logoUrl = uploadResult.get("secure_url").toString();
                supplier.setLogoUrl(logoUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error uploading image");
        }

        supplier = supplierRepository.save(supplier);

        return toDTO(supplier);
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found with id: " + id));
        supplierRepository.delete(supplier);
    }

    public SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getLogoUrl());
    }

    public SupplierDTO toDTO(Supplier s) {
        return new SupplierDTO(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getContactNumber(),
                s.getEmail(),
                s.getLogoUrl()
        );
    }

    public PaginatedSupplierResponse getAllSuppliersPaginated(Pageable pageable) {
        Page<Supplier> page = supplierRepository.findAll(pageable);

        return PaginatedSupplierResponse.builder()
                .content(page.getContent().stream().map(this::toDTO).collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}
