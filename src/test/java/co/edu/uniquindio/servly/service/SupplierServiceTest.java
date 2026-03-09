package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.Inventory.PaginatedSupplierResponse;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierCreateRequest;
import co.edu.uniquindio.servly.DTO.Inventory.SupplierDTO;
import co.edu.uniquindio.servly.exception.NotFoundException;
import co.edu.uniquindio.servly.model.entity.Supplier;
import co.edu.uniquindio.servly.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService Tests")
class SupplierServiceTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private ImageService imageService;

    @InjectMocks
    private SupplierService supplierService;

    // ── fixtures ──────────────────────────────────────────────────────────────
    private Supplier supplier;
    private SupplierCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        supplier = Supplier.builder()
                .id(1L)
                .name("Proveedor ABC")
                .description("Descripción del proveedor")
                .contactNumber("3001234567")
                .email("proveedor@abc.com")
                .logoUrl("https://res.cloudinary.com/demo/logo.png")
                .build();

        validRequest = new SupplierCreateRequest();
        validRequest.setName("Proveedor ABC");
        validRequest.setDescription("Descripción del proveedor");
        validRequest.setContactNumber("3001234567");
        validRequest.setEmail("proveedor@abc.com");
    }

    // =========================================================================
    // getAllSuppliers
    // =========================================================================
    @Nested
    @DisplayName("getAllSuppliers()")
    class GetAllSuppliers {

        @Test
        @DisplayName("Debe retornar lista de SupplierDTO cuando existen proveedores")
        void shouldReturnSupplierDTOList() {
            when(supplierRepository.findAll()).thenReturn(List.of(supplier));

            List<SupplierDTO> result = supplierService.getAllSuppliers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getName()).isEqualTo("Proveedor ABC");
            verify(supplierRepository).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay proveedores")
        void shouldReturnEmptyList() {
            when(supplierRepository.findAll()).thenReturn(List.of());

            assertThat(supplierService.getAllSuppliers()).isEmpty();
        }
    }

    // =========================================================================
    // getSupplierById
    // =========================================================================
    @Nested
    @DisplayName("getSupplierById()")
    class GetSupplierById {

        @Test
        @DisplayName("Debe retornar SupplierDTO cuando el proveedor existe")
        void shouldReturnSupplierDTO() {
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

            SupplierDTO result = supplierService.getSupplierById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Proveedor ABC");
            assertThat(result.getEmail()).isEqualTo("proveedor@abc.com");
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException cuando el proveedor no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // createSupplier
    // =========================================================================
    @Nested
    @DisplayName("createSupplier()")
    class CreateSupplier {

        @Test
        @DisplayName("Debe crear proveedor sin imagen cuando image es null")
        void shouldCreateSupplierWithoutImage()throws Exception {
            Supplier savedSupplier = supplier.toBuilder().logoUrl(null).build();
            when(supplierRepository.save(any(Supplier.class))).thenReturn(savedSupplier);

            SupplierDTO result = supplierService.createSupplier(validRequest, null);

            assertThat(result.getName()).isEqualTo("Proveedor ABC");
            assertThat(result.getLogoUrl()).isNull();
            verify(imageService, never()).upload(any());
        }

        @Test
        @DisplayName("Debe crear proveedor sin imagen cuando el archivo está vacío")
        void shouldCreateSupplierWithEmptyFile() throws Exception{
            MockMultipartFile emptyFile = new MockMultipartFile("image", new byte[0]);
            Supplier savedSupplier = supplier.toBuilder().logoUrl(null).build();
            when(supplierRepository.save(any(Supplier.class))).thenReturn(savedSupplier);

            SupplierDTO result = supplierService.createSupplier(validRequest, emptyFile);

            assertThat(result.getLogoUrl()).isNull();
            verify(imageService, never()).upload(any(MultipartFile.class));        }

        @Test
        @DisplayName("Debe subir imagen y guardar la URL cuando se provee un archivo válido")
        void shouldCreateSupplierWithImage() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "logo.png", "image/png", "fake-content".getBytes());

            when(imageService.upload(any(MultipartFile.class)))
                    .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/logo.png"));
            when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

            SupplierDTO result = supplierService.createSupplier(validRequest, imageFile);

            assertThat(result.getLogoUrl()).isEqualTo("https://res.cloudinary.com/demo/logo.png");
            verify(imageService).upload(imageFile);
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException cuando falla la subida de imagen")
        void shouldThrowWhenImageUploadFails() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "logo.png", "image/png", "fake-content".getBytes());

            when(imageService.upload(any(MultipartFile.class)))
                    .thenThrow(new RuntimeException("Cloudinary error"));

            assertThatThrownBy(() -> supplierService.createSupplier(validRequest, imageFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error uploading image");

            verify(supplierRepository, never()).save(any());
        }
    }

    // =========================================================================
    // updateSupplier
    // =========================================================================
    @Nested
    @DisplayName("updateSupplier()")
    class UpdateSupplier {

        @Test
        @DisplayName("Debe actualizar campos del proveedor sin cambiar la imagen")
        void shouldUpdateSupplierWithoutChangingImage()throws Exception {
            SupplierCreateRequest updateRequest = new SupplierCreateRequest();
            updateRequest.setName("Proveedor XYZ");
            updateRequest.setDescription("Nueva descripción");
            updateRequest.setContactNumber("3109876543");
            updateRequest.setEmail("nuevo@xyz.com");

            Supplier updatedSupplier = supplier.toBuilder()
                    .name("Proveedor XYZ")
                    .description("Nueva descripción")
                    .contactNumber("3109876543")
                    .email("nuevo@xyz.com")
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
            when(supplierRepository.save(any(Supplier.class))).thenReturn(updatedSupplier);

            SupplierDTO result = supplierService.updateSupplier(1L, updateRequest, null);

            assertThat(result.getName()).isEqualTo("Proveedor XYZ");
            assertThat(result.getEmail()).isEqualTo("nuevo@xyz.com");
            verify(imageService, never()).upload(any());
        }

        @Test
        @DisplayName("Debe actualizar la imagen cuando se provee un archivo válido")
        void shouldUpdateSupplierWithNewImage() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "new-logo.png", "image/png", "new-content".getBytes());

            Supplier updatedSupplier = supplier.toBuilder()
                    .logoUrl("https://res.cloudinary.com/demo/new-logo.png")
                    .build();

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
            when(imageService.upload(any(MultipartFile.class)))
                    .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/new-logo.png"));
            when(supplierRepository.save(any(Supplier.class))).thenReturn(updatedSupplier);

            SupplierDTO result = supplierService.updateSupplier(1L, validRequest, imageFile);

            assertThat(result.getLogoUrl()).isEqualTo("https://res.cloudinary.com/demo/new-logo.png");
            verify(imageService).upload(imageFile);
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el proveedor no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.updateSupplier(99L, validRequest, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("99");

            verify(supplierRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException cuando falla la subida de imagen en update")
        void shouldThrowWhenImageUploadFailsOnUpdate() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "logo.png", "image/png", "fake-content".getBytes());

            when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
            when(imageService.upload(any(MultipartFile.class)))
                    .thenThrow(new RuntimeException("Cloudinary error"));

            assertThatThrownBy(() -> supplierService.updateSupplier(1L, validRequest, imageFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error uploading image");
        }
    }

    // =========================================================================
    // deleteSupplier
    // =========================================================================
    @Nested
    @DisplayName("deleteSupplier()")
    class DeleteSupplier {

        @Test
        @DisplayName("Debe eliminar el proveedor cuando existe")
        void shouldDeleteSupplierSuccessfully() {
            when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

            supplierService.deleteSupplier(1L);

            verify(supplierRepository).delete(supplier);
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el proveedor no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.deleteSupplier(99L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("99");

            verify(supplierRepository, never()).delete(any());
        }
    }

    // =========================================================================
    // getAllSuppliersPaginated
    // =========================================================================
    @Nested
    @DisplayName("getAllSuppliersPaginated()")
    class GetAllSuppliersPaginated {

        @Test
        @DisplayName("Debe retornar respuesta paginada correctamente")
        void shouldReturnPaginatedResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Supplier> page = new PageImpl<>(List.of(supplier), pageable, 1);
            when(supplierRepository.findAll(pageable)).thenReturn(page);

            PaginatedSupplierResponse response = supplierService.getAllSuppliersPaginated(pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getPageNumber()).isZero();
            assertThat(response.getPageSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(1L);
            assertThat(response.getTotalPages()).isEqualTo(1);
            assertThat(response.isLast()).isTrue();
        }

        @Test
        @DisplayName("Debe retornar página vacía cuando no hay proveedores")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Supplier> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(supplierRepository.findAll(pageable)).thenReturn(emptyPage);

            PaginatedSupplierResponse response = supplierService.getAllSuppliersPaginated(pageable);

            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    // =========================================================================
    // toDTO / toResponse (helpers)
    // =========================================================================
    @Nested
    @DisplayName("Mapeo toDTO() y toResponse()")
    class Mapping {

        @Test
        @DisplayName("toDTO debe mapear todos los campos correctamente")
        void shouldMapAllFieldsToDTO() {
            SupplierDTO dto = supplierService.toDTO(supplier);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Proveedor ABC");
            assertThat(dto.getDescription()).isEqualTo("Descripción del proveedor");
            assertThat(dto.getContactNumber()).isEqualTo("3001234567");
            assertThat(dto.getEmail()).isEqualTo("proveedor@abc.com");
            assertThat(dto.getLogoUrl()).isEqualTo("https://res.cloudinary.com/demo/logo.png");
        }

        @Test
        @DisplayName("toResponse debe mapear id, name y logoUrl correctamente")
        void shouldMapFieldsToResponse() {
            var response = supplierService.toResponse(supplier);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Proveedor ABC");
            assertThat(response.getLogoUrl()).isEqualTo("https://res.cloudinary.com/demo/logo.png");
        }
    }
}