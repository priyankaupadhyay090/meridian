package com.meridian.library.adapter.storage;

import com.meridian.library.application.ContractFileStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Bean
    ContractFileStore contractFileStore(@Value("${meridian.storage.provider:memory}") String provider,
                                        @Value("${meridian.storage.s3.bucket:meridian-contracts}") String bucket,
                                        @Value("${meridian.storage.s3.region:eu-central-1}") String region,
                                        @Value("${meridian.storage.s3.endpoint:}") String endpoint) {
        if (!provider.equals("s3")) {
            return new InMemoryContractFileStore();
        }
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        if (endpoint.isBlank()) {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        } else {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        }
        S3ContractFileStore store = new S3ContractFileStore(builder.build(), bucket);
        store.ensureBucket();
        return store;
    }
}
