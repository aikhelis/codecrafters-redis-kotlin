#!/bin/bash

# Test script for XADD ID validation (Task 10)
echo "Testing XADD ID validation..."

# Start the Redis server in background
cd /Users/alexikhelis/Development/aikhelis/codecrafters-2025/codecrafters-redis-kotlin
./your_program.sh &
SERVER_PID=$!

# Wait for server to start
sleep 2

echo "1. Testing valid IDs in sequence..."
redis-cli -p 6379 XADD stream_key 1-1 foo bar
redis-cli -p 6379 XADD stream_key 1-2 bar baz

echo "2. Testing same ID (should fail)..."
redis-cli -p 6379 XADD stream_key 1-2 baz foo

echo "3. Testing smaller time with larger sequence (should fail)..."
redis-cli -p 6379 XADD stream_key 0-3 baz foo

echo "4. Testing 0-0 ID (should fail)..."
redis-cli -p 6379 XADD stream_key 0-0 baz foo

echo "5. Testing valid larger ID..."
redis-cli -p 6379 XADD stream_key 2-1 test value

# Clean up
kill $SERVER_PID
echo "Test completed."