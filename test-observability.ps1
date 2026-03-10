# Servly Observability - Quick Test Script (PowerShell)
# This script validates that metrics are being recorded correctly

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Servly Observability - Quick Validation" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$SERVLY_URL = "http://localhost:8081"
$PROMETHEUS_URL = "http://localhost:9090"
$GRAFANA_URL = "http://localhost:3000"
$TIMEOUT = 5

# Test counters
$TESTS_PASSED = 0
$TESTS_FAILED = 0

# Function to test endpoint
function Test-Endpoint {
    param(
        [string]$Url,
        [string]$Name
    )

    Write-Host "Testing $Name... " -NoNewline

    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec $TIMEOUT -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ PASS" -ForegroundColor Green
            $script:TESTS_PASSED++
        }
    } catch {
        Write-Host "✗ FAIL" -ForegroundColor Red
        $script:TESTS_FAILED++
    }
}

# Function to test Prometheus metric
function Test-Metric {
    param(
        [string]$Metric,
        [string]$Name
    )

    Write-Host "Checking metric '$Name'... " -NoNewline

    try {
        $response = Invoke-WebRequest -Uri "$PROMETHEUS_URL/api/v1/query?query=$Metric" -TimeoutSec $TIMEOUT -ErrorAction SilentlyContinue
        if ($response.Content -like "*success*") {
            Write-Host "✓ Found" -ForegroundColor Green
            $script:TESTS_PASSED++
        } else {
            Write-Host "⚠ Not yet available (may need more traffic)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠ Endpoint not ready" -ForegroundColor Yellow
    }
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "1. Testing Service Availability" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Test-Endpoint "$SERVLY_URL/actuator/health" "Servly Health Check"
Test-Endpoint "$SERVLY_URL/actuator/prometheus" "Servly Prometheus Endpoint"
Test-Endpoint "$PROMETHEUS_URL" "Prometheus UI"
Test-Endpoint "$GRAFANA_URL" "Grafana UI"

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "2. Testing Prometheus Connectivity" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Host "Checking Prometheus targets... " -NoNewline
try {
    $response = Invoke-WebRequest -Uri "$PROMETHEUS_URL/api/v1/targets?state=active" -TimeoutSec $TIMEOUT -ErrorAction SilentlyContinue
    if ($response.Content -like "*servly-app*") {
        Write-Host "✓ servly-app is being scraped" -ForegroundColor Green
        $TESTS_PASSED++
    } else {
        Write-Host "⚠ Target not yet scraped (wait for first scrape)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Prometheus not ready" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "3. Checking Available Metrics" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Check for auth metrics
Write-Host "Auth metrics present... " -NoNewline
try {
    $response = Invoke-WebRequest -Uri "$SERVLY_URL/actuator/metrics" -TimeoutSec $TIMEOUT -ErrorAction SilentlyContinue
    $count = ([regex]::Matches($response.Content, "auth\.")).Count
    if ($count -gt 0) {
        Write-Host "✓ Found ($count)" -ForegroundColor Green
        $TESTS_PASSED++
    } else {
        Write-Host "⚠ Not yet registered (need auth activity)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Cannot access metrics endpoint" -ForegroundColor Yellow
}

# Check for inventory metrics
Write-Host "Inventory metrics present... " -NoNewline
try {
    $response = Invoke-WebRequest -Uri "$SERVLY_URL/actuator/metrics" -TimeoutSec $TIMEOUT -ErrorAction SilentlyContinue
    $count = ([regex]::Matches($response.Content, "inventory\.")).Count
    if ($count -gt 0) {
        Write-Host "✓ Found ($count)" -ForegroundColor Green
        $TESTS_PASSED++
    } else {
        Write-Host "⚠ Not yet registered (need inventory activity)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Cannot access metrics endpoint" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "4. Generated Prometheus Queries" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Host "Try these queries in Prometheus ($PROMETHEUS_URL):" -ForegroundColor Yellow
Write-Host ""

Write-Host "AUTHENTICATION METRICS:" -ForegroundColor Green
Write-Host "  - auth_login_total"
Write-Host "  - auth_login_success_total"
Write-Host "  - (auth_login_success_total / auth_login_total) * 100"
Write-Host "  - histogram_quantile(0.95, rate(auth_login_duration_seconds_bucket[5m])) * 1000"
Write-Host ""

Write-Host "INVENTORY METRICS:" -ForegroundColor Green
Write-Host "  - inventory_item_creation_total"
Write-Host "  - inventory_item_creation_success_total"
Write-Host "  - inventory_category_creation_total"
Write-Host "  - inventory_category_creation_success_total"
Write-Host "  - (inventory_category_creation_success_total / inventory_category_creation_total) * 100"
Write-Host ""

Write-Host "PAGINATED QUERIES:" -ForegroundColor Green
Write-Host "  - histogram_quantile(0.95, rate(inventory_items_paginated_query_duration_seconds_bucket[5m])) * 1000"
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "5. Dashboard Access" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Host "Grafana Dashboard:" -ForegroundColor Green
Write-Host "  URL:  $GRAFANA_URL"
Write-Host "  User: admin"
Write-Host "  Pass: admin"
Write-Host ""
Write-Host "  Dashboard: 'Servly - Observability Dashboard (ISO/IEC 25010)'"
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "Passed: " -NoNewline
Write-Host "$TESTS_PASSED" -ForegroundColor Green
Write-Host "Failed: " -NoNewline
Write-Host "$TESTS_FAILED" -ForegroundColor Red
Write-Host ""

if ($TESTS_FAILED -eq 0 -and $TESTS_PASSED -gt 0) {
    Write-Host "✓ All checks passed!" -ForegroundColor Green
} else {
    Write-Host "⚠ Some services may still be starting up. Wait a few seconds and try again." -ForegroundColor Yellow
}

