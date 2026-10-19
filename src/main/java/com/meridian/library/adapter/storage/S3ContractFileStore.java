package com.meridian.library.adapter.storage;

import com.meridian.library.application.ContractFileStore;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

public class S3ContractFileStore implements ContractFileStore {

    private final S3Client s3;
    private final String bucket;

    public S3ContractFileStore(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public void ensureBucket() {
        try {
            s3.headBucket(request -> request.bucket(bucket));
        } catch (NoSuchBucketException e) {
            s3.createBucket(request -> request.bucket(bucket));
        }
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        s3.putObject(request -> request.bucket(bucket).key(key).contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256),
                RequestBody.fromBytes(content));
    }

    @Override
    public byte[] get(String key) {
        return s3.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asByteArray();
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(request -> request.bucket(bucket).key(key));
    }
}
