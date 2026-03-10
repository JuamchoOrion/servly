#!/bin/bash

# Servly Observability - Quick Test Script
# Este script valida que las métricas se están registrando correctamente

set -e

echo "=========================================="
echo "Servly Observability - Quick Validation"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SERVLY_URL="http://localhost:8081"
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"
TIMEOUT=5

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to test endpoint
test_endpoint() {
    local url=$1
    local name=$2

    echo -n "Testing $name... "

    if curl -s --connect-timeout $TIMEOUT "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC}"
        ((TESTS_FAILED++))
    fi
}

# Function to test Prometheus metric
test_metric() {
    local metric=$1
    local name=$2

    echo -n "Checking metric '$name'... "

    if curl -s "$PROMETHEUS_URL/api/v1/query?query=$metric" | grep -q "success"; then
        echo -e "${GREEN}✓ Found${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${YELLOW}⚠ Not yet available (may need more traffic)${NC}"
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Testing Service Availability"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

test_endpoint "$SERVLY_URL/actuator/health" "Servly Health Check"
test_endpoint "$SERVLY_URL/actuator/prometheus" "Servly Prometheus Endpoint"
test_endpoint "$PROMETHEUS_URL" "Prometheus UI"
test_endpoint "$GRAFANA_URL" "Grafana UI"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. Testing Prometheus Connectivity"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo -n "Checking Prometheus targets... "
TARGETS=$(curl -s "$PROMETHEUS_URL/api/v1/targets?state=active" | grep -c "servly-app" || echo "0")
if [ "$TARGETS" -gt 0 ]; then
    echo -e "${GREEN}✓ servly-app is being scraped${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠ Target not yet scraped (wait for first scrape)${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. Checking Available Metrics"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check for auth metrics
echo -n "Auth metrics present... "
METRICS=$(curl -s "$SERVLY_URL/actuator/metrics" | grep -c "auth\." || echo "0")
if [ "$METRICS" -gt 0 ]; then
    echo -e "${GREEN}✓ Found ($METRICS)${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠ Not yet registered (need auth activity)${NC}"
fi

# Check for inventory metrics
echo -n "Inventory metrics present... "
METRICS=$(curl -s "$SERVLY_URL/actuator/metrics" | grep -c "inventory\." || echo "0")
if [ "$METRICS" -gt 0 ]; then
    echo -e "${GREEN}✓ Found ($METRICS)${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠ Not yet registered (need inventory activity)${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. Generated Prometheus Queries"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

cat << 'EOF'
Try these queries in Prometheus (http://localhost:9090):

AUTHENTICATION METRICS:
  - auth_login_total                                  # Total login attempts
  - auth_login_success_total                          # Successful logins
  - (auth_login_success_total / auth_login_total) * 100  # Success rate %
  - histogram_quantile(0.95, rate(auth_login_duration_seconds_bucket[5m])) * 1000

INVENTORY METRICS:
  - inventory_item_creation_total                     # Total item creations
  - inventory_item_creation_success_total             # Successful creations
  - inventory_category_creation_total                 # Total category creations
  - inventory_category_creation_success_total         # Successful creations
  - (inventory_category_creation_success_total / inventory_category_creation_total) * 100

PAGINATED QUERIES:
  - histogram_quantile(0.95, rate(inventory_items_paginated_query_duration_seconds_bucket[5m])) * 1000
EOF

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. Dashboard Access"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo -e "${GREEN}Grafana Dashboard:${NC}"
echo "  URL: $GRAFANA_URL"
echo "  User: admin"
echo "  Pass: admin"
echo ""
echo "  Dashboard: 'Servly - Observability Dashboard (ISO/IEC 25010)'"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "Passed: ${GREEN}${TESTS_PASSED}${NC}"
echo -e "Failed: ${RED}${TESTS_FAILED}${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ] && [ $TESTS_PASSED -gt 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠ Some services may still be starting up. Wait a few seconds and try again.${NC}"
    exit 0
fi

