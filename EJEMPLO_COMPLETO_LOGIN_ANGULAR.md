# 🚀 EJEMPLO COMPLETO - Login con CORS funcionando

## Angular (TypeScript + HTML)

### 1. app.module.ts

```typescript
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { JwtInterceptor } from './auth/jwt.interceptor';
import { AuthService } from './auth/auth.service';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,        // ✅ IMPORTANTE
    ReactiveFormsModule,
    FormsModule
  ],
  providers: [
    AuthService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,  // ✅ Agregar token automáticamente
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

### 2. auth.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

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

  // ✅ URL DEL BACKEND
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) { }

  /**
   * Login - Envía credenciales + token de reCAPTCHA
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    console.log('Enviando login request a:', `${this.apiUrl}/login`);
    
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      request
      // ✅ NO NECESITAS withCredentials
      // ✅ NO NECESITAS setHeaders (Angular lo maneja)
      // ✅ HttpClient automáticamente establece Content-Type: application/json
    ).pipe(
      tap(response => {
        console.log('✅ Login exitoso para:', response.email);
        this.setTokens(response.token, response.refreshToken);
      }),
      catchError(error => {
        console.error('❌ Error en login:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Guardar tokens en sessionStorage
   */
  setTokens(token: string, refreshToken: string): void {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
    console.log('Tokens guardados en sessionStorage');
  }

  /**
   * Obtener access token
   */
  getAccessToken(): string | null {
    return sessionStorage.getItem('access_token');
  }

  /**
   * Obtener refresh token
   */
  getRefreshToken(): string | null {
    return sessionStorage.getItem('refresh_token');
  }

  /**
   * Logout - Limpiar tokens
   */
  logout(): void {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    console.log('Tokens eliminados - Logout exitoso');
  }

  /**
   * Verificar si está autenticado
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }
}
```

### 3. jwt.interceptor.ts

```typescript
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {

    // ✅ OBTENER TOKEN
    const token = this.authService.getAccessToken();

    // ✅ SI EXISTE TOKEN, AGREGARLO AL HEADER
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('Token agregado al request:', request.url);
    }

    // ✅ MANEJAR RESPUESTA
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        
        // ✅ SI ES 401, EL TOKEN EXPIRÓ
        if (error.status === 401) {
          console.warn('❌ Token expirado (401) - Redirigiendo a login');
          this.authService.logout();
          this.router.navigate(['/login']);
        }

        // ✅ SI ES 403, SIN PERMISOS
        if (error.status === 403) {
          console.warn('❌ Sin permisos (403)');
          this.router.navigate(['/access-denied']);
        }

        return throwError(() => error);
      })
    );
  }
}
```

### 4. login.component.ts

```typescript
import { Component, OnInit, ViewChild, NgZone } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginRequest, LoginResponse } from '../auth/auth.service';

declare var grecaptcha: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  loginForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  recaptchaVerified = false;
  reCaptchaSiteKey = '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI'; // Demo key

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone
  ) {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  ngOnInit(): void {
    // Si ya está autenticado, redirigir
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }

    // Cargar script de reCAPTCHA
    this.loadRecaptcha();
  }

  /**
   * Cargar script de reCAPTCHA dinámicamente
   */
  loadRecaptcha(): void {
    const script = document.createElement('script');
    script.src = 'https://www.google.com/recaptcha/api.js';
    script.async = true;
    script.defer = true;
    document.body.appendChild(script);
    console.log('Script de reCAPTCHA cargado');
  }

  /**
   * Callback cuando reCAPTCHA se verifica exitosamente
   */
  onRecaptchaSuccess(token: string): void {
    this.recaptchaVerified = true;
    console.log('✅ reCAPTCHA verificado');
  }

  /**
   * Callback cuando reCAPTCHA expira
   */
  onRecaptchaExpired(): void {
    this.recaptchaVerified = false;
    console.log('❌ reCAPTCHA expirado');
  }

  /**
   * Manejo del submit del formulario
   */
  onSubmit(): void {
    this.errorMessage = '';

    // ✅ VALIDAR FORMULARIO
    if (this.loginForm.invalid) {
      this.errorMessage = 'Por favor completa todos los campos correctamente';
      return;
    }

    // ✅ VALIDAR reCAPTCHA
    const recaptchaToken = grecaptcha.getResponse();
    if (!recaptchaToken) {
      this.errorMessage = 'Por favor completa el reCAPTCHA';
      console.warn('reCAPTCHA no completado');
      return;
    }

    this.isLoading = true;

    // ✅ PREPARAR REQUEST
    const loginRequest: LoginRequest = {
      email: this.loginForm.get('email')?.value,
      password: this.loginForm.get('password')?.value,
      recaptchaToken: recaptchaToken
    };

    console.log('📤 Enviando login request');

    // ✅ LLAMAR AL SERVICIO
    this.authService.login(loginRequest).subscribe({

      next: (response: LoginResponse) => {
        console.log('✅ Login exitoso');
        console.log('Usuario:', response.email);
        console.log('Roles:', response.roles);

        // ✅ REDIRIGIR SEGÚN ROL
        this.ngZone.run(() => {
          if (response.roles.includes('ADMIN')) {
            console.log('Redirigiendo a /admin/dashboard');
            this.router.navigate(['/admin/dashboard']);
          } else {
            console.log('Redirigiendo a /dashboard');
            this.router.navigate(['/dashboard']);
          }
        });
      },

      error: (error: any) => {
        this.isLoading = false;
        grecaptcha.reset();

        console.error('❌ Error en login:', error);
        console.error('Status:', error.status);
        console.error('Message:', error.error?.message);

        // ✅ MANEJAR DIFERENTES ERRORES
        if (error.status === 0) {
          this.errorMessage = '❌ Error de CORS - Verifica que el backend esté corriendo en puerto 8081';
          console.error('CORS ERROR - Backend no responde o CORS no configurado');
        } else if (error.status === 400) {
          this.errorMessage = error.error?.message || 'reCAPTCHA inválido - Intenta de nuevo';
        } else if (error.status === 401) {
          this.errorMessage = 'Email o contraseña incorrectos';
        } else if (error.status === 403) {
          this.errorMessage = 'Tu cuenta ha sido deshabilitada';
        } else if (error.status === 422) {
          this.errorMessage = 'Validación fallida - Revisa los datos';
        } else if (error.status === 500) {
          this.errorMessage = 'Error del servidor - Intenta más tarde';
        } else {
          this.errorMessage = error.error?.message || 'Error desconocido';
        }
      }
    });
  }

  /**
   * Getters para validación en template
   */
  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }
}
```

### 5. login.component.html

```html
<div class="login-container">
  <div class="login-card">
    <h1>🔐 Servly Login</h1>
    
    <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
      
      <!-- Email Input -->
      <div class="form-group">
        <label>Email</label>
        <input
          type="email"
          formControlName="email"
          placeholder="tu@email.com"
          class="form-control"
          [class.is-invalid]="email?.invalid && email?.touched">
        <div *ngIf="email?.invalid && email?.touched" class="error-text">
          <small *ngIf="email?.errors?.['required']">Email es requerido</small>
          <small *ngIf="email?.errors?.['email']">Email inválido</small>
        </div>
      </div>

      <!-- Password Input -->
      <div class="form-group">
        <label>Contraseña</label>
        <input
          type="password"
          formControlName="password"
          placeholder="Tu contraseña"
          class="form-control"
          [class.is-invalid]="password?.invalid && password?.touched">
        <div *ngIf="password?.invalid && password?.touched" class="error-text">
          <small *ngIf="password?.errors?.['required']">Contraseña es requerida</small>
          <small *ngIf="password?.errors?.['minlength']">Mínimo 6 caracteres</small>
        </div>
      </div>

      <!-- reCAPTCHA Widget -->
      <div class="form-group">
        <div 
          class="g-recaptcha"
          [attr.data-sitekey]="reCaptchaSiteKey"
          data-callback="grecaptcha.render('recaptcha', {sitekey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI'})">
        </div>
      </div>

      <!-- Error Message -->
      <div *ngIf="errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <!-- Submit Button -->
      <button
        type="submit"
        class="btn btn-primary"
        [disabled]="isLoading || loginForm.invalid">
        <span *ngIf="!isLoading">Iniciar Sesión</span>
        <span *ngIf="isLoading">
          <i class="spinner"></i> Cargando...
        </span>
      </button>

    </form>

  </div>
</div>
```

### 6. login.component.css

```css
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 400px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-control:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 5px rgba(102, 126, 234, 0.3);
}

.form-control.is-invalid {
  border-color: #dc3545;
}

.error-text {
  color: #dc3545;
  font-size: 12px;
  margin-top: 5px;
}

.alert {
  padding: 12px;
  margin-bottom: 20px;
  border-radius: 5px;
}

.alert-danger {
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  color: #721c24;
}

.btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* reCAPTCHA widget */
.g-recaptcha {
  transform: scale(1);
  transform-origin: 0 0;
}

@media (max-width: 500px) {
  .g-recaptcha {
    transform: scale(0.77);
    transform-origin: 0 0;
  }

  .login-card {
    padding: 20px;
  }
}
```

---

## Testing

### 1. Verificar que funciona

```bash
# Terminal 1: Backend corriendo
java -jar build/libs/servly-0.0.1-SNAPSHOT.jar

# Terminal 2: Frontend corriendo
ng serve --port 4200

# Terminal 3: Navegador
http://localhost:4200/login
```

### 2. Abrir DevTools (F12)

```
Tab "Console":
  Deberías ver logs como:
  ✓ "Script de reCAPTCHA cargado"
  ✓ "📤 Enviando login request"
  ✓ "✅ Login exitoso"

Tab "Network":
  POST /api/auth/login
  Status: 200
  Response Headers:
    Access-Control-Allow-Origin: http://localhost:4200
```

---

## Referencia Rápida

| Qué | Dónde | Valor |
|-----|-------|-------|
| **Backend URL** | auth.service.ts | `http://localhost:8081/api/auth` |
| **Endpoint Login** | auth.service.ts | `POST /api/auth/login` |
| **Headers** | Automático | `Content-Type: application/json` |
| **Token Header** | jwt.interceptor.ts | `Authorization: Bearer {token}` |
| **Storage** | auth.service.ts | `sessionStorage` |
| **Key Token** | auth.service.ts | `access_token` |
| **CORS Origen** | SecurityConfig.java | `http://localhost:4200` |

---

**Status:** ✅ CORS FUNCIONANDO  
**Frontend:** http://localhost:4200  
**Backend:** http://localhost:8081  
**Auth:** JWT + reCAPTCHA

