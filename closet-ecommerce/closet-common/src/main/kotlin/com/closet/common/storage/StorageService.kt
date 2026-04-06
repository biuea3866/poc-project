package com.closet.common.storage

/**
 * 오브젝트 스토리지 서비스 인터페이스.
 *
 * AWS S3 호환 API를 추상화하여 MinIO(개발) / S3(운영) 모두 지원한다.
 * Presigned URL 방식으로 클라이언트가 직접 업로드/다운로드한다.
 */
interface StorageService {
    /**
     * Presigned Upload URL 생성 (PUT).
     * 클라이언트가 이 URL로 직접 파일을 업로드한다.
     */
    fun generatePresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationMinutes: Long = 10,
    ): String

    /**
     * Presigned Download URL 생성 (GET).
     * 비공개 객체에 대한 임시 다운로드 URL을 발급한다.
     */
    fun generatePresignedDownloadUrl(
        bucket: String,
        key: String,
        expirationMinutes: Long = 60,
    ): String

    /**
     * 오브젝트 삭제.
     */
    fun deleteObject(
        bucket: String,
        key: String,
    )

    /**
     * 공개 URL 생성.
     * 버킷이 public-read로 설정된 경우 사용한다.
     */
    fun getPublicUrl(
        bucket: String,
        key: String,
    ): String
}
