package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.PaginatedSupplierResponse;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierDTO;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.Supplier;
import co.edu.uniquindio.servly.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService Tests")
class SupplierServiceTest {

    @Mock SupplierRepository supplierRepository;
    @Mock ImageService imageService;

    @InjectMocks SupplierService supplierService;

    private Supplier supplier;
    private SupplierCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder()
                .id(1L)
                .name("Proveedor ABC")
                .description("Carnes frescas")
                .contactNumber("3001234567")
                .email("proveedor@abc.com")
                .logoUrl(null)
                .build();

        createRequest = new SupplierCreateRequest(
                "Proveedor ABC", "Carnes frescas", "3001234567", "proveedor@abc.com", null);
    }

    // ── getAllSuppliers ────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllSuppliers - retorna lista de DTOs")
    void getAllSuppliers_success() {
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));

        List<SupplierDTO> result = supplierService.getAllSuppliers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Proveedor ABC");
        assertThat(result.get(0).getContactNumber()).isEqualTo("3001234567");
        assertThat(result.get(0).getEmail()).isEqualTo("proveedor@abc.com");
    }

    @Test
    @DisplayName("getAllSuppliers - lista vacía retorna lista vacía")
    void getAllSuppliers_empty() {
        when(supplierRepository.findAll()).thenReturn(List.of());

        List<SupplierDTO> result = supplierService.getAllSuppliers();

        assertThat(result).isEmpty();
    }

    // ── getSupplierById ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSupplierById - retorna proveedor existente")
    void getSupplierById_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        SupplierDTO result = supplierService.getSupplierById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Proveedor ABC");
    }

    @Test
    @DisplayName("getSupplierById - ID no encontrado lanza NotFoundException")
    void getSupplierById_notFound_throws() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Supplier not found with id: 99");
    }

    // ── createSupplier ────────────────────────────────────────────────────

    @Test
    @DisplayName("createSupplier - sin imagen crea correctamente")
    void createSupplier_noImage_success() throws Exception {
        when(supplierRepository.save(any())).thenReturn(supplier);

        SupplierDTO result = supplierService.createSupplier(createRequest, null);

        assertThat(result.getName()).isEqualTo("Proveedor ABC");
        assertThat(result.getLogoUrl()).isNull();
        verify(imageService, never()).upload(any());
    }

    @Test
    @DisplayName("createSupplier - con imagen sube y guarda URL")
    @SuppressWarnings("unchecked")
    void createSupplier_withImage_uploadsAndSavesUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "logo.png", "image/png", "bytes".getBytes());

        Map<String, Object> uploadResult = Map.of("secure_url", "https://cloudinary.com/logo.png");
        when(imageService.upload(image)).thenReturn(uploadResult);

        Supplier withLogo = Supplier.builder()
                .id(1L).name("Proveedor ABC").description("Carnes frescas")
                .contactNumber("3001234567").email("proveedor@abc.com")
                .logoUrl("https://cloudinary.com/logo.png").build();
        when(supplierRepository.save(any())).thenReturn(withLogo);

        SupplierDTO result = supplierService.createSupplier(createRequest, image);

        assertThat(result.getLogoUrl()).isEqualTo("https://cloudinary.com/logo.png");
        verify(imageService).upload(image);
    }

    @Test
    @DisplayName("createSupplier - error al subir imagen lanza RuntimeException")
    void createSupplier_imageUploadFails_throws() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "logo.png", "image/png", "bytes".getBytes());

        when(imageService.upload(any())).thenThrow(new RuntimeException("Cloudinary down"));

        assertThatThrownBy(() -> supplierService.createSupplier(createRequest, image))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error uploading image");
    }

    // ── updateSupplier ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateSupplier - actualiza datos sin imagen")
    void updateSupplier_noImage_success() throws Exception {
        SupplierCreateRequest updateReq = new SupplierCreateRequest(
                "Nuevo Nombre", "Nueva desc", "3009999999", "nuevo@abc.com", null);

        Supplier updated = Supplier.builder()
                .id(1L).name("Nuevo Nombre").description("Nueva desc")
                .contactNumber("3009999999").email("nuevo@abc.com").logoUrl(null).build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any())).thenReturn(updated);

        SupplierDTO result = supplierService.updateSupplier(1L, updateReq, null);

        assertThat(result.getName()).isEqualTo("Nuevo Nombre");
        assertThat(result.getContactNumber()).isEqualTo("3009999999");
        verify(imageService, never()).upload(any());
    }

    @Test
    @DisplayName("updateSupplier - con imagen actualiza logoUrl")
    @SuppressWarnings("unchecked")
    void updateSupplier_withImage_updatesLogoUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "new.png", "image/png", "bytes".getBytes());

        Map<String, Object> uploadResult = Map.of("secure_url", "https://cloudinary.com/new.png");
        when(imageService.upload(image)).thenReturn(uploadResult);

        Supplier updated = Supplier.builder()
                .id(1L).name("Proveedor ABC").description("Carnes frescas")
                .contactNumber("3001234567").email("proveedor@abc.com")
                .logoUrl("https://cloudinary.com/new.png").build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any())).thenReturn(updated);

        SupplierDTO result = supplierService.updateSupplier(1L, createRequest, image);

        assertThat(result.getLogoUrl()).isEqualTo("https://cloudinary.com/new.png");
    }

    @Test
    @DisplayName("updateSupplier - ID no encontrado lanza NotFoundException")
    void updateSupplier_notFound_throws() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier(99L, createRequest, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Supplier not found with id: 99");
    }

    @Test
    @DisplayName("updateSupplier - error al subir imagen lanza RuntimeException")
    void updateSupplier_imageUploadFails_throws() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "bad.png", "image/png", "bytes".getBytes());

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(imageService.upload(any())).thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> supplierService.updateSupplier(1L, createRequest, image))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error uploading image");
    }

    // ── deleteSupplier ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSupplier - elimina proveedor correctamente")
    void deleteSupplier_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        supplierService.deleteSupplier(1L);

        verify(supplierRepository).delete(supplier);
    }

    @Test
    @DisplayName("deleteSupplier - ID no encontrado lanza NotFoundException")
    void deleteSupplier_notFound_throws() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deleteSupplier(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Supplier not found with id: 99");
    }

    // ── getAllSuppliersPaginated ───────────────────────────────────────────

    @Test
    @DisplayName("getAllSuppliersPaginated - retorna respuesta paginada")
    void getAllSuppliersPaginated_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> page = new PageImpl<>(List.of(supplier), pageable, 1);

        when(supplierRepository.findAll(pageable)).thenReturn(page);

        PaginatedSupplierResponse response = supplierService.getAllSuppliersPaginated(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getPageNumber()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
        assertThat(response.getContent().get(0).getName()).isEqualTo("Proveedor ABC");
    }

    @Test
    @DisplayName("getAllSuppliersPaginated - página vacía retorna content vacío")
    void getAllSuppliersPaginated_emptyPage() {
        Pageable pageable = PageRequest.of(5, 10);
        Page<Supplier> page = new PageImpl<>(List.of(), pageable, 0);

        when(supplierRepository.findAll(pageable)).thenReturn(page);

        PaginatedSupplierResponse response = supplierService.getAllSuppliersPaginated(pageable);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0L);
    }
}