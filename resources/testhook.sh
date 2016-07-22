#!/bin/sh

input_file_uri="$1"
input_file_original_name="$2"

echo "FROM SCRIPT START"
echo "$input_file_uri"
echo "$input_file_original_name"
md5sum "$input_file_uri"
echo "FROM SCRIPT END"
