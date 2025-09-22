#!/bin/bash

# Test script to reproduce the BLPOP multiple clients issue
echo "Starting Redis server..."
./your_program.sh &
SERVER_PID=$!

# Wait for server to start and check if it's listening
sleep 5
echo "Checking if server is listening on port 6379..."
netstat -an | grep 6379 || echo "Server not listening on port 6379"

echo "Testing BLPOP with multiple blocking clients..."

# Start two BLPOP clients in background
echo "Starting client 1 (BLPOP blueberry 0)..."
redis-cli BLPOP blueberry 0 > client1_output.txt &
CLIENT1_PID=$!

echo "Starting client 2 (BLPOP blueberry 0)..."
redis-cli BLPOP blueberry 0 > client2_output.txt &
CLIENT2_PID=$!

# Wait a moment for clients to connect and block
sleep 2

echo "Sending RPUSH blueberry pear..."
redis-cli RPUSH blueberry pear

# Wait for responses
sleep 2

echo "Checking results..."

# Check client outputs
echo "Client 1 output:"
cat client1_output.txt
echo ""

echo "Client 2 output:"
cat client2_output.txt
echo ""

# Count how many clients received responses
RESPONSES=0
if [ -s client1_output.txt ]; then
    RESPONSES=$((RESPONSES + 1))
fi
if [ -s client2_output.txt ]; then
    RESPONSES=$((RESPONSES + 1))
fi

echo "Number of clients that received responses: $RESPONSES"

if [ $RESPONSES -eq 1 ]; then
    echo "✓ TEST PASSED: Only one client received the response (correct behavior)"
else
    echo "✗ TEST FAILED: $RESPONSES clients received responses (should be 1)"
fi

# Cleanup
kill $CLIENT1_PID $CLIENT2_PID $SERVER_PID 2>/dev/null
rm -f client1_output.txt client2_output.txt
wait 2>/dev/null