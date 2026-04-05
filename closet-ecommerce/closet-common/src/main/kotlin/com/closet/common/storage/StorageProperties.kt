package com.closet.common.storage

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 오브젝트 스토리지 설정.
 *
 * MinIO(개발) / AWS S3(운영) 모두 동일한 S3 호환 API를 사용하며,
 * endpoint만 변경하면 AWS 전환이 가능하다.
 */
@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    val endpoint: String = "http://localhost:9000",
    val accessKey: String = "closet-admin",
    val secretKey: String = "closet-secret-key",
    val region: String = "ap-northeast-2",
    val productBucket: String = "closet-product-images",
    val reviewBucket: String = "closet-review-images",
)
