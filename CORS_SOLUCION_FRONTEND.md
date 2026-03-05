# 🔧 SOLUCIÓN CORS - Frontend http://localhost:4200 ↔ Backend http://localhost:8081

## ✅ Backend Configurado

Se han realizado los siguientes cambios en el backend Spring Boot:

### 1. **CorsConfig.java** (Nuevo archivo)
Ubicación: `src/main/java/co/edu/uniquindio/servly/config/CorsConfig.java`

Este archivo configura CORS para permitir peticiones desde `http://localhost:4200`:

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Orígenes permitidos
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",      // ✅ TU FRONTEND
            "http://localhost:3000",
            "http://127.0.0.1:4200"
        ));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        
        // Permitir credenciales
        configuration.setAllowCredentials(true);
        
        // Caché de preflight requests (1 hora)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
```

### 2. **SecurityConfig.java** (Actualizado)
Se agregó CORS al inicio del SecurityFilterChain:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        // ✅ CORS HABILITADO - PRIMERO EN LA CADENA
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        
        .csrf(AbstractHttpConfigurer::disable)
        // ... resto de la configuración
}
```

---

## ✅ Frontend - Lo que necesitas hacer

### 1. **HttpClientModule** (Verificar que está importado)

En tu `app.module.ts` o `app.config.ts`:

```typescript
import { HttpClientModule } from '@angular/common/http';

// En App Module
@NgModule({
  imports: [
    HttpClientModule,
    // ... otros imports
  ]
})
export class AppModule { }

// O en App Config (Angular 14+)
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient()
  ]
};
```

### 2. **AuthService** (Actualizado para CORS)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) { }

  login(email: string, password: string, recaptchaToken: string): Observable<any> {
    const loginRequest = {
      email,
      password,
      recaptchaToken
    };

    // ✅ NO NECESITAS withCredentials si no usas cookies
    // ✅ NO NECESITAS setHeaders customizados (Angular lo hace automático)
    
    return this.http.post<any>(
      `${this.apiUrl}/login`,
      loginRequest
      // Eso es todo - HttpClient maneja Content-Type automáticamente
    );
  }

  setTokens(token: string, refreshToken: string) {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem('access_token');
  }

  logout() {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
  }
}
```

### 3. **JwtInterceptor** (Agregar token automáticamente)

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  
  constructor(private authService: AuthService) { }

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    
    const token = this.authService.getAccessToken();
    
    if (token) {
      // ✅ Clonar request y agregar Authorization header
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request);
  }
}
```

Registra en `app.module.ts`:

```typescript
import { HTTP_INTERCEPTORS } from '@angular/common/http';

@NgModule({
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ]
})
export class AppModule { }
```

### 4. **LoginComponent** (Ejemplo completo)

```typescript
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

declare var grecaptcha: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  
  email = '';
  password = '';
  isLoading = false;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.loadRecaptcha();
  }

  loadRecaptcha() {
    const script = document.createElement('script');
    script.src = 'https://www.google.com/recaptcha/api.js';
    script.async = true;
    script.defer = true;
    document.body.appendChild(script);
  }

  onLogin() {
    this.isLoading = true;
    this.errorMessage = '';

    const recaptchaToken = grecaptcha.getResponse();

    if (!recaptchaToken) {
      this.errorMessage = 'Por favor completa el reCAPTCHA';
      this.isLoading = false;
      return;
    }

    // ✅ Llamada al backend
    this.authService.login(this.email, this.password, recaptchaToken)
      .subscribe({
        next: (response) => {
          console.log('✅ Login exitoso');
          
          // Guardar tokens
          this.authService.setTokens(response.token, response.refreshToken);

          // Redirigir según rol
          if (response.roles.includes('ADMIN')) {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/dashboard']);
          }
        },
        error: (error) => {
          this.isLoading = false;
          grecaptcha.reset();

          // Manejo de errores
          if (error.status === 0) {
            this.errorMessage = '❌ Error de CORS - Verifica que el backend esté corriendo';
            console.error('CORS ERROR:', error);
          } else if (error.status === 401) {
            this.errorMessage = 'Email o contraseña incorrectos';
          } else if (error.status === 400) {
            this.errorMessage = 'reCAPTCHA inválido - Intenta de nuevo';
          } else if (error.status === 403) {
            this.errorMessage = 'Tu cuenta ha sido deshabilitada';
          } else {
            this.errorMessage = `Error: ${error.error?.message || 'Error desconocido'}`;
          }
          
          console.error('Login error:', error);
        }
      });
  }
}
```

---

## 🧪 Testing - Verificar CORS

### 1. **Con cURL** (Verifica que CORS está habilitado)

```bash
curl -i -X OPTIONS http://localhost:8081/api/auth/login \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type"
```

**Respuesta esperada:**
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
Access-Control-Allow-Headers: Content-Type,Authorization,X-Requested-With,Accept,Origin
Access-Control-Max-Age: 3600
Access-Control-Allow-Credentials: true
```

### 2. **Con Postman**

1. Abre Postman
2. Click en "Headers"
3. Agrega:
   ```
   Origin: http://localhost:4200
   ```
4. Realiza request POST a `http://localhost:8081/api/auth/login`
5. Verifica que en respuesta ves `Access-Control-Allow-Origin: http://localhost:4200`

### 3. **En el navegador** (F12 → Network)

1. Abre tu frontend en `http://localhost:4200`
2. Intenta hacer login
3. En DevTools → Network tab
4. Busca la petición POST `/api/auth/login`
5. Verifica que no hay error de CORS
6. En la pestaña "Response Headers" deberías ver:
   ```
   Access-Control-Allow-Origin: http://localhost:4200
   ```

---

## 🔍 Si Sigue Dando Error CORS

### 1. **Verifica que el servidor está corriendo**

```bash
netstat -an | findstr 8081
```

Si no aparece, el servidor no está corriendo.

### 2. **Verifica la compilación**

```bash
cd "C:\Users\ramir\Documents\7mo Semestre\Ing de software III\servly"
./gradlew.bat clean build -x test
```

Si aparece `BUILD FAILED`, hay un error en el código.

### 3. **Verifica CorsConfig está siendo usado**

En `SecurityConfig.java` debe estar:

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource))
```

No simplemente `.cors()` sin parámetros.

### 4. **Reinicia ambos servidores**

- Mata el proceso de Spring Boot (puerto 8081)
- Mata el proceso de Angular (puerto 4200)
- Reinicia ambos

### 5. **Verifica que HttpClientModule está importado**

En `app.module.ts`:

```typescript
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  imports: [HttpClientModule]
})
```

---

## 📋 Checklist Final

- [ ] Archivo `CorsConfig.java` creado en `config/`
- [ ] `SecurityConfig.java` actualizado con `.cors(cors -> cors.configurationSource(corsConfigurationSource))`
- [ ] Backend compilado sin errores (`BUILD SUCCESSFUL`)
- [ ] Backend corriendo en `http://localhost:8081`
- [ ] `HttpClientModule` importado en Angular
- [ ] `JwtInterceptor` configurado
- [ ] Frontend corriendo en `http://localhost:4200`
- [ ] Request de login se envía sin error CORS
- [ ] Response includes `Access-Control-Allow-Origin: http://localhost:4200`

---

## ✅ Si todo está bien

Cuando hagas login desde `http://localhost:4200`, deberías:

1. ✅ Ver la petición en Network sin error de CORS
2. ✅ Recibir respuesta con `token` y `refreshToken`
3. ✅ Tokens guardados en `sessionStorage`
4. ✅ Redirigido a dashboard

---

**Status:** ✅ CORS Configurado  
**Frontend:** http://localhost:4200  
**Backend:** http://localhost:8081  
**Archivo Config:** `src/main/java/co/edu/uniquindio/servly/config/CorsConfig.java`

