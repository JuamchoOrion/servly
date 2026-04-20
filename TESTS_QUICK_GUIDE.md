## 🧪 Guía Rápida de Ejecución de Tests

### Ejecutar TODOS los tests
```bash
./gradlew test
```

### Ejecutar tests específicos

#### OrderServiceTest
```bash
# Todos los tests de OrderService
./gradlew test --tests OrderServiceTest

# Solo tests de crear órdenes delivery
./gradlew test --tests OrderServiceTest.CreateDeliveryOrder

# Test específico
./gradlew test --tests "OrderServiceTest.CreateDeliveryOrder.shouldCreateDeliveryOrderSuccessfully"

# Tests de cancelar órdenes
./gradlew test --tests OrderServiceTest.CancelOrder

# Tests de confirmar pago
./gradlew test --tests OrderServiceTest.ConfirmPayment
```

#### RestaurantTableServiceTest
```bash
# Todos los tests de RestaurantTableService
./gradlew test --tests RestaurantTableServiceTest

# Tests de crear mesas
./gradlew test --tests RestaurantTableServiceTest.CreateTable

# Tests de cambiar estado
./gradlew test --tests RestaurantTableServiceTest.UpdateTableStatus

# Tests de transiciones de estado
./gradlew test --tests RestaurantTableServiceTest.TableStatusTransitions

# Tests por capacidad
./gradlew test --tests RestaurantTableServiceTest.TableCapacity
```

#### TableSessionServiceTest
```bash
# Todos los tests de TableSessionService
./gradlew test --tests TableSessionServiceTest

# Tests de abrir sesión
./gradlew test --tests TableSessionServiceTest.OpenSession

# Tests de cerrar sesión
./gradlew test --tests TableSessionServiceTest.CloseSession

# Tests del ciclo de vida
./gradlew test --tests TableSessionServiceTest.SessionLifecycle

# Tests de sesiones múltiples
./gradlew test --tests TableSessionServiceTest.MultipleSessions
```

### Ver resultados detallados
```bash
# Ejecutar con salida verbosa
./gradlew test --info

# Mostrar salida de los tests en consola
./gradlew test -i
```

### Generar reporte de cobertura (Jacoco)
```bash
# Generar reporte de cobertura
./gradlew test jacocoTestReport

# Abrir reporte (en Windows)
start build\reports\jacoco\test\html\index.html

# Abrir reporte (en Mac/Linux)
open build/reports/jacoco/test/html/index.html
```

### Ver reporte HTML de pruebas
```bash
# El reporte automático está en:
build/reports/tests/test/index.html
```

### Debug de tests
```bash
# Ejecutar tests con debug
./gradlew test --debug

# Ejecutar un test específico con debug
./gradlew test --tests OrderServiceTest.CancelOrder --debug
```

### Ejecutar tests sin cache
```bash
./gradlew clean test
```

### Ejecutar solo tests que fallaron
```bash
./gradlew test --tests-rerun-failed
```

---

## 📊 Resumen de Tests por Módulo

### OrderServiceTest - 35+ tests
- CreateDeliveryOrder (3 tests)
- CreateTableOrderFromStaff (3 tests)
- UpdateOrderStatus (2 tests)
- CancelOrder (4 tests)
- ConfirmPayment (2 tests)
- GetOrdersByTable (2 tests)
- GetOrdersByStatus (2 tests)
- CalculateTotal (1 test)
- GetOrderById (2 tests)
- GetOrdersByDate (1 test)

### RestaurantTableServiceTest - 40+ tests
- CreateTable (6 tests)
- GetTableByNumber (2 tests)
- UpdateTableStatus (6 tests)
- GetAllTables (2 tests)
- GetTablesByStatus (4 tests)
- DeleteTable (2 tests)
- TableStatusTransitions (4 tests)
- TableCapacity (3 tests)
- TableLocation (2 tests)

### TableSessionServiceTest - 30+ tests
- OpenSession (4 tests)
- CloseSession (4 tests)
- IsTableActive (3 tests)
- SessionExpiration (2 tests)
- SessionToken (2 tests)
- MultipleSessions (2 tests)
- SessionLifecycle (1 test)

**TOTAL: 105+ tests unitarios**

---

## 🎯 Ejemplos de Ejecución

### Ejecutar todo y generar reporte
```bash
./gradlew clean test jacocoTestReport
```

### Ejecutar tests de órdenes y mesas juntos
```bash
./gradlew test --tests "OrderServiceTest|RestaurantTableServiceTest"
```

### Ejecutar solo tests de cancelación y pago
```bash
./gradlew test --tests "OrderServiceTest.CancelOrder|OrderServiceTest.ConfirmPayment"
```

### Ejecutar tests de cambio de estado de mesa
```bash
./gradlew test --tests RestaurantTableServiceTest --tests-rerun-failed
```

### Verificar que un test específico pase
```bash
./gradlew test --tests "OrderServiceTest.CreateTableOrderFromStaff.shouldCreateTableOrderSuccessfully"
```

---

## 💡 Tips Útiles

1. **Agregar nuevo test**: Copiar estructura del @Nested existente
2. **Debugging**: Agregar `@Test` sobre el test y ejecutar con IDE
3. **Mock objects**: Usar `when()` para configurar comportamiento
4. **Verify**: Usar `verify()` para asegurar que se llamaron métodos
5. **AssertJ**: Se puede usar para assertions más legibles (opcional)

---

## ⚠️ Troubleshooting

### Tests fallan con "No qualifying bean"
- Verificar que los @Mock están anotados correctamente
- Confirmar que las dependencias son inyectadas

### "Cannot find symbol" en tests
- Revisar imports de las clases
- Confirmar que las clases existen en el proyecto

### "NullPointerException" en tests
- Verificar que los mocks están configurados con `when()`
- Asegurar que `@BeforeEach` inicializa todos los objetos

### Tests lentos
- Reducir cantidad de tests por clase
- Evitar crear muchos mocks
- Usar `@ExtendWith(MockitoExtension.class)` en lugar de setup manual

---

**Para más información, ver: TESTS_README.md**

