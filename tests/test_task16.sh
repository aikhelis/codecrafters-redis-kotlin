#!/bin/bash

# Test script for task 16 - XREAD command

echo "=== Testing XREAD Command (Task 16) ==="

# Start the Redis server in the background
cd /Users/alexikhelis/Development/aikhelis/codecrafters-2025/codecrafters-redis-kotlin
./your_program.sh &
SERVER_PID=$!

# Wait for server to start
sleep 3

echo "Server started with PID: $SERVER_PID"

# Test case from task 16: Add entry and test XREAD
echo "Adding entry to stream..."
redis-cli XADD stream_key 0-1 temperature 96

echo "Testing XREAD command..."
echo "Command: redis-cli XREAD streams stream_key 0-0"
redis-cli XREAD streams stream_key 0-0

echo ""
echo "=== Additional Tests ==="

# Add more entries for comprehensive testing
echo "Adding more entries..."
redis-cli XADD stream_key 0-2 humidity 85
redis-cli XADD stream_key 0-3 pressure 1013

echo "Testing XREAD with different starting points..."
echo "Command: redis-cli XREAD streams stream_key 0-1"
redis-cli XREAD streams stream_key 0-1

echo "Command: redis-cli XREAD streams stream_key 0-2"
redis-cli XREAD streams stream_key 0-2

echo "Testing XREAD with non-existent entries..."
echo "Command: redis-cli XREAD streams stream_key 0-5"
redis-cli XREAD streams stream_key 0-5

# Clean up
echo "Cleaning up..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "=== Test completed ==="