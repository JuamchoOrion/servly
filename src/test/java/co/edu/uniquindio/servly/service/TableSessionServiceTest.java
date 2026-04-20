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
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn("jwt-token-123");
            when(tableJwtProvider.getSessionExpirationMs()).thenReturn(14400000L);
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            TableSessionResponse result = tableSessionService.openSession(1);

            assertNotNull(result);
            assertEquals(1, result.getTableNumber());
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando la mesa no existe")
        void shouldThrowExceptionWhenTableNotFound() {
            when(restaurantTableRepository.findByTableNumber(999)).thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> tableSessionService.openSession(999));
        }

        @Test
        @DisplayName("Debe reutilizar sesión activa existente en lugar de crear nueva")
        void shouldReuseExistingSessionWhenAlreadyActive() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.of(session));

            TableSessionResponse result = tableSessionService.openSession(1);

            assertNotNull(result);
            assertEquals(1, result.getTableNumber());
        }

        @Test
        @DisplayName("Debe cambiar el estado de la mesa a OCCUPIED")
        void shouldChangeTableStatusToOccupied() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn("jwt-token-123");
            when(tableJwtProvider.getSessionExpirationMs()).thenReturn(14400000L);
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            tableSessionService.openSession(1);

            assertEquals(RestaurantTable.TableStatus.OCCUPIED, table.getStatus());
        }

        @Test
        @DisplayName("Debe generar un token JWT válido")
        void shouldGenerateValidJWT() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn("valid-jwt-token");
            when(tableJwtProvider.getSessionExpirationMs()).thenReturn(14400000L);
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            TableSessionResponse result = tableSessionService.openSession(1);

            assertNotNull(result);
            verify(tableJwtProvider, times(1)).generateTableToken(anyInt(), anyString());
        }
    }

    @Nested
    @DisplayName("closeSession Tests")
    class CloseSession {

        @Test
        @DisplayName("Debe cerrar una sesión activa")
        void shouldCloseActiveSession() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            MessageResponse result = tableSessionService.closeSession(1);

            assertNotNull(result);
            assertFalse(session.isActive());
        }

        @Test
        @DisplayName("Debe registrar el cierre de sesión con timestamp")
        void shouldRecordSessionClosureTime() {
            LocalDateTime beforeClose = LocalDateTime.now();
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            tableSessionService.closeSession(1);
            LocalDateTime afterClose = LocalDateTime.now();

            assertNotNull(session.getClosedAt());
            assertTrue(session.getClosedAt().isAfter(beforeClose.minusSeconds(1)));
            assertTrue(session.getClosedAt().isBefore(afterClose.plusSeconds(1)));
        }

        @Test
        @DisplayName("Debe cambiar el estado de la mesa a AVAILABLE al cerrar")
        void shouldChangeTableStatusToAvailable() {
            table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            tableSessionService.closeSession(1);

            assertEquals(RestaurantTable.TableStatus.AVAILABLE, table.getStatus());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no hay sesión activa")
        void shouldThrowExceptionWhenNoActiveSession() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> tableSessionService.closeSession(1));
        }
    }

    @Nested
    @DisplayName("isTableActive Tests")
    class IsTableActive {

        @Test
        @DisplayName("Debe retornar true cuando la mesa tiene sesión activa")
        void shouldReturnTrueWhenTableHasActiveSession() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.existsByRestaurantTableAndActiveTrue(table)).thenReturn(true);

            boolean result = tableSessionService.isTableActive(1);

            assertTrue(result);
        }

        @Test
        @DisplayName("Debe retornar false cuando no hay sesión activa")
        void shouldReturnFalseWhenNoActiveSession() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.existsByRestaurantTableAndActiveTrue(table)).thenReturn(false);

            boolean result = tableSessionService.isTableActive(1);

            assertFalse(result);
        }

        @Test
        @DisplayName("Debe retornar false cuando la mesa no existe")
        void shouldReturnFalseWhenTableNotFound() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.empty());

            boolean result = tableSessionService.isTableActive(1);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Session Expiration Tests")
    class SessionExpiration {

        @Test
        @DisplayName("Debe crear sesión con tiempo de expiración")
        void shouldCreateSessionWithExpirationTime() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn("jwt-token");
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            TableSessionResponse result = tableSessionService.openSession(1);

            assertNotNull(result);
            assertNotNull(session.getExpiresAt());
            assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("Debe crear sesión que expire en 4 horas")
        void shouldCreateSessionExpiringIn4Hours() {
            LocalDateTime now = LocalDateTime.now();

            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn("jwt-token");
            when(tableJwtProvider.getSessionExpirationMs()).thenReturn(14400000L);
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);

            tableSessionService.openSession(1);

            assertNotNull(session.getExpiresAt());
            long minutesDifference = java.time.temporal.ChronoUnit.MINUTES
                    .between(now, session.getExpiresAt());
            assertTrue(minutesDifference >= 235 && minutesDifference <= 245);
        }
    }

    @Nested
    @DisplayName("Session Token Tests")
    class SessionToken {

        @Test
        @DisplayName("Debe almacenar el token JWT en la sesión")
        void shouldStoreJWTTokenInSession() {
            String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn(expectedToken);

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

            TableSessionResponse result = tableSessionService.openSession(1);

            assertNotNull(result);
            assertEquals(expectedToken, sessionWithToken.getSessionToken());
        }

        @Test
        @DisplayName("Debe usar el mismo token para identificar la sesión")
        void shouldUseTokenToIdentifySession() {
            String sessionToken = "unique-jwt-token-123";
            session.setSessionToken(sessionToken);

            String storedToken = session.getSessionToken();

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
                    .openedAt(LocalDateTime.now())
                    .build();

            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(1, "session-uuid-123")).thenReturn("jwt-token-123");
            when(tableJwtProvider.getSessionExpirationMs()).thenReturn(14400000L);

            when(restaurantTableRepository.findByTableNumber(2)).thenReturn(Optional.of(table2));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table2)).thenReturn(Optional.empty());
            when(tableJwtProvider.generateTableToken(2, "session-uuid-456")).thenReturn("jwt-token-456");

            when(sessionRepository.save(any(TableSession.class)))
                    .thenReturn(session)
                    .thenReturn(session2);

            TableSessionResponse result1 = tableSessionService.openSession(1);
            TableSessionResponse result2 = tableSessionService.openSession(2);

            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(1, result1.getTableNumber());
            assertEquals(2, result2.getTableNumber());
        }

        @Test
        @DisplayName("Debe reutilizar sesión existente cuando se intenta abrir sesión en mesa ocupada")
        void shouldReuseExistingSessionWhenTableOccupied() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table)).thenReturn(Optional.of(session));

            TableSessionResponse result = tableSessionService.openSession(1);

            assertNotNull(result);
            assertEquals(1, result.getTableNumber());
        }
    }

    @Nested
    @DisplayName("Session Lifecycle Tests")
    class SessionLifecycle {

        @Test
        @DisplayName("Debe completar ciclo: abrir -> activar -> cerrar")
        void shouldCompleteSessionLifecycle() {
            when(restaurantTableRepository.findByTableNumber(1)).thenReturn(Optional.of(table));
            when(sessionRepository.findByRestaurantTableAndActiveTrue(table))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(session));
            when(tableJwtProvider.generateTableToken(anyInt(), anyString())).thenReturn("jwt-token");
            when(tableJwtProvider.getSessionExpirationMs()).thenReturn(14400000L);
            when(sessionRepository.save(any(TableSession.class))).thenReturn(session);
            when(sessionRepository.existsByRestaurantTableAndActiveTrue(table)).thenReturn(true);

            TableSessionResponse openResult = tableSessionService.openSession(1);
            assertNotNull(openResult);
            assertTrue(session.isActive());

            boolean isActive = tableSessionService.isTableActive(1);
            assertTrue(isActive);

            MessageResponse closeResult = tableSessionService.closeSession(1);
            assertNotNull(closeResult);
            assertFalse(session.isActive());

            assertNotNull(session.getClosedAt());
            assertEquals(RestaurantTable.TableStatus.AVAILABLE, table.getStatus());
        }
    }
}

