package com.closet.common.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * S3 클라이언트 설정.
 *
 * MinIO는 path-style access만 지원하므로 pathStyleAccessEnabled = true로 설정한다.
 * storage.endpoint 프로퍼티가 존재할 때만 빈이 생성된다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties::class)
@ConditionalOnProperty(prefix = "storage", name = ["endpoint"])
class StorageConfig(
    private val storageProperties: StorageProperties,
) {
    @Bean
    fun s3Client(): S3Client {
        val credentials =
            AwsBasicCredentials.create(
                storageProperties.accessKey,
                storageProperties.secretKey,
            )

        return S3Client.builder()
            .endpointOverride(URI.create(storageProperties.endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(storageProperties.region))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build(),
            )
            .forcePathStyle(true)
            .build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials =
            AwsBasicCredentials.create(
                storageProperties.accessKey,
                storageProperties.secretKey,
            )

        return S3Presigner.builder()
            .endpointOverride(URI.create(storageProperties.endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(storageProperties.region))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build(),
            )
            .build()
    }
}
