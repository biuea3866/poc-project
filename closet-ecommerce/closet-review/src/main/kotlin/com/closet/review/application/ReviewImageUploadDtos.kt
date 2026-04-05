package com.closet.review.application

import jakarta.validation.constraints.NotBlank

/**
 * 리뷰 이미지 Presigned URL 요청/응답 DTO.
 *
 * 클라이언트가 Presigned URL을 발급받아 MinIO/S3에 직접 업로드한다.
 */
data class PresignedUploadUrlRequest(
    @field:NotBlank val fileName: String,
    @field:NotBlank val contentType: String,
)

data class PresignedUploadUrlResponse(
    val uploadUrl: String,
    val imageUrl: String,
    val key: String,
)
