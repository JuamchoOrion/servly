# 📋 FRONTEND INTEGRATION CONTRACT - Servly Authentication API

**Versión:** 1.0.0  
**Fecha:** Marzo 5, 2026  
**Estado:** ✅ Ready for Production

---

## 📌 Resumen Ejecutivo

Este documento define el **contrato exacto** entre el Backend (Spring Boot) y el Frontend (Angular/React) para el sistema de autenticación. Todos los DTOs, endpoints, códigos HTTP y formatos JSON están especificados aquí.

**Protocolo:** HTTPS (HTTP en desarrollo)  
**Content-Type:** application/json  
**Encoding:** UTF-8  

---

## 🔐 ENDPOINT: POST /api/auth/login

### Descripción
Autentica un usuario con email, contraseña y token de reCAPTCHA v2. Retorna JWT para acceso posterior a endpoints protegidos.

### HTTP Method
```
POST
```

### URL
```
http://localhost:8081/api/auth/login
# Producción:
https://api.servly.com/api/auth/login
```

### Headers Requeridos
```http
Content-Type: application/json
Accept: application/json
```

---

## 📤 REQUEST BODY

### Estructura JSON
```json
{
  "email": "usuario@example.com",
  "password": "MiPassword123!",
  "recaptchaToken": "03AOLTBLTVEy5C3...string_muy_largo_de_google...XyZ"
}
```

### Campo: `email` (String)
- **Tipo:** String
- **Requerido:** Sí ✅
- **Validación:**
  - No puede estar vacío
  - Debe ser un email válido (RFC 5322)
  - Máximo: 255 caracteres
- **Ejemplo:** `admin@servly.com`
- **Error si falta:** 
  ```json
  {
    "status": 422,
    "message": "El email es obligatorio",
    "errorType": "VALIDATION_ERROR"
  }
  ```

### Campo: `password` (String)
- **Tipo:** String
- **Requerido:** Sí ✅
- **Validación:**
  - No puede estar vacío
  - Mínimo: 8 caracteres
  - Máximo: 255 caracteres
- **Ejemplo:** `SecurePassword123!`
- **Error si falta:**
  ```json
  {
    "status": 422,
    "message": "La contraseña es obligatoria",
    "errorType": "VALIDATION_ERROR"
  }
  ```

### Campo: `recaptchaToken` (String)
- **Tipo:** String
- **Requerido:** Sí ✅ (cuando `app.recaptcha.enabled=true`)
- **Cómo obtenerlo:**
  1. El widget de reCAPTCHA debe estar en tu HTML
  2. Cuando el usuario completa el captcha: `grecaptcha.getResponse()`
  3. Google retorna un token largo (~500 caracteres)
- **Validación:**
  - No puede estar vacío
  - Será validado con Google
- **Ejemplo:** `03AOLTBLTVEy5C3m-o...` (truncado por brevedad)
- **Notas:**
  - Expira después de 2 minutos
  - Usa la clave del sitio de reCAPTCHA v2
  - En desarrollo puedes usar `app.recaptcha.enabled=false` para saltarlo

---

## 📥 SUCCESS RESPONSE (200 OK)

### Respuesta Exitosa
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQxNTMyMDB9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQ2NzIwMDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "email": "usuario@example.com",
  "name": "Juan Pérez García",
  "roles": ["STAFF", "ADMIN"],
  "mustChangePassword": false,
  "message": null
}
```

### Campos de Respuesta

#### `token` (String - JWT)
- **Descripción:** JWT (JSON Web Token) para autenticación
- **Duración:** 24 horas
- **Uso:** Incluir en header `Authorization: Bearer <token>` para todos los requests posteriores
- **Estructura:**
  - Header: `{"alg":"HS256","typ":"JWT"}`
  - Payload: `{"sub":"email","iat":timestamp,"exp":expiration,"roles":[...]}`
  - Signature: HMAC-SHA256
- **Ejemplo decodificado:** (base64)
  ```
  Header: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
  Payload: eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzA0MDY3MjAwLCJleXAiOjE3MDQxNTMyMDB9
  Signature: TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ
  ```

#### `refreshToken` (String - JWT)
- **Descripción:** Token para renovar el `token` cuando expire
- **Duración:** 7 días
- **Uso:** Guardar de forma segura (localStorage con cuidado, mejor sessionStorage)
- **Cuándo usarlo:**
  ```
  Si GET /api/staff/tables retorna 401 Unauthorized:
  1. POST /api/auth/refresh-token con { refreshToken }
  2. Recibir nuevo token
  3. Reintentar GET /api/staff/tables
  ```

#### `email` (String)
- **Descripción:** Email del usuario autenticado
- **Ejemplo:** `admin@servly.com`
- **Uso:** Mostrar al usuario, o para requests posteriores

#### `name` (String)
- **Descripción:** Nombre completo del usuario
- **Ejemplo:** `Juan Pérez García`
- **Uso:** Mostrar en interfaz (ej: "Bienvenido, Juan")

#### `roles` (Array de Strings)
- **Descripción:** Array de roles del usuario
- **Valores posibles:** `ADMIN`, `MANAGER`, `STAFF`, `CUSTOMER`
- **Ejemplo:** `["STAFF", "ADMIN"]`
- **Uso:**
  - Controlar qué se muestra en UI
  - Proteger rutas locales en Angular (AuthGuard)
  - Determinar a dónde redirigir después del login

#### `mustChangePassword` (Boolean)
- **Descripción:** Si es true, usuario debe cambiar contraseña antes de acceder
- **Valores:**
  - `true`: Redirigir a formulario de cambio de contraseña
  - `false`: Acceso normal
- **Valor típico:** `false` (excepto primer login)
- **Qué hacer si true:**
  ```typescript
  if (response.mustChangePassword) {
    // Mostrar modal de cambio de contraseña
    // Usuario no puede acceder a funcionalidades
    this.router.navigate(['/auth/change-password']);
  }
  ```

#### `message` (String, nullable)
- **Descripción:** Mensaje adicional para el usuario
- **Valores típicos:** `null` (vacío en login exitoso)
- **Casos especiales:**
  - Primer login: puede incluir mensaje sobre cambio de contraseña
  - Login 2FA: puede ser `"Verificación en dos pasos requerida"`
- **Uso:** Mostrar en notificación al usuario

---

## ❌ ERROR RESPONSES

### Error 400 - Bad Request (reCAPTCHA Inválido)

**Causa:** El token de reCAPTCHA es inválido, expirado o no fue proporcionado

**Response:**
```json
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "status": 400,
  "message": "Validación de reCAPTCHA fallida. Por favor, intenta nuevamente.",
  "errorType": "RECAPTCHA_FAILED",
  "timestamp": "2026-03-05T10:30:45.123456",
  "path": "/api/auth/login"
}
```

**Qué hacer en Frontend:**
```typescript
if (error.status === 400 && error.error.errorType === 'RECAPTCHA_FAILED') {
  // El reCAPTCHA falló
  // Resetear el widget: grecaptcha.reset()
  // Mostrar mensaje: "Por favor completa el reCAPTCHA nuevamente"
  this.resetCaptcha();
  this.showErrorMessage('Verificación de reCAPTCHA fallida. Intenta de nuevo.');
}
```

---

### Error 401 - Unauthorized (Credenciales Inválidas)

**Causa:** Email o contraseña son incorrectos

**Response:**
```json
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "status": 401,
  "message": "Email o contraseña incorrectos",
  "errorType": "INVALID_CREDENTIALS",
  "timestamp": "2026-03-05T10:30:45.123456",
  "path": "/api/auth/login"
}
```

**Qué hacer en Frontend:**
```typescript
if (error.status === 401) {
  // Credenciales inválidas
  // Limpiar inputs
  this.password = '';
  // Mostrar error
  this.showErrorMessage('Email o contraseña incorrectos');
  // Resetear captcha para nuevo intento
  grecaptcha.reset();
}
```

---

### Error 403 - Forbidden (Usuario Deshabilitado)

**Causa:** El usuario existe pero su cuenta está deshabilitada

**Response:**
```json
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "status": 403,
  "message": "Tu cuenta ha sido deshabilitada. Contacta al administrador.",
  "errorType": "ACCOUNT_DISABLED",
  "timestamp": "2026-03-05T10:30:45.123456",
  "path": "/api/auth/login"
}
```

**Qué hacer en Frontend:**
```typescript
if (error.status === 403) {
  this.showErrorMessage(
    'Tu cuenta ha sido deshabilitada. ' +
    'Por favor contacta al administrador de Servly.'
  );
  // No permitir reintentos
  this.loginForm.disable();
}
```

---

### Error 422 - Unprocessable Entity (Validación Fallida)

**Causa:** Campos obligatorios vacíos o formato inválido

**Response:**
```json
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/json

{
  "status": 422,
  "message": "Validación fallida",
  "errorType": "VALIDATION_ERROR",
  "details": {
    "email": "El email es obligatorio",
    "password": "La contraseña debe tener al menos 8 caracteres"
  },
  "timestamp": "2026-03-05T10:30:45.123456",
  "path": "/api/auth/login"
}
```

**Qué hacer en Frontend:**
```typescript
if (error.status === 422) {
  // Errores de validación múltiples
  const details = error.error.details; // {email: "...", password: "..."}
  Object.keys(details).forEach(field => {
    this.showFieldError(field, details[field]);
  });
}
```

---

### Error 429 - Too Many Requests (Rate Limiting)

**Causa:** Demasiados intentos fallidos de login

**Response:**
```json
HTTP/1.1 429 Too Many Requests
Content-Type: application/json

{
  "status": 429,
  "message": "Demasiados intentos de login. Intenta más tarde.",
  "errorType": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2026-03-05T10:30:45.123456",
  "path": "/api/auth/login"
}
```

**Qué hacer en Frontend:**
```typescript
if (error.status === 429) {
  this.loginDisabled = true;
  this.showErrorMessage(
    'Demasiados intentos fallidos. ' +
    'Por favor intenta en 15 minutos.'
  );
  // Deshabilitar form por 15 minutos
}
```

---

### Error 500 - Internal Server Error

**Causa:** Error inesperado en el servidor

**Response:**
```json
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "status": 500,
  "message": "Error interno del servidor. Por favor contacta al soporte.",
  "errorType": "INTERNAL_ERROR",
  "timestamp": "2026-03-05T10:30:45.123456",
  "path": "/api/auth/login"
}
```

**Qué hacer en Frontend:**
```typescript
if (error.status === 500) {
  this.showErrorMessage(
    'Error del servidor. Por favor intenta más tarde.'
  );
  // Loguear el error para debugging
  console.error('Server error:', error);
}
```

---

## 🔄 Tabla de Códigos HTTP

| Código | Situación | Action |
|--------|-----------|--------|
| **200** | Login exitoso | Guardar tokens, redirigir a dashboard |
| **202** | 2FA requerido | Redirigir a verificación de código |
| **400** | reCAPTCHA inválido | Resetear captcha, reintentar |
| **401** | Credenciales inválidas | Mostrar error, limpiar password |
| **403** | Cuenta deshabilitada | Mostrar error, desabilitar formulario |
| **422** | Validación fallida | Mostrar errores de campo |
| **429** | Rate limit | Deshabilitar intento por 15 min |
| **500** | Error del servidor | Contactar soporte |

---

## 🌐 CORS Configuration

### Orígenes Permitidos

**Desarrollo:**
```
http://localhost:4200
http://localhost:3000
http://127.0.0.1:4200
```

**Staging:**
```
https://staging-app.servly.com
```

**Producción:**
```
https://app.servly.com
https://www.servly.com
```

### Headers Permitidos

**Request Headers:**
```
Content-Type
Authorization
Accept
X-Requested-With
```

**Response Headers:**
```
Access-Control-Allow-Origin
Access-Control-Allow-Methods
Access-Control-Allow-Headers
Access-Control-Allow-Credentials
X-2FA-Required
```

### Configuración en Backend

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "http://localhost:4200",
                        "http://localhost:3000",
                        "https://app.servly.com"
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

---

## 🔐 Headers de Autorización

### Para Requests Posteriores

Todos los endpoints protegidos requieren este header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Formato:**
- `Bearer ` (con espacio)
- JWT token completo

**Ejemplo:**
```typescript
const headers = new HttpHeaders({
  'Authorization': `Bearer ${this.accessToken}`
});

this.http.get('/api/staff/tables', { headers })
```

### Angular HttpInterceptor (Recomendado)

```typescript
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = this.authService.getAccessToken();
    
    if (token) {
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

---

## 💾 Almacenamiento de Tokens

### localStorage vs sessionStorage

**Recomendación:** sessionStorage (más seguro)

```typescript
// ✅ RECOMENDADO: sessionStorage (se limpia al cerrar el navegador)
sessionStorage.setItem('accessToken', response.token);
sessionStorage.setItem('refreshToken', response.refreshToken);

// ⚠️ USAR CON CUIDADO: localStorage (persiste en disco)
// Solo si la app es una PWA o necesita persistencia real
localStorage.setItem('accessToken', response.token);
```

### Estructura Recomendada

```typescript
// auth.service.ts
export class AuthService {
  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_KEY = 'refresh_token';
  private readonly USER_KEY = 'current_user';

  setTokens(token: string, refreshToken: string) {
    sessionStorage.setItem(this.TOKEN_KEY, token);
    sessionStorage.setItem(this.REFRESH_KEY, refreshToken);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(this.REFRESH_KEY);
  }

  clearTokens() {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_KEY);
    sessionStorage.removeItem(this.USER_KEY);
  }
}
```

---

## 🔄 Renovar Token

Cuando el JWT expire (después de 24 horas), usa el refresh token:

### Request
```http
POST /api/auth/refresh-token HTTP/1.1
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Response (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "usuario@example.com",
  "name": "Juan Pérez",
  "roles": ["STAFF"],
  "mustChangePassword": false
}
```

### Angular HttpInterceptor (Manejo de 401)

```typescript
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = this.authService.getAccessToken();
    
    if (token) {
      request = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token expirado, intentar renovar
          const refreshToken = this.authService.getRefreshToken();
          
          if (refreshToken) {
            return this.authService.refreshToken(refreshToken).pipe(
              switchMap((response: LoginResponse) => {
                // Guardar nuevo token
                this.authService.setTokens(response.token, response.refreshToken);
                
                // Reintentar request original con nuevo token
                request = request.clone({
                  setHeaders: { Authorization: `Bearer ${response.token}` }
                });
                
                return next.handle(request);
              }),
              catchError(() => {
                // Refresh falló, logout y redirigir a login
                this.authService.logout();
                this.router.navigate(['/login']);
                return throwError(() => error);
              })
            );
          }
        }

        return throwError(() => error);
      })
    );
  }
}
```

---

## 📱 Flujo de Login Completo en Angular

### 1. Formulario HTML

```html
<form [formGroup]="loginForm" (ngSubmit)="onLogin()">
  <!-- Email -->
  <input
    type="email"
    formControlName="email"
    placeholder="Email"
    required>
  <div *ngIf="emailField.invalid && emailField.touched" class="error">
    {{ emailField.errors?.['email'] ? 'Email inválido' : 'Email requerido' }}
  </div>

  <!-- Contraseña -->
  <input
    type="password"
    formControlName="password"
    placeholder="Contraseña"
    required>
  <div *ngIf="passwordField.invalid && passwordField.touched" class="error">
    Contraseña requerida
  </div>

  <!-- reCAPTCHA v2 -->
  <div class="g-recaptcha" 
       #recaptcha
       data-sitekey="YOUR_SITE_KEY"
       data-callback="onRecaptchaSuccess"
       data-expired-callback="onRecaptchaExpired">
  </div>

  <!-- Submit -->
  <button 
    type="submit" 
    [disabled]="isLoading || !loginForm.valid || !recaptchaValid">
    Iniciar Sesión
  </button>
</form>
```

### 2. Component TypeScript

```typescript
import { Component, OnInit, ViewChild, NgZone } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

declare var grecaptcha: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  @ViewChild('recaptcha') recaptchaElement: any;

  loginForm: FormGroup;
  isLoading = false;
  recaptchaValid = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  ngOnInit() {
    this.loadRecaptcha();
  }

  loadRecaptcha() {
    const script = document.createElement('script');
    script.src = 'https://www.google.com/recaptcha/api.js';
    script.async = true;
    script.defer = true;
    document.body.appendChild(script);
  }

  onRecaptchaSuccess(token: string) {
    this.recaptchaValid = true;
  }

  onRecaptchaExpired() {
    this.recaptchaValid = false;
  }

  onLogin() {
    if (!this.loginForm.valid) {
      this.errorMessage = 'Por favor completa todos los campos';
      return;
    }

    if (!this.recaptchaValid) {
      this.errorMessage = 'Por favor completa el reCAPTCHA';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const recaptchaToken = grecaptcha.getResponse();
    const loginRequest = {
      ...this.loginForm.value,
      recaptchaToken
    };

    this.authService.login(loginRequest).subscribe({
      next: (response: LoginResponse) => {
        // Guardar tokens
        this.authService.setTokens(response.token, response.refreshToken);

        // Guardar información del usuario
        this.authService.setCurrentUser(response);

        // Redirigir según rol
        this.ngZone.run(() => {
          if (response.roles.includes('ADMIN')) {
            this.router.navigate(['/admin/dashboard']);
          } else if (response.roles.includes('MANAGER')) {
            this.router.navigate(['/staff/dashboard']);
          } else {
            this.router.navigate(['/staff/dashboard']);
          }
        });
      },
      error: (error) => {
        this.isLoading = false;
        grecaptcha.reset();
        
        if (error.status === 400) {
          this.errorMessage = 'Verificación de reCAPTCHA fallida. Intenta de nuevo.';
        } else if (error.status === 401) {
          this.errorMessage = 'Email o contraseña incorrectos';
          this.loginForm.get('password')?.reset();
        } else if (error.status === 403) {
          this.errorMessage = 'Tu cuenta ha sido deshabilitada';
        } else if (error.status === 422) {
          this.errorMessage = 'Verifica los datos ingresados';
        } else {
          this.errorMessage = 'Error al iniciar sesión. Intenta más tarde.';
        }
      }
    });
  }

  get emailField() {
    return this.loginForm.get('email')!;
  }

  get passwordField() {
    return this.loginForm.get('password')!;
  }
}
```

### 3. AuthService TypeScript

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface LoginRequest {
  email: string;
  password: string;
  recaptchaToken: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  email: string;
  name: string;
  roles: string[];
  mustChangePassword: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';
  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, request)
      .pipe(
        tap(response => {
          console.log('Login successful for:', response.email);
        })
      );
  }

  setTokens(token: string, refreshToken: string) {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem('access_token');
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem('refresh_token');
  }

  setCurrentUser(user: LoginResponse) {
    this.currentUserSubject.next(user);
    sessionStorage.setItem('current_user', JSON.stringify(user));
  }

  getCurrentUser(): LoginResponse | null {
    const user = sessionStorage.getItem('current_user');
    return user ? JSON.parse(user) : null;
  }

  refreshToken(refreshToken: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/refresh-token`,
      { refreshToken }
    );
  }

  logout() {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('current_user');
    this.currentUserSubject.next(null);
  }
}
```

---

## 📋 Checklist de Implementación

### Backend (Ya implementado ✅)
- [x] LoginRequest DTO
- [x] LoginResponse DTO
- [x] ErrorResponse DTO
- [x] POST /api/auth/login endpoint
- [x] RecaptchaService
- [x] JWT generation
- [x] CORS configuration

### Frontend (Para que implementes)
- [ ] Formulario de login HTML
- [ ] LoginComponent Angular
- [ ] AuthService
- [ ] JwtInterceptor
- [ ] AuthGuard para proteger rutas
- [ ] reCAPTCHA v2 widget
- [ ] Manejo de errores
- [ ] Almacenamiento de tokens
- [ ] Logout functionality
- [ ] Renovación de token

---

## 🚀 Quick Start Frontend

```bash
# 1. Instalar dependencias
npm install @angular/common @angular/forms ngx-recaptcha2

# 2. Copiar AuthService
# Usar el código de EJEMPLO_ANGULAR_SERVICES.ts

# 3. Importar en AppModule
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { JwtInterceptor } from './auth/jwt.interceptor';

@NgModule({
  imports: [HttpClientModule],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ]
})
export class AppModule { }

# 4. Implementar componentes
# Usar el HTML y TypeScript de arriba

# 5. Probar en navegador
# http://localhost:4200/login
```

---

## 📞 Soporte

Si encuentras problemas:

1. **Verifica el status code** (200, 401, 400, etc.)
2. **Mira el errorType** en la respuesta
3. **Revisa los logs del navegador** (F12 → Console)
4. **Verifica que CORS está permitido** (Network tab)
5. **Contacta al equipo backend** con el error exacto

---

## ✅ Completamente Implementado

Este contrato garantiza que:
- ✅ El frontend sabe exactamente qué enviar
- ✅ El backend sabe exactamente qué retornar
- ✅ Los errores se manejan consistentemente
- ✅ La integración es plug-and-play
- ✅ No hay sorpresas en producción

**Versión:** 1.0.0  
**Última actualización:** Marzo 5, 2026  
**Estado:** Aprobado para producción ✅

