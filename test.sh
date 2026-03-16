#!/bin/bash

# 1. Clean up previous artifacts
rm -f test.txt.choi test.txt.dec
echo "Cleaning up..."

# 2. Compile the C programs (choienc, choidec)
# Assuming Makefile exists. If not, using direct gcc commands.
pushd choicrypt
if [ -f "Makefile" ]; then
    make clean && make
else
    gcc -O2 choi_enc.c -o choienc
    gcc -O2 choi_dec.c -o choidec
fi

# 3. Create a sample plain text if not exists
if [ ! -f "test.txt" ]; then
		echo "Generating 10M random test file from /dev/urandom..."
		dd if=/dev/urandom of=test.txt bs=10M count=1 status=none
fi

# 4. Run Encryption
# Note: Providing password 'herenow1700xx881' via stdin as required by your C code
echo "Encrypting..."
echo "herenow1700xx881" | ./choienc test.txt
mv test.* ../
popd

# 5. Run Java Cryptanalysis Tool
# This assumes you are in the 'encrymania' directory or 'choicrypt' is inside it
echo "Starting Java Cryptanalysis..."
mvn exec:java -Dexec.mainClass="com.encrymania.CryptanalysisTool" \
    -Dexec.args="--key herenow1700xx881 --encrypted test.txt.choi --plain test.txt"
