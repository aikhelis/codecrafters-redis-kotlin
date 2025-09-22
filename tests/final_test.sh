#!/bin/bash

# Final test matching task 10 requirements exactly
echo "=== Final Test for Redis XADD ID Validation (Task 10) ==="

# Start the Redis server in background
cd /Users/alexikhelis/Development/aikhelis/codecrafters-2025/codecrafters-redis-kotlin
./your_program.sh > server_output.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
sleep 3

# Use a unique stream key with timestamp to ensure clean state
STREAM_KEY="test_stream_$(date +%s)"

echo "1. Creating entries with valid sequential IDs..."
echo "Command: XADD $STREAM_KEY 1-1 foo bar"
redis-cli -p 6379 XADD $STREAM_KEY 1-1 foo bar
echo "Command: XADD $STREAM_KEY 1-2 bar baz"
redis-cli -p 6379 XADD $STREAM_KEY 1-2 bar baz

echo ""
echo "2. Testing same ID as last entry (should fail)..."
echo "Command: XADD $STREAM_KEY 1-2 baz foo"
redis-cli -p 6379 XADD $STREAM_KEY 1-2 baz foo

echo ""
echo "3. Testing smaller time with larger sequence (should fail)..."
echo "Command: XADD $STREAM_KEY 0-3 baz foo"
redis-cli -p 6379 XADD $STREAM_KEY 0-3 baz foo

echo ""
echo "4. Testing 0-0 ID (should fail)..."
echo "Command: XADD $STREAM_KEY 0-0 baz foo"
redis-cli -p 6379 XADD $STREAM_KEY 0-0 baz foo

echo ""
echo "5. Testing valid larger ID..."
echo "Command: XADD $STREAM_KEY 2-1 test value"
redis-cli -p 6379 XADD $STREAM_KEY 2-1 test value

# Clean up
kill $SERVER_PID
echo ""
echo "=== Test completed successfully! ==="