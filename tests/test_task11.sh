#!/bin/bash

# Test script for Task 11 - XADD with auto-generated sequence numbers
echo "=== Testing Task 11: XADD with auto-generated sequence numbers ==="

# Start the Redis server in background
cd /Users/alexikhelis/Development/aikhelis/codecrafters-2025/codecrafters-redis-kotlin
./your_program.sh > server_output.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 3

# Use a unique stream key to ensure clean state
STREAM_KEY="test_stream_$(date +%s)"

echo "1. Testing 0-* (should return 0-1)..."
echo "Command: XADD $STREAM_KEY 0-* foo bar"
redis-cli -p 6379 XADD $STREAM_KEY 0-* foo bar

echo ""
echo "2. Testing 5-* (should return 5-0)..."
echo "Command: XADD $STREAM_KEY 5-* foo bar"
redis-cli -p 6379 XADD $STREAM_KEY 5-* foo bar

echo ""
echo "3. Testing 5-* again (should return 5-1)..."
echo "Command: XADD $STREAM_KEY 5-* bar baz"
redis-cli -p 6379 XADD $STREAM_KEY 5-* bar baz

echo ""
echo "4. Testing another time part 6-* (should return 6-0)..."
echo "Command: XADD $STREAM_KEY 6-* test value"
redis-cli -p 6379 XADD $STREAM_KEY 6-* test value

# Clean up
kill $SERVER_PID
echo ""
echo "=== Task 11 test completed ==="