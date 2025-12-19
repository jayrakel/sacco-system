#!/bin/bash

echo "Testing Spring Boot application startup..."
echo "==========================================="

# Clean and build
echo "1. Building application..."
./mvnw clean package -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✓ Build successful"

# Try to start the application with timeout
echo "2. Starting application (will timeout after 90 seconds)..."
timeout 90s ./mvnw spring-boot:run > startup_test.log 2>&1 &
PID=$!

# Wait for startup or error
sleep 30

# Check if process is still running
if ps -p $PID > /dev/null; then
    echo "✓ Application is running"

    # Check for errors in log
    if grep -i "ERROR.*Application run failed" startup_test.log > /dev/null; then
        echo "❌ Application failed to start. Errors found:"
        grep -A 10 "ERROR.*Application run failed" startup_test.log
        kill $PID 2>/dev/null
        exit 1
    elif grep "Started SaccoSystemApplication" startup_test.log > /dev/null; then
        echo "✓ Application started successfully!"
        grep "Started SaccoSystemApplication" startup_test.log
        kill $PID 2>/dev/null
        exit 0
    else
        echo "⏳ Application still starting... Check startup_test.log for details"
        tail -20 startup_test.log
        kill $PID 2>/dev/null
        exit 0
    fi
else
    echo "❌ Application stopped unexpectedly"
    tail -50 startup_test.log
    exit 1
fi

