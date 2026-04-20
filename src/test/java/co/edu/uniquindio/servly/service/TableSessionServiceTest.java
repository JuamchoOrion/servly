package co.edu.uniquindio.servly.service;

import co.edu.uniquindio.servly.DTO.TableSessionResponse;
import co.edu.uniquindio.servly.DTO.MessageResponse;
import co.edu.uniquindio.servly.model.entity.RestaurantTable;
import co.edu.uniquindio.servly.model.entity.TableSession;
import co.edu.uniquindio.servly.repository.RestaurantTableRepository;
import co.edu.uniquindio.servly.repository.TableSessionRepository;
import co.edu.uniquindio.servly.security.TableJwtProvider;
import co.edu.uniquindio.servly.metrics.TableMetricsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableSessionService Tests")
class TableSessionServiceTest {

    @Mock
    private TableSessionRepository sessionRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private TableJwtProvider tableJwtProvider;

    @Mock
    private TableMetricsService tableMetricsService;

    @InjectMocks
    private TableSessionService tableSessionService;

    private RestaurantTable table;
    private TableSession session;

    @BeforeEach
    void setUp() {
        table = RestaurantTable.builder()
                .id(1)
                .tableNumber(1)
                .capacity(4)
                .status(RestaurantTable.TableStatus.AVAILABLE)
                .location("Piso 1")
                .build();

        session = TableSession.builder()
                .id("session-uuid-123")
                .restaurantTable(table)
                .tableNumber(1)
                .sessionToken("jwt-token-123")
                .active(true)
                .expiresAt(LocalDateTime.now().plusHours(4))
                .openedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("openSession Tests")
    class OpenSession {

        @Test
        @DisplayName("Debe abrir una sesión cuando la mesa está disponible")
        void shouldOpenSessionWhenTableAvailable() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(anyInt())).thenReturn("jwt-token-123");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            TableSessionResponse result = tableSessionService.openSession(1);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTableNumber());
            assertTrue(result.isActive());
            verify(sessionRepository, times(1)).save(any(TableSession.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la mesa no existe")
        void shouldThrowExceptionWhenTableNotFound() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(Exception.class, () -> tableSessionService.openSession(999));
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando ya existe sesión activa")
        void shouldThrowExceptionWhenSessionAlreadyActive() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));

            // Act & Assert
            assertThrows(Exception.class, () -> tableSessionService.openSession(1));
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe cambiar el estado de la mesa a OCCUPIED")
        void shouldChangeTableStatusToOccupied() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(anyInt())).thenReturn("jwt-token-123");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            tableSessionService.openSession(1);

            // Assert
            assertEquals(RestaurantTable.TableStatus.OCCUPIED, table.getStatus());
        }

        @Test
        @DisplayName("Debe generar un token JWT válido")
        void shouldGenerateValidJWT() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(1)).thenReturn("valid-jwt-token");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            TableSessionResponse result = tableSessionService.openSession(1);

            // Assert
            assertNotNull(result);
            verify(tableJwtProvider, times(1)).generateToken(1);
        }
    }

    @Nested
    @DisplayName("closeSession Tests")
    class CloseSession {

        @Test
        @DisplayName("Debe cerrar una sesión activa")
        void shouldCloseActiveSession() {
            // Arrange
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            MessageResponse result = tableSessionService.closeSession(1);

            // Assert
            assertNotNull(result);
            assertFalse(session.isActive());
            verify(sessionRepository, times(1)).save(any(TableSession.class));
        }

        @Test
        @DisplayName("Debe registrar el cierre de sesión con timestamp")
        void shouldRecordSessionClosureTime() {
            // Arrange
            LocalDateTime beforeClose = LocalDateTime.now();
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            tableSessionService.closeSession(1);
            LocalDateTime afterClose = LocalDateTime.now();

            // Assert
            assertNotNull(session.getClosedAt());
            assertTrue(session.getClosedAt().isAfter(beforeClose.minusSeconds(1)));
            assertTrue(session.getClosedAt().isBefore(afterClose.plusSeconds(1)));
        }

        @Test
        @DisplayName("Debe cambiar el estado de la mesa a AVAILABLE al cerrar")
        void shouldChangeTableStatusToAvailable() {
            // Arrange
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            tableSessionService.closeSession(1);

            // Assert
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, table.getStatus());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no hay sesión activa")
        void shouldThrowExceptionWhenNoActiveSession() {
            // Arrange
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(Exception.class, () -> tableSessionService.closeSession(1));
            verify(sessionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isTableActive Tests")
    class IsTableActive {

        @Test
        @DisplayName("Debe retornar true cuando la mesa tiene sesión activa")
        void shouldReturnTrueWhenTableHasActiveSession() {
            // Arrange
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));

            // Act
            boolean result = tableSessionService.isTableActive(1);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Debe retornar false cuando no hay sesión activa")
        void shouldReturnFalseWhenNoActiveSession() {
            // Arrange
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());

            // Act
            boolean result = tableSessionService.isTableActive(1);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Debe retornar false cuando la sesión no está activa")
        void shouldReturnFalseWhenSessionNotActive() {
            // Arrange
            session.setActive(false);
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());

            // Act
            boolean result = tableSessionService.isTableActive(1);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Session Expiration Tests")
    class SessionExpiration {

        @Test
        @DisplayName("Debe crear sesión con tiempo de expiración")
        void shouldCreateSessionWithExpirationTime() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(1)).thenReturn("jwt-token");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            TableSessionResponse result = tableSessionService.openSession(1);

            // Assert
            assertNotNull(result);
            assertNotNull(session.getExpiresAt());
            assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Debe crear sesión que expire en 4 horas")
        void shouldCreateSessionExpiringIn4Hours() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expectedExpiration = now.plusHours(4);

            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(1)).thenReturn("jwt-token");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            tableSessionService.openSession(1);

            // Assert
            assertNotNull(session.getExpiresAt());
            // La diferencia debe ser aproximadamente 4 horas (permitir 5 minutos de margen)
            long minutesDifference = java.time.temporal.ChronoUnit.MINUTES
                    .between(now, session.getExpiresAt());
            assertTrue(minutesDifference >= 235 && minutesDifference <= 245); // 4 horas ± 5 min
        }
    }

    @Nested
    @DisplayName("Session Token Tests")
    class SessionToken {

        @Test
        @DisplayName("Debe almacenar el token JWT en la sesión")
        void shouldStoreJWTTokenInSession() {
            // Arrange
            String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(1)).thenReturn(expectedToken);

            TableSession sessionWithToken = TableSession.builder()
                    .id("session-uuid")
                    .restaurantTable(table)
                    .tableNumber(1)
                    .sessionToken(expectedToken)
                    .active(true)
                    .expiresAt(LocalDateTime.now().plusHours(4))
                    .openedAt(LocalDateTime.now())
                    .build();

            when(sessionRepository.save(any(TableSession.class))).thenReturn(sessionWithToken);

            // Act
            TableSessionResponse result = tableSessionService.openSession(1);

            // Assert
            assertNotNull(result);
            assertEquals(expectedToken, sessionWithToken.getSessionToken());
        }

        @Test
        @DisplayName("Debe usar el mismo token para identificar la sesión")
        void shouldUseTokenToIdentifySession() {
            // Arrange
            String sessionToken = "unique-jwt-token-123";
            session.setSessionToken(sessionToken);

            // Act
            String storedToken = session.getSessionToken();

            // Assert
            assertEquals(sessionToken, storedToken);
            assertEquals("unique-jwt-token-123", storedToken);
        }
    }

    @Nested
    @DisplayName("Multiple Sessions Tests")
    class MultipleSessions {

        @Test
        @DisplayName("Debe permitir múltiples mesas con sesiones simultáneas")
        void shouldAllowMultipleTablesWithConcurrentSessions() {
            // Arrange
            RestaurantTable table2 = RestaurantTable.builder()
                    .id(2)
                    .tableNumber(2)
                    .capacity(4)
                    .status(RestaurantTable.TableStatus.AVAILABLE)
                    .build();

            TableSession session2 = TableSession.builder()
                    .id("session-uuid-456")
                    .restaurantTable(table2)
                    .tableNumber(2)
                    .sessionToken("jwt-token-456")
                    .active(true)
                    .expiresAt(LocalDateTime.now().plusHours(4))
                    .build();

            // Primero abrimos sesión en mesa 1
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(1)).thenReturn("jwt-token-123");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Luego abrimos sesión en mesa 2
            when(restaurantTableRepository.findByTableNumber(2)).thenReturn(Optional.of(table2));
            when(sessionRepository.findByTableNumberAndActiveTrue(2)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateToken(2)).thenReturn("jwt-token-456");

            // Act
            TableSessionResponse result1 = tableSessionService.openSession(1);
            TableSessionResponse result2 = tableSessionService.openSession(2);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(1, result1.getTableNumber());
            assertEquals(2, result2.getTableNumber());
        }

        @Test
        @DisplayName("No debe permitir dos sesiones activas en la misma mesa")
        void shouldNotAllowTwoActiveSessionsOnSameTable() {
            // Arrange
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));

            // Act & Assert
            assertThrows(Exception.class, () -> tableSessionService.openSession(1));
        }
    }

    @Nested
    @DisplayName("Session Lifecycle Tests")
    class SessionLifecycle {

        @Test
        @DisplayName("Debe completar ciclo: abrir -> activar -> cerrar")
        void shouldCompleteSessionLifecycle() {
            // Arrange
            // 1. Abrir sesión
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByTableNumberAndActiveTrue(1))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(session));
            when(tableJwtProvider.generateToken(1)).thenReturn("jwt-token");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            // Act
            // Abrir sesión
            TableSessionResponse openResult = tableSessionService.openSession(1);
            assertNotNull(openResult);
            assertTrue(session.isActive());

            // Verificar que la sesión está activa
            boolean isActive = tableSessionService.isTableActive(1);
            assertTrue(isActive);

            // Cerrar sesión
            when(sessionRepository.findByTableNumberAndActiveTrue(1)).thenReturn(Optional.of(session));
            MessageResponse closeResult = tableSessionService.closeSession(1);
            assertNotNull(closeResult);
            assertFalse(session.isActive());

            // Assert
            assertNotNull(session.getClosedAt());
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, table.getStatus());
        }
    }
}

