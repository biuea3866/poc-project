package com.closet.review.application

import com.closet.common.storage.StorageProperties
import com.closet.common.storage.StorageService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 리뷰 이미지 업로드 서비스.
 *
 * Presigned URL을 발급하여 클라이언트가 MinIO/S3에 직접 업로드하도록 한다.
 * 서버를 거치지 않으므로 대용량 이미지 업로드에도 서버 부하가 없다.
 */
@Service
class ReviewImageService(
    private val storageService: StorageService,
    private val storageProperties: StorageProperties,
) {
    /**
     * 리뷰 이미지 Presigned Upload URL 생성.
     *
     * key 형식: reviews/{memberId}/{uuid}.{ext}
     */
    fun generatePresignedUploadUrl(
        memberId: Long,
        request: PresignedUploadUrlRequest,
    ): PresignedUploadUrlResponse {
        val ext = request.fileName.substringAfterLast('.', "jpg")
        val key = "reviews/$memberId/${UUID.randomUUID()}.$ext"
        val bucket = storageProperties.reviewBucket

        val uploadUrl =
            storageService.generatePresignedUploadUrl(
                bucket = bucket,
                key = key,
                contentType = request.contentType,
            )

        val imageUrl = storageService.getPublicUrl(bucket, key)

        logger.info { "리뷰 이미지 Presigned URL 생성: memberId=$memberId, key=$key" }

        return PresignedUploadUrlResponse(
            uploadUrl = uploadUrl,
            imageUrl = imageUrl,
            key = key,
        )
    }
}
