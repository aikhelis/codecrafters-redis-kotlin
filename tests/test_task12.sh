#!/bin/bash

# Test script for Task 12 - XADD with auto-generated full IDs
echo "=== Testing Task 12: XADD with auto-generated full IDs (*) ==="

# Start the Redis server in background
cd /Users/alexikhelis/Development/aikhelis/codecrafters-2025/codecrafters-redis-kotlin
./your_program.sh > server_output.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 3

# Use a unique stream key to ensure clean state
STREAM_KEY="test_stream_$(date +%s)"

echo "1. Testing * (should return current timestamp with sequence 0)..."
echo "Command: XADD $STREAM_KEY * foo bar"
RESULT1=$(redis-cli -p 6379 XADD $STREAM_KEY * foo bar)
echo "Result: $RESULT1"

echo ""
echo "2. Testing * again (should return current timestamp with sequence 0 or 1)..."
echo "Command: XADD $STREAM_KEY * bar baz"
RESULT2=$(redis-cli -p 6379 XADD $STREAM_KEY * bar baz)
echo "Result: $RESULT2"

echo ""
echo "3. Testing explicit ID after * (should work if greater)..."
echo "Command: XADD $STREAM_KEY 9999999999999-0 test value"
RESULT3=$(redis-cli -p 6379 XADD $STREAM_KEY 9999999999999-0 test value)
echo "Result: $RESULT3"

echo ""
echo "4. Testing * after explicit ID (should use current time)..."
echo "Command: XADD $STREAM_KEY * final test"
RESULT4=$(redis-cli -p 6379 XADD $STREAM_KEY * final test)
echo "Result: $RESULT4"

# Clean up
kill $SERVER_PID
echo ""
echo "=== Task 12 test completed ==="

# Validate results
echo ""
echo "=== Validation ==="
if [[ $RESULT1 =~ ^[0-9]+-[0-9]+$ ]]; then
    echo "✓ Test 1: Generated valid ID format"
else
    echo "✗ Test 1: Invalid ID format: $RESULT1"
fi

if [[ $RESULT2 =~ ^[0-9]+-[0-9]+$ ]]; then
    echo "✓ Test 2: Generated valid ID format"
else
    echo "✗ Test 2: Invalid ID format: $RESULT2"
fi