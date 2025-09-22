In this section, you'll add support for creating a new list using the RPUSH command.

# Stage 1

The RPUSH Command

The RPUSH command is used to append elements to a list. If the list doesn't exist, it is created first.

Example usage:

Creating a new list with single element

> RPUSH list_key "foo"

> (integer) 1

Appending a single element to an existing list

> RPUSH list_key "bar"

> (integer) 2

The return value is the number of elements in the list after appending. It is encoded as a RESP integer.

Tests

The tester will execute your program like this:

./your_program.sh

It will then send the following command to your program.

$ redis-cli RPUSH list_key "element"

The tester will verify that the response to the command is :1\r\n, which is 1 (the number of elements in the list), encoded as a RESP Integer.


# Stage 2

In this stage, you'll add support for RPUSH when a list already exists and a single element is being appended.

Tests

The tester will execute your program like this:

./your_program.sh

It will then send multiple RPUSH commands specifying the same list.

$ redis-cli RPUSH list_key "element1"

Expect: (integer) 1 → encoded as :1\r\n

$ redis-cli RPUSH list_key "element2"

Expect: (integer) 2 → encoded as :2\r\n

In each case, the tester will expect the response to be the length of the list as a RESP encoded integer.


# Stage 3

In this stage, you'll add support for appending multiple elements in a single RPUSH command.

RPUSH with multiple elements

RPUSH supports specifying multiple elements in a single command. Example usage:

Creating a new list with multiple elements

> RPUSH another_list "bar" "baz"

> (integer) 2

Appending multiple elements to an existing list

> RPUSH another_list "foo" "bar" "baz"
> (integer) 5

The response to each command is a RESP integer indicating the length of the list after appending.

Tests

The tester will execute your program like this:

./your_program.sh

It will then send multiple RPUSH commands, each including more than one element to append to the list.

$ redis-cli RPUSH list_key "element1" "element2" "element3"

Expect: (integer) 3 → encoded as :3\r\n

$ redis-cli RPUSH list_key "element4" "element5"

Expect: (integer) 5 → encoded as :5\r\n

In each case, the tester will expect the response to be the length of the list as a RESP encoded integer.

Notes
- RPUSH accepts multiple elements even when creating a new list, not only when appending to an existing list.


# Stage 4

In this stage, you will add support for listing the elements of a list using the LRANGE command.

The LRANGE command

The LRANGE command is used to list the elements in a list given a start index and end index. The index of the first element 0. The end index is inclusive, which means that the element at the end index will be included in the response.

Example usage:

Create a list with 5 items

> RPUSH list_key "a" "b" "c" "d" "e"
> (integer) 5

List first 2 items
> LRANGE list_key 0 1
1) "a"
2) "b"

List items from indexes 2-4
> LRANGE list_key 2 4
1) "c"
2) "d"
3) "e"
   
Here are some additional notes on how the LRANGE command behaves with different types of inputs:
- If the list does not exist, an empty array is returned 
- If the start index is greater than or equal to the list's length, an empty array is returned. 
- If the stop index is greater than the list's length, the stop index is treated as the last element. 
- If the start index is greater than the stop index, the result is an empty array.

Tests

The tester will execute your program like this:

./your_program.sh

It will then create a new list with multiple elements.

$ redis-cli RPUSH list_key "a" "b" "c" "d" "e"

After that the tester will send your program a series of LRANGE commands. It will expect the response to be an RESP Array or empty array in each case, depending on the test case.

As an example, the tester might send your program a command like this:

$ redis-cli LRANGE list_key 0 2

Expect RESP Encoded Array: ["a", "b", "c"]

It will expect the response to be an RESP-encoded array ["a", "b", "c"], which would look like this:

*3\r\n
$1\r\n
a\r\n
$1\r\n
b\r\n
$1\r\n
c\r\n

The tester will issue multiple such commands and verify their responses.

Notes
- In this stage, you will only implement LRANGE with non-negative indexes. We will get to handling LRANGE for negative indexes in the next stage.
- If a list doesn't exist, LRANGE should respond with an empty RESP array (*0\r\n).


# Stage 5

In this stage, you will add support for negative indexes for the LRANGE command.

LRANGE with negative indexes

The LRANGE command can accept negative indexes too, example usage:

Create a list with 5 items
> RPUSH list_key "a" "b" "c" "d" "e"

> (integer) 5

List last 2 items
> LRANGE list_key -2 -1
1) "d"
2) "e"

List all items expect last 2
> LRANGE list_key 0 -3
1) "a"
2) "b"
3) "c"

An index of -1 refers to the last element, -2 to the second last, and so on. If a negative index is out of range (i.e. >= the length of the list), it is treated as 0 (start of the list).

Tests

The tester will execute your program like this:

./your_program.sh

It will then create a new list with multiple elements.

$ redis-cli RPUSH list_key "a" "b" "c" "d" "e"

The tester will then send your program a series of LRANGE commands with one or more negative indexes.

For example, the tester might send you this command:

$ redis-cli LRANGE list_key 2 -1

In this case, the tester will verify that the response is the array ["c", "d", "e"], which is RESP Encoded as:

*3\r\n
$1\r\n
c\r\n
$1\r\n
d\r\n
$1\r\n
e\r\n


# Stage 6

In this stage, you'll add support for the LPUSH command, which prepends elements to a list.

The LPUSH Command

The LPUSH command is similar to RPUSH, except that it inserts elements from the left rather than right. If a list doesn't exist, it is created first before prepending elements.

Example usage:

> LPUSH list_key "a" "b" "c"

(integer) 3

> LRANGE list_key 0 -1
1) "c"
2) "b"
3) "a"

Tests

The tester will execute your program like this:

./your_program.sh

It will then send a series of LPUSH commands and expect the response to be the list length in each case, which is a RESP integer.

> $ redis-cli LPUSH list_key "c"

Expect: (integer) 1

> LPUSH list_key "b" "a"

Expect: (integer) 3

It'll also use the LRANGE command to verify that elements are inserted in the correct order.

> LRANGE list_key 0 -1

Expect RESP Encoded Array: ["a", "b", "c"]


# Stage 7

In this stage, you'll add support for querying the length of a list using LLEN.

The LLEN Command

The LLEN command is used to query a list's length. It returns a RESP-encoded integer.

Example usage:

> RPUSH list_key "a" "b" "c" "d"

(integer) 4

> LLEN list_key

(integer) 4

Tests

The tester will execute your program like this:

./your_program.sh

It will then create a list with random number of elements using RPUSH.

> redis-cli RPUSH list_key <random number of elements>

The tester will then send a LLEN command specifying the list that was just created.

> LLEN list_key

Expect: list_length (RESP Encoded Integer)

It will expect the response to be length of the list encoded as a RESP integer.

It will also verify the response of LLEN command for a non-existent list.

> LLEN non_existent_list_key

Expect:  (integer) 0

The tester expects 0, which is RESP Encoded as :0\r\n.


# Stage 8

In this stage, you'll implement support for removing a single element from the left using the LPOP command.

The LPOP Command

The LPOP command removes and returns the first element of the list. If the list is empty or doesn't exist, it returns a null bulk string ($-1\r\n).

Example usage:

> RPUSH list_key "a" "b" "c" "d"

(integer) 4

> LPOP list_key

"a"

Tests

The tester will execute your program like this:

./your_program.sh

It will then create a list with some elements.

> redis-cli RPUSH list_key "one" "two" "three" "four" "five"

It will then send an LPOP command to your server specifying the list that was just created.

> LPOP list_key

Expecting: (Bulk string) "one"

The tester will expect the removed element to be returned, which is encoded as a RESP Bulk string ($3\r\none\r\n).

The tester will also verify that the remaining elements are present in the list using the LRANGE command.

> LRANGE list_key 0 -1

Expect RESP Encoded Array: ["two", "three", "four", "five"]


# Stage 9

In this stage, you'll add support for removing multiple elements in a single LPOP command.

LPOP with multiple elements

The LPOP command accepts an optional argument to specify how many elements are to be removed.

Example usage:

> RPUSH list_key "a" "b" "c" "d"

(integer) 4

> LPOP list_key 2
1) "a"
2) "b"

> LRANGE list_key 0 -1
1) "c"
2) "d"

If the number of elements to remove is greater than the list length, it returns RESP encoded array of all the elements of the list.

Tests

The tester will execute your program like this:

./your_program.sh

It will create a list with multiple elements in it.

> redis-cli RPUSH list_key "one" "two" "three" "four" "five"

Expect: 4 (Resp Encoded Integer)

After that it will send your program a LPOP command with the number of elements to remove.

> LPOP list_key 2

Expect RESP Encoded Array: ["one", "two"]

The tester will verify that the response is a RESP encoded array of removed elements.

It will also use the LRANGE command to verify the remaining elements in the list.

> LRANGE list_key 0 -1

Expect RESP Encoded Array: ["three", "four", "five"]


# Stage 10

In this stage, you'll add support for the BLPOP command, which blocks until an element is available to be popped.

The BLPOP Command
BLPOP is a blocking variant of the LPOP command. It allows clients to wait for an element to become available on one or more lists. If the list is empty, the command blocks until:

An element is pushed to the list
Or the specified timeout is reached (in seconds)
It blocks indefinitely if the timeout specified is 0.
Example usage:

# Here the timeout is specified as 0 (i.e. wait indefinitely)
> BLPOP list_key 0

# ... this blocks until an element is added to the list

# As soon as an element is added, it responds with a resp array:
1) "list_key"
2) "foobar"
   If a timeout duration is supplied, it is the number of seconds the client will wait for an element to be available for removal. If no elements were inserted during this interval, the server returns a null bulk string ($-1\r\n).

If an element was inserted during this interval, the server removes it from the list and responds to the blocking client with a RESP-encoded array containing two elements:

The list name (as a bulk string)
The element that was popped (as a bulk string)
If multiple clients are blocked for BLPOP command, the server responds to the client which has been blocked for the longest duration.

Tests
The tester will execute your program like this:

./your_program.sh
It will then send a BLPOP command with the timeout set to 0:

$ redis-cli BLPOP list_key 0
# (Blocks)
After a short while, it'll then send RPUSH using another client:

# In another client:
$ redis-cli RPUSH list_key "foo"
# Expect: (integer) 1
The tester will then expect the following response from the first client:

# RESP encoding of ["list_key", "foo"] ->
*2\r\n
$8\r\n
list_key\r\n
$3\r\n
foo\r\n
The tester will also test BLPOP using multiple blocking clients. For example, it will spawn multiple clients one after another, and send BLPOP command from each client specifying the same list.

# Client 1 sends BLPOP first
$ redis-cli BLPOP another_list_key 0

# Client 2 sends BLPOP second
$ redis-cli BLPOP another_list key 0
It will then spawn a separate client which will send RPUSH to the server specifying the list.

$ redis-cli RPUSH list_key "foo"
The tester will expect the response to be sent to Client 1 since it has been blocked for the longest time.

Notes
- In this stage, the timeout argument will always be 0, i.e. BLPOP should wait indefinitely. We'll get to non-zero timeouts in later stages.

# Stage 11

In this stage, you will add support for a non-zero timeout duration for the BLPOP command.

Tests

The tester will execute your program like this:

./your_program.sh

It will then send a BLPOP command with a non-zero timeout:
```
$ redis-cli BLPOP list_key 0.1
# (Blocks)
```
After the timeout expires, the tester will expect to receive a null bulk string ($-1\r\n) as the response.

The tester will also test the case where an element is appended to the list before the timeout is reached. In this case, the response should be a RESP encoded array like ["list_key", "foo"] where foo is the added element.