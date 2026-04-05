package com.closet.common.storage

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI

class MinioStorageServiceTest : BehaviorSpec({

    val s3Client = mockk<S3Client>()
    val s3Presigner = mockk<S3Presigner>()
    val storageProperties = StorageProperties(
        endpoint = "http://localhost:9000",
        accessKey = "closet-admin",
        secretKey = "closet-secret-key",
        region = "ap-northeast-2",
        productBucket = "closet-product-images",
        reviewBucket = "closet-review-images",
    )

    val storageService = MinioStorageService(s3Client, s3Presigner, storageProperties)

    Given("Presigned Upload URL 생성 시") {
        val bucket = "closet-review-images"
        val key = "reviews/1/test-uuid.jpg"
        val contentType = "image/jpeg"

        val presignedPutRequest = mockk<PresignedPutObjectRequest>()
        every { presignedPutRequest.url() } returns URI("http://localhost:9000/closet-review-images/reviews/1/test-uuid.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256").toURL()
        every { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } returns presignedPutRequest

        When("generatePresignedUploadUrl을 호출하면") {
            val result = storageService.generatePresignedUploadUrl(bucket, key, contentType)

            Then("S3 Presigner를 사용하여 PUT URL을 생성한다") {
                result shouldContain "closet-review-images"
                result shouldContain "X-Amz-Algorithm"
                verify(exactly = 1) { s3Presigner.presignPutObject(any<PutObjectPresignRequest>()) }
            }
        }
    }

    Given("Presigned Download URL 생성 시") {
        val bucket = "closet-review-images"
        val key = "reviews/1/test-uuid.jpg"

        val presignedGetRequest = mockk<PresignedGetObjectRequest>()
        every { presignedGetRequest.url() } returns URI("http://localhost:9000/closet-review-images/reviews/1/test-uuid.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256").toURL()
        every { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) } returns presignedGetRequest

        When("generatePresignedDownloadUrl을 호출하면") {
            val result = storageService.generatePresignedDownloadUrl(bucket, key)

            Then("S3 Presigner를 사용하여 GET URL을 생성한다") {
                result shouldContain "closet-review-images"
                result shouldContain "X-Amz-Algorithm"
                verify(exactly = 1) { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) }
            }
        }
    }

    Given("오브젝트 삭제 시") {
        val bucket = "closet-review-images"
        val key = "reviews/1/test-uuid.jpg"

        every { s3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse.builder().build()

        When("deleteObject를 호출하면") {
            storageService.deleteObject(bucket, key)

            Then("S3Client를 사용하여 오브젝트를 삭제한다") {
                verify(exactly = 1) { s3Client.deleteObject(any<DeleteObjectRequest>()) }
            }
        }
    }

    Given("공개 URL 생성 시") {
        val bucket = "closet-review-images"
        val key = "reviews/1/test-uuid.jpg"

        When("getPublicUrl을 호출하면") {
            val result = storageService.getPublicUrl(bucket, key)

            Then("endpoint/bucket/key 형식의 URL을 반환한다") {
                result shouldBe "http://localhost:9000/closet-review-images/reviews/1/test-uuid.jpg"
            }
        }
    }
})
