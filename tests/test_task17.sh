#!/bin/bash

# Test script for task 17 - XREAD command with multiple streams

echo "=== Testing XREAD Multi-Stream Command (Task 17) ==="

# Start the Redis server in the background
cd /Users/alexikhelis/Development/aikhelis/codecrafters-2025/codecrafters-redis-kotlin
./your_program.sh &
SERVER_PID=$!

# Wait for server to start
sleep 3

echo "Server started with PID: $SERVER_PID"

# Test case from task 17: Add entries to multiple streams
echo "Adding entries to multiple streams..."
redis-cli XADD stream_key 0-1 temperature 95
redis-cli XADD other_stream_key 0-2 humidity 97

echo "Testing multi-stream XREAD command..."
echo "Command: redis-cli XREAD streams stream_key other_stream_key 0-0 0-1"
redis-cli XREAD streams stream_key other_stream_key 0-0 0-1

echo ""
echo "=== Additional Tests ==="

# Test single stream (backward compatibility)
echo "Testing single stream XREAD (backward compatibility)..."
echo "Command: redis-cli XREAD streams stream_key 0-0"
redis-cli XREAD streams stream_key 0-0

# Test with more streams
echo "Adding third stream..."
redis-cli XADD third_stream 0-3 pressure 1013

echo "Testing three streams..."
echo "Command: redis-cli XREAD streams stream_key other_stream_key third_stream 0-0 0-1 0-2"
redis-cli XREAD streams stream_key other_stream_key third_stream 0-0 0-1 0-2

# Test edge cases
echo "Testing with no matching entries..."
echo "Command: redis-cli XREAD streams stream_key other_stream_key 0-5 0-5"
redis-cli XREAD streams stream_key other_stream_key 0-5 0-5

# Clean up
echo "Cleaning up..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "=== Test completed ==="