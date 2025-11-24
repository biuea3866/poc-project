#!/bin/bash
set -e

echo "Waiting for cache loading to complete..."

# Wait for cache loading
while true; do
  if grep -q "Eager cache loading completed" app-log.txt; then
    echo "✓ Cache loading completed!"
    break
  fi

  LATEST=$(tail -100 app-log.txt | grep "Loaded.*orders to cache" | tail -1 || echo "Loading...")
  echo "$(date '+%H:%M:%S') - $LATEST"
  sleep 20
done

# Wait for application to fully start
while true; do
  if grep -q "Started CachePracticeApplicationKt" app-log.txt; then
    echo "✓ Application started!"
    break
  fi
  echo "Waiting for application to finish startup..."
  sleep 5
done

# Wait for actuator to be ready
echo "Waiting for actuator endpoint..."
sleep 10

for i in {1..30}; do
  if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo "✓ Application is healthy!"
    break
  fi
  echo "Waiting for health check..."
  sleep 2
done

echo ""
echo "========================================="
echo "Starting k6 Load Tests"
echo "========================================="
echo ""

# Test 1: No Cache
echo "1/3: Running NO CACHE baseline test..."
k6 run --quiet k6/test-no-cache.js --out json=results-no-cache.json 2>&1 | tee k6-no-cache.log
echo "✓ No cache test completed"
echo ""

# Test 2: Lazy Loading
echo "2/3: Running LAZY LOADING test..."
k6 run --quiet k6/test-lazy-cache.js --out json=results-lazy.json 2>&1 | tee k6-lazy.log
echo "✓ Lazy loading test completed"
echo ""

# Test 3: Eager Loading with TTL
echo "3/3: Running EAGER LOADING test (2 minutes, observing TTL expiration)..."
k6 run --quiet k6/test-eager-cache.js --out json=results-eager.json 2>&1 | tee k6-eager.log
echo "✓ Eager loading test completed"
echo ""

echo "========================================="
echo "All tests completed!"
echo "========================================="
echo ""
echo "Results files generated:"
echo "  - results-no-cache.json"
echo "  - results-lazy.json"
echo "  - results-eager.json"
echo "  - k6-no-cache.log"
echo "  - k6-lazy.log"
echo "  - k6-eager.log"
echo ""
