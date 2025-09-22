#!/bin/bash

# Script to reproduce the BLPOP timeout issue

echo "Starting Redis server..."
./your_program.sh &
SERVER_PID=$!

# Wait for server to start and check if it's listening
sleep 5
echo "Checking if server is listening on port 6379..."
netstat -an | grep 6379 || echo "Server not listening on port 6379"

echo "Testing BLPOP with timeout 0.4 (should block and timeout)..."
redis-cli BLPOP raspberry 0.4 > blpop1_output.txt &
BLPOP1_PID=$!

sleep 1

echo "Testing BLPOP with timeout 0.2 (should block)..."
redis-cli BLPOP orange 0.2 > blpop2_output.txt &
BLPOP2_PID=$!

sleep 0.1

echo "Adding element to orange list with RPUSH..."
redis-cli RPUSH orange grape > rpush_output.txt

# Wait for commands to complete
sleep 2

echo "Results:"
echo "BLPOP raspberry 0.4 output:"
cat blpop1_output.txt
echo ""

echo "BLPOP orange 0.2 output:"
cat blpop2_output.txt
echo ""

echo "RPUSH orange grape output:"
cat rpush_output.txt
echo ""

# Clean up
kill $BLPOP1_PID $BLPOP2_PID $SERVER_PID 2>/dev/null
rm -f blpop1_output.txt blpop2_output.txt rpush_output.txt
wait 2>/dev/null

echo "Test completed."