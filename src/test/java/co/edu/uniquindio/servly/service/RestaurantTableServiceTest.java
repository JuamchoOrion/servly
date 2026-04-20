package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.exception.AuthException;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.repository.RestaurantTableRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantTableService Tests")
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository tableRepository;

    @InjectMocks
    private RestaurantTableService tableService;

    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = RestaurantTable.builder()
                .id(1)
                .tableNumber(1)
                .capacity(4)
                .status(RestaurantTable.TableStatus.AVAILABLE)
                .location("Piso 1, Esquina")
                .build();
    }

    @Nested
    @DisplayName("createTable Tests")
    class CreateTable {

        @Test
        @DisplayName("Debe crear una mesa exitosamente")
        void shouldCreateTableSuccessfully() {
            // Arrange
            when(tableRepository.existsByTableNumber(1)).thenReturn(false);
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.createTable(1, 4, "Piso 1, Esquina");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTableNumber());
            assertEquals(4, result.getCapacity());
            assertEquals("Piso 1, Esquina", result.getLocation());
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, result.getStatus());
            verify(tableRepository, times(1)).save(any(RestaurantTable.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el número de mesa es null")
        void shouldThrowExceptionWhenTableNumberIsNull() {
            // Act & Assert
            assertThrows(AuthException.class, () -> tableService.createTable(null, 4, "Piso 1"));
            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando el número de mesa es menor a 1")
        void shouldThrowExceptionWhenTableNumberLessThanOne() {
            // Act & Assert
            assertThrows(AuthException.class, () -> tableService.createTable(0, 4, "Piso 1"));
            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la capacidad es null")
        void shouldThrowExceptionWhenCapacityIsNull() {
            // Act & Assert
            assertThrows(AuthException.class, () -> tableService.createTable(1, null, "Piso 1"));
            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la capacidad es menor a 1")
        void shouldThrowExceptionWhenCapacityLessThanOne() {
            // Act & Assert
            assertThrows(AuthException.class, () -> tableService.createTable(1, 0, "Piso 1"));
            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la mesa ya existe")
        void shouldThrowExceptionWhenTableAlreadyExists() {
            // Arrange
            when(tableRepository.existsByTableNumber(1)).thenReturn(true);

            // Act & Assert
            AuthException exception = assertThrows(AuthException.class,
                    () -> tableService.createTable(1, 4, "Piso 1"));
            assertTrue(exception.getMessage().contains("ya existe"));
            verify(tableRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTableByNumber Tests")
    class GetTableByNumber {

        @Test
        @DisplayName("Debe obtener una mesa por número")
        void shouldGetTableByNumberSuccessfully() {
            // Arrange
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));

            // Act
            RestaurantTable result = tableService.getTableByNumber(1);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTableNumber());
            assertEquals(4, result.getCapacity());
            verify(tableRepository, times(1)).findByTableNumber(1);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la mesa no existe")
        void shouldThrowExceptionWhenTableNotFound() {
            // Arrange
            when(tableRepository.findByTableNumber(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AuthException.class, () -> tableService.getTableByNumber(999));
            assertTrue(assertThrows(AuthException.class,
                    () -> tableService.getTableByNumber(999)).getMessage().contains("no existe"));
        }
    }

    @Nested
    @DisplayName("updateTableStatus Tests")
    class UpdateTableStatus {

        @Test
        @DisplayName("Debe cambiar el estado de una mesa a OCCUPIED")
        void shouldUpdateStatusToOccupied() {
            // Arrange
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.updateTableStatus(1, RestaurantTable.TableStatus.OCCUPIED);

            // Assert
            assertNotNull(result);
            assertEquals(RestaurantTable.TableStatus.OCCUPIED, table.getStatus());
            verify(tableRepository, times(1)).save(any(RestaurantTable.class));
        }

        @Test
        @DisplayName("Debe cambiar el estado de una mesa a MAINTENANCE")
        void shouldUpdateStatusToMaintenance() {
            // Arrange
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.updateTableStatus(1, RestaurantTable.TableStatus.MAINTENANCE);

            // Assert
            assertNotNull(result);
            assertEquals(RestaurantTable.TableStatus.MAINTENANCE, table.getStatus());
            verify(tableRepository, times(1)).save(any(RestaurantTable.class));
        }

        @Test
        @DisplayName("Debe cambiar el estado de una mesa a RESERVED")
        void shouldUpdateStatusToReserved() {
            // Arrange
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.updateTableStatus(1, RestaurantTable.TableStatus.RESERVED);

            // Assert
            assertNotNull(result);
            assertEquals(RestaurantTable.TableStatus.RESERVED, table.getStatus());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la mesa no existe")
        void shouldThrowExceptionWhenTableNotFound() {
            // Arrange
            when(tableRepository.findByTableNumber(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AuthException.class,
                    () -> tableService.updateTableStatus(999, RestaurantTable.TableStatus.OCCUPIED));
            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe cambiar el estado de OCCUPIED a AVAILABLE")
        void shouldChangeFromOccupiedToAvailable() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.updateTableStatus(1, RestaurantTable.TableStatus.AVAILABLE);

            // Assert
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, table.getStatus());
            verify(tableRepository, times(1)).save(any(RestaurantTable.class));
        }
    }

    @Nested
    @DisplayName("getAllTables Tests")
    class GetAllTables {

        @Test
        @DisplayName("Debe obtener todas las mesas ordenadas por número")
        void shouldGetAllTablesOrderedByNumber() {
            // Arrange
            RestaurantTable table2 = RestaurantTable.builder()
                    .id(2)
                    .tableNumber(2)
                    .capacity(6)
                    .status(RestaurantTable.TableStatus.OCCUPIED)
                    .location("Piso 2")
                    .build();

            List<RestaurantTable> tables = List.of(table, table2);
            when(tableRepository.findAllOrderedByTableNumber()).thenReturn(tables);

            // Act
            List<RestaurantTable> result = tableService.getAllTables();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getTableNumber());
            assertEquals(2, result.get(1).getTableNumber());
            verify(tableRepository, times(1)).findAllOrderedByTableNumber();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay mesas")
        void shouldReturnEmptyListWhenNoTables() {
            // Arrange
            when(tableRepository.findAllOrderedByTableNumber()).thenReturn(List.of());

            // Act
            List<RestaurantTable> result = tableService.getAllTables();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getTablesByStatus Tests")
    class GetTablesByStatus {

        @Test
        @DisplayName("Debe obtener todas las mesas disponibles")
        void shouldGetAllAvailableTables() {
            // Arrange
            List<RestaurantTable> tables = List.of(table);
            when(tableRepository.findByStatus(RestaurantTable.TableStatus.AVAILABLE))
                    .thenReturn(tables);

            // Act
            List<RestaurantTable> result = tableService.getTablesByStatus(RestaurantTable.TableStatus.AVAILABLE);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, result.get(0).getStatus());
            verify(tableRepository, times(1)).findByStatus(RestaurantTable.TableStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Debe obtener todas las mesas ocupadas")
        void shouldGetAllOccupiedTables() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            List<RestaurantTable> tables = List.of(table);
            when(tableRepository.findByStatus(RestaurantTable.TableStatus.OCCUPIED))
                    .thenReturn(tables);

            // Act
            List<RestaurantTable> result = tableService.getTablesByStatus(RestaurantTable.TableStatus.OCCUPIED);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(RestaurantTable.TableStatus.OCCUPIED, result.get(0).getStatus());
        }

        @Test
        @DisplayName("Debe obtener todas las mesas en mantenimiento")
        void shouldGetAllMaintenanceTables() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.MAINTENANCE);
            List<RestaurantTable> tables = List.of(table);
            when(tableRepository.findByStatus(RestaurantTable.TableStatus.MAINTENANCE))
                    .thenReturn(tables);

            // Act
            List<RestaurantTable> result = tableService.getTablesByStatus(RestaurantTable.TableStatus.MAINTENANCE);

            // Assert
            assertEquals(1, result.size());
            assertEquals(RestaurantTable.TableStatus.MAINTENANCE, result.get(0).getStatus());
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay mesas con ese estado")
        void shouldReturnEmptyListWhenNoTablesWithStatus() {
            // Arrange
            when(tableRepository.findByStatus(RestaurantTable.TableStatus.RESERVED))
                    .thenReturn(List.of());

            // Act
            List<RestaurantTable> result = tableService.getTablesByStatus(RestaurantTable.TableStatus.RESERVED);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("deleteTable Tests")
    class DeleteTable {

        @Test
        @DisplayName("Debe eliminar una mesa correctamente")
        void shouldDeleteTableSuccessfully() {
            // Arrange
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            doNothing().when(tableRepository).delete(any(RestaurantTable.class));

            // Act
            tableService.deleteTable(1);

            // Assert
            verify(tableRepository, times(1)).delete(any(RestaurantTable.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando intenta eliminar una mesa que no existe")
        void shouldThrowExceptionWhenDeletingNonExistentTable() {
            // Arrange
            when(tableRepository.findByTableNumber(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AuthException.class, () -> tableService.deleteTable(999));
            verify(tableRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("TableStatus Transitions Tests")
    class TableStatusTransitions {

        @Test
        @DisplayName("Debe permitir cambio de AVAILABLE a OCCUPIED")
        void shouldAllowTransitionFromAvailableToOccupied() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            tableService.updateTableStatus(1, RestaurantTable.TableStatus.OCCUPIED);

            // Assert
            assertEquals(RestaurantTable.TableStatus.OCCUPIED, table.getStatus());
        }

        @Test
        @DisplayName("Debe permitir cambio de OCCUPIED a AVAILABLE")
        void shouldAllowTransitionFromOccupiedToAvailable() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            tableService.updateTableStatus(1, RestaurantTable.TableStatus.AVAILABLE);

            // Assert
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, table.getStatus());
        }

        @Test
        @DisplayName("Debe permitir cambio a MAINTENANCE desde cualquier estado")
        void shouldAllowTransitionToMaintenanceFromAnyStatus() {
            // Arrange
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            tableService.updateTableStatus(1, RestaurantTable.TableStatus.MAINTENANCE);

            // Assert
            assertEquals(RestaurantTable.TableStatus.MAINTENANCE, table.getStatus());
        }

        @Test
        @DisplayName("Debe permitir cambio a RESERVED desde cualquier estado")
        void shouldAllowTransitionToReservedFromAnyStatus() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            when(tableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            tableService.updateTableStatus(1, RestaurantTable.TableStatus.RESERVED);

            // Assert
            assertEquals(RestaurantTable.TableStatus.RESERVED, table.getStatus());
        }
    }

    @Nested
    @DisplayName("Table Capacity Tests")
    class TableCapacity {

        @Test
        @DisplayName("Debe crear mesa con capacidad correcta")
        void shouldCreateTableWithCorrectCapacity() {
            // Arrange
            when(tableRepository.existsByTableNumber(1)).thenReturn(false);
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.createTable(1, 4, "Piso 1");

            // Assert
            assertEquals(4, result.getCapacity());
        }

        @Test
        @DisplayName("Debe crear mesa con capacidad de 1 persona")
        void shouldCreateTableWithCapacityOfOne() {
            // Arrange
            RestaurantTable singleTable = RestaurantTable.builder()
                    .id(5)
                    .tableNumber(5)
                    .capacity(1)
                    .status(RestaurantTable.TableStatus.AVAILABLE)
                    .build();

            when(tableRepository.existsByTableNumber(5)).thenReturn(false);
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(singleTable);

            // Act
            RestaurantTable result = tableService.createTable(5, 1, "Barra");

            // Assert
            assertEquals(1, result.getCapacity());
        }

        @Test
        @DisplayName("Debe crear mesa con capacidad grande")
        void shouldCreateTableWithLargeCapacity() {
            // Arrange
            RestaurantTable largeTable = RestaurantTable.builder()
                    .id(10)
                    .tableNumber(10)
                    .capacity(20)
                    .status(RestaurantTable.TableStatus.AVAILABLE)
                    .build();

            when(tableRepository.existsByTableNumber(10)).thenReturn(false);
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(largeTable);

            // Act
            RestaurantTable result = tableService.createTable(10, 20, "Salón");

            // Assert
            assertEquals(20, result.getCapacity());
        }
    }

    @Nested
    @DisplayName("Table Location Tests")
    class TableLocation {

        @Test
        @DisplayName("Debe crear mesa con ubicación")
        void shouldCreateTableWithLocation() {
            // Arrange
            when(tableRepository.existsByTableNumber(1)).thenReturn(false);
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

            // Act
            RestaurantTable result = tableService.createTable(1, 4, "Piso 1, Esquina");

            // Assert
            assertEquals("Piso 1, Esquina", result.getLocation());
        }

        @Test
        @DisplayName("Debe permitir crear mesa sin ubicación (null)")
        void shouldAllowNullLocation() {
            // Arrange
            RestaurantTable tableNoLocation = RestaurantTable.builder()
                    .id(1)
                    .tableNumber(1)
                    .capacity(4)
                    .status(RestaurantTable.TableStatus.AVAILABLE)
                    .location(null)
                    .build();

            when(tableRepository.existsByTableNumber(1)).thenReturn(false);
            when(tableRepository.save(any(RestaurantTable.class))).thenReturn(tableNoLocation);

            // Act
            RestaurantTable result = tableService.createTable(1, 4, null);

            // Assert
            assertNull(result.getLocation());
        }
    }
}

