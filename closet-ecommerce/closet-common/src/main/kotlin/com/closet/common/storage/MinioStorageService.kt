package com.closet.common.storage

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * MinIO/S3 호환 스토리지 서비스 구현.
 *
 * AWS S3 SDK를 사용하되 endpoint를 MinIO로 지정한다.
 * 나중에 AWS S3로 전환할 때는 endpoint(storage.endpoint)만 제거하면 된다.
 */
@Service
@ConditionalOnBean(S3Client::class)
class MinioStorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val storageProperties: StorageProperties,
) : StorageService {
    override fun generatePresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationMinutes: Long,
    ): String {
        val putObjectRequest =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build()

        val presignRequest =
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build()

        val presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString()
        logger.debug { "Presigned upload URL 생성: bucket=$bucket, key=$key, expiration=${expirationMinutes}min" }
        return presignedUrl
    }

    override fun generatePresignedDownloadUrl(
        bucket: String,
        key: String,
        expirationMinutes: Long,
    ): String {
        val getObjectRequest =
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

        val presignRequest =
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build()

        val presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString()
        logger.debug { "Presigned download URL 생성: bucket=$bucket, key=$key, expiration=${expirationMinutes}min" }
        return presignedUrl
    }

    override fun deleteObject(
        bucket: String,
        key: String,
    ) {
        val deleteRequest =
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

        s3Client.deleteObject(deleteRequest)
        logger.info { "오브젝트 삭제 완료: bucket=$bucket, key=$key" }
    }

    override fun getPublicUrl(
        bucket: String,
        key: String,
    ): String {
        return "${storageProperties.endpoint}/$bucket/$key"
    }
}
