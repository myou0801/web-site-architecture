#!/bin/sh

echo "Creating buckets..."

awslocal s3api create-bucket --bucket test-bucket

echo "Bucket creation completed"

echo "Creating queue..."

awslocal sqs create-queue --queue-name test-queue 

echo "Queue creatin completed"