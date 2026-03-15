# Stage 12: BE 코드 생성

> 08_ticket_breakdown.md + 09_risk_scoring.md 기준 | 작성일: 2026-03-14

---

## 티켓별 구현 코드

---

### NAW-BE-001: Document 도메인 POJO + 상태 머신

**모듈**: `core/domain`

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/model/DocumentStatus.kt`

```kotlin
package com.biuea.aiwiki.domain.document.model

enum class DocumentStatus {
    DRAFT,
    ACTIVE,
    DELETED,
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/model/AiStatus.kt`

```kotlin
package com.biuea.aiwiki.domain.document.model

enum class AiStatus {
    NOT_STARTED,
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/model/Document.kt`

```kotlin
package com.biuea.aiwiki.domain.document.model

import java.time.Instant

class Document(
    val id: Long? = null,
    title: String,
    content: String,
    status: DocumentStatus = DocumentStatus.DRAFT,
    aiStatus: AiStatus = AiStatus.NOT_STARTED,
    tags: List<String> = emptyList(),
    val createdBy: Long,
    updatedBy: Long,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
) {
    var title: String = title
        private set

    var content: String = content
        private set

    var status: DocumentStatus = status
        private set

    var aiStatus: AiStatus = aiStatus
        private set

    private val _tags: MutableList<String> = tags.toMutableList()
    val tags: List<String>
        get() = _tags.toList()

    var updatedBy: Long = updatedBy
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var summary: String? = null
        private set

    fun activate(userId: Long) {
        status = DocumentStatus.ACTIVE
        updatedBy = userId
    }

    fun updateDraft(title: String, content: String, tags: List<String>, userId: Long) {
        this.title = title
        this.content = content
        _tags.clear()
        _tags.addAll(tags)
        updatedBy = userId
    }

    fun requestAnalysis(): Document {
        require(status == DocumentStatus.ACTIVE) { "ACTIVE 문서만 분석할 수 있습니다." }
        require(aiStatus != AiStatus.PROCESSING) { "PROCESSING 중에는 재요청할 수 없습니다." }
        aiStatus = AiStatus.PENDING
        return this
    }

    fun startProcessing() {
        require(aiStatus == AiStatus.PENDING) { "PENDING 상태에서만 처리를 시작할 수 있습니다." }
        aiStatus = AiStatus.PROCESSING
    }

    fun completeAnalysis(summary: String, tags: List<String>) {
        require(aiStatus == AiStatus.PROCESSING) { "PROCESSING 상태에서만 완료할 수 있습니다." }
        this.summary = summary
        _tags.clear()
        _tags.addAll(tags)
        aiStatus = AiStatus.COMPLETED
    }

    fun failAnalysis() {
        aiStatus = AiStatus.FAILED
    }

    fun applySummary(value: String) {
        summary = value
    }

    fun markUpdated(at: Instant) {
        updatedAt = at
    }
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/model/DocumentRevision.kt`

```kotlin
package com.biuea.aiwiki.domain.document.model

import java.time.Instant

class DocumentRevision(
    val id: Long? = null,
    val documentId: Long,
    val title: String,
    val content: String,
    val status: DocumentStatus,
    val createdBy: Long,
    val createdAt: Instant? = null,
)
```

---

### NAW-BE-002: Outbound Port 인터페이스 정의

**모듈**: `core/domain`

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/port/DocumentRepository.kt`

```kotlin
package com.biuea.aiwiki.domain.document.port

import com.biuea.aiwiki.domain.document.model.Document

interface DocumentRepository {
    fun save(document: Document): Document
    fun findById(id: Long): Document?
    fun searchActiveOwnedByUser(query: String, userId: Long): List<Document>
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/port/DocumentRevisionRepository.kt`

```kotlin
package com.biuea.aiwiki.domain.document.port

import com.biuea.aiwiki.domain.document.model.DocumentRevision

interface DocumentRevisionRepository {
    fun save(revision: DocumentRevision): DocumentRevision
    fun findByDocumentId(documentId: Long): List<DocumentRevision>
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/port/SummaryPort.kt`

```kotlin
package com.biuea.aiwiki.domain.document.port

interface SummaryPort {
    fun summarize(content: String): String
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/port/TaggerPort.kt`

```kotlin
package com.biuea.aiwiki.domain.document.port

interface TaggerPort {
    fun generateTags(content: String): List<String>
}
```

#### `core/domain/src/main/kotlin/com/biuea/aiwiki/domain/document/port/EmbeddingPort.kt`

```kotlin
package com.biuea.aiwiki.domain.document.port

interface EmbeddingPort {
    fun embed(content: String): List<Float>
}
```

---

### NAW-BE-003: JPA Entity + Repository + Mapper

**모듈**: `adapters/persistence-jpa`

#### `adapters/persistence-jpa/src/main/kotlin/com/biuea/aiwiki/adapter/persistence/jpa/document/entity/DocumentJpaEntity.kt`

```kotlin
package com.biuea.aiwiki.adapter.persistence.jpa.document.entity

import com.biuea.aiwiki.domain.document.model.AiStatus
import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "documents")
class DocumentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DocumentStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false)
    var aiStatus: AiStatus,
    @ElementCollection
    @CollectionTable(name = "document_tags", joinColumns = [JoinColumn(name = "document_id")])
    @Column(name = "tag_name")
    var tags: MutableList<String> = mutableListOf(),
    @Column(name = "summary", columnDefinition = "TEXT")
    var summary: String? = null,
    @Column(name = "created_by", nullable = false)
    var createdBy: Long,
    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version
    @Column(name = "entity_version", nullable = false)
    var version: Long = 0L,
) {
    fun toDomain(): Document {
        val doc = Document(
            id = id,
            title = title,
            content = content,
            status = status,
            aiStatus = aiStatus,
            tags = tags.toList(),
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
        summary?.let { doc.applySummary(it) }
        return doc
    }

    companion object {
        fun from(document: Document): DocumentJpaEntity =
            DocumentJpaEntity(
                id = document.id,
                title = document.title,
                content = document.content,
                status = document.status,
                aiStatus = document.aiStatus,
                tags = document.tags.toMutableList(),
                summary = document.summary,
                createdBy = document.createdBy,
                updatedBy = document.updatedBy,
                createdAt = document.createdAt ?: Instant.now(),
                updatedAt = document.updatedAt ?: Instant.now(),
            )
    }
}
```

#### `adapters/persistence-jpa/src/main/kotlin/com/biuea/aiwiki/adapter/persistence/jpa/document/entity/DocumentRevisionJpaEntity.kt`

```kotlin
package com.biuea.aiwiki.adapter.persistence.jpa.document.entity

import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "document_revisions")
class DocumentRevisionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "document_id", nullable = false)
    val documentId: Long,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: DocumentStatus,
    @Column(name = "created_by", nullable = false)
    val createdBy: Long,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    fun toDomain(): DocumentRevision =
        DocumentRevision(
            id = id,
            documentId = documentId,
            title = title,
            content = content,
            status = status,
            createdBy = createdBy,
            createdAt = createdAt,
        )

    companion object {
        fun from(revision: DocumentRevision): DocumentRevisionJpaEntity =
            DocumentRevisionJpaEntity(
                id = revision.id,
                documentId = revision.documentId,
                title = revision.title,
                content = revision.content,
                status = revision.status,
                createdBy = revision.createdBy,
                createdAt = revision.createdAt ?: Instant.now(),
            )
    }
}
```

#### `adapters/persistence-jpa/src/main/kotlin/com/biuea/aiwiki/adapter/persistence/jpa/document/DocumentJpaRepository.kt`

```kotlin
package com.biuea.aiwiki.adapter.persistence.jpa.document

import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentJpaRepository : JpaRepository<DocumentJpaEntity, Long> {
    fun findByCreatedByAndStatus(createdBy: Long, status: String): List<DocumentJpaEntity>
}
```

#### `adapters/persistence-jpa/src/main/kotlin/com/biuea/aiwiki/adapter/persistence/jpa/document/DocumentRevisionJpaRepository.kt`

```kotlin
package com.biuea.aiwiki.adapter.persistence.jpa.document

import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentRevisionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRevisionJpaRepository : JpaRepository<DocumentRevisionJpaEntity, Long> {
    fun findByDocumentIdOrderByCreatedAtDesc(documentId: Long): List<DocumentRevisionJpaEntity>
}
```

#### `adapters/persistence-jpa/src/main/kotlin/com/biuea/aiwiki/adapter/persistence/jpa/document/DocumentPersistenceAdapter.kt`

```kotlin
package com.biuea.aiwiki.adapter.persistence.jpa.document

import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentJpaEntity
import com.biuea.aiwiki.adapter.persistence.jpa.document.entity.DocumentRevisionJpaEntity
import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import com.biuea.aiwiki.domain.document.port.DocumentRevisionRepository
import org.springframework.stereotype.Component

@Component
class DocumentPersistenceAdapter(
    private val documentJpaRepository: DocumentJpaRepository,
    private val documentRevisionJpaRepository: DocumentRevisionJpaRepository,
) : DocumentRepository, DocumentRevisionRepository {
    override fun save(document: Document): Document =
        documentJpaRepository.save(DocumentJpaEntity.from(document)).toDomain()

    override fun findById(id: Long): Document? =
        documentJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun searchActiveOwnedByUser(query: String, userId: Long): List<Document> =
        documentJpaRepository.findByCreatedByAndStatus(userId, DocumentStatus.ACTIVE.name)
            .map { it.toDomain() }
            .filter {
                val normalized = query.trim()
                normalized.isBlank() ||
                    it.title.contains(normalized, ignoreCase = true) ||
                    it.content.contains(normalized, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(normalized, ignoreCase = true) }
            }

    override fun save(revision: DocumentRevision): DocumentRevision =
        documentRevisionJpaRepository.save(DocumentRevisionJpaEntity.from(revision)).toDomain()

    override fun findByDocumentId(documentId: Long): List<DocumentRevision> =
        documentRevisionJpaRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
            .map { it.toDomain() }
}
```

---

### NAW-BE-004: 문서 CRUD UseCase

**모듈**: `core/application`

#### `core/application/src/main/kotlin/com/biuea/aiwiki/application/document/DocumentCommandService.kt`

```kotlin
package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import com.biuea.aiwiki.domain.document.port.DocumentRevisionRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DocumentCommandService(
    private val documentRepository: DocumentRepository,
    private val documentRevisionRepository: DocumentRevisionRepository,
) {
    fun createDraft(command: CreateDraftCommand): Document {
        val saved = documentRepository.save(
            Document(
                title = command.title,
                content = command.content,
                tags = command.tags,
                createdBy = command.userId,
                updatedBy = command.userId,
            )
        )
        return saved
    }

    fun requestAnalysis(documentId: Long): Document {
        val current = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }
        current.requestAnalysis()
        val updated = documentRepository.save(current)
        saveRevision(updated)
        return updated
    }

    fun activate(documentId: Long, userId: Long): Document {
        val current = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }
        current.activate(userId)
        val updated = documentRepository.save(current)
        saveRevision(updated)
        return updated
    }

    fun updateDraft(command: UpdateDraftCommand): Document {
        val current = requireNotNull(documentRepository.findById(command.documentId)) { "문서를 찾을 수 없습니다." }
        if (current.updatedAt != command.expectedUpdatedAt) {
            throw OptimisticLockException("문서가 이미 변경되었습니다. 최신 상태를 다시 조회하세요.")
        }
        current.updateDraft(
            title = command.title,
            content = command.content,
            tags = command.tags,
            userId = command.userId,
        )
        val updated = documentRepository.save(current)
        if (updated.status == DocumentStatus.ACTIVE) {
            saveRevision(updated)
        }
        return updated
    }

    private fun saveRevision(updated: Document) {
        documentRevisionRepository.save(
            DocumentRevision(
                documentId = requireNotNull(updated.id),
                title = updated.title,
                content = updated.content,
                status = updated.status,
                createdBy = updated.updatedBy,
            )
        )
    }
}

data class CreateDraftCommand(
    val title: String,
    val content: String,
    val tags: List<String>,
    val userId: Long,
)

data class UpdateDraftCommand(
    val documentId: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val userId: Long,
    val expectedUpdatedAt: Instant,
)

class OptimisticLockException(message: String) : RuntimeException(message)
```

#### `core/application/src/main/kotlin/com/biuea/aiwiki/application/document/DocumentQueryService.kt`

```kotlin
package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import org.springframework.stereotype.Service

@Service
class DocumentQueryService(
    private val documentRepository: DocumentRepository,
) {
    fun getDocument(documentId: Long): Document =
        requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }

    fun searchActiveDocuments(userId: Long, query: String): List<Document> =
        documentRepository.searchActiveOwnedByUser(query, userId)
            .filter { it.status == DocumentStatus.ACTIVE }
}
```

---

### NAW-BE-005: AI 파이프라인 비동기 실행

**모듈**: `core/application`

#### `core/application/src/main/kotlin/com/biuea/aiwiki/application/document/AiPipelineService.kt`

```kotlin
package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.AiStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AiPipelineService(
    private val documentRepository: DocumentRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Async
    fun processDocument(documentId: Long) {
        val document = requireNotNull(documentRepository.findById(documentId)) { "문서를 찾을 수 없습니다." }

        try {
            // PENDING → PROCESSING
            document.startProcessing()
            documentRepository.save(document)
            eventPublisher.publishEvent(AiStatusChangedEvent(documentId, AiStatus.PROCESSING))

            // Simulate AI processing (1 second delay)
            Thread.sleep(1000)

            // PROCESSING → COMPLETED with stub data
            document.completeAnalysis(
                summary = "AI generated summary",
                tags = listOf("tag1", "tag2"),
            )
            documentRepository.save(document)
            eventPublisher.publishEvent(AiStatusChangedEvent(documentId, AiStatus.COMPLETED))
        } catch (e: Exception) {
            document.failAnalysis()
            documentRepository.save(document)
            eventPublisher.publishEvent(AiStatusChangedEvent(documentId, AiStatus.FAILED))
        }
    }
}

data class AiStatusChangedEvent(
    val documentId: Long,
    val status: AiStatus,
)
```

---

### NAW-BE-006: Mock AI Adapter

**모듈**: `adapters/ai` (신규)

#### `adapters/ai/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation("org.springframework:spring-context:6.1.12")
}
```

#### `adapters/ai/src/main/kotlin/com/biuea/aiwiki/adapter/ai/MockSummaryAdapter.kt`

```kotlin
package com.biuea.aiwiki.adapter.ai

import com.biuea.aiwiki.domain.document.port.SummaryPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("mock", "default")
class MockSummaryAdapter : SummaryPort {
    override fun summarize(content: String): String =
        "AI generated summary for: ${content.take(50)}..."
}
```

#### `adapters/ai/src/main/kotlin/com/biuea/aiwiki/adapter/ai/MockTaggerAdapter.kt`

```kotlin
package com.biuea.aiwiki.adapter.ai

import com.biuea.aiwiki.domain.document.port.TaggerPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("mock", "default")
class MockTaggerAdapter : TaggerPort {
    override fun generateTags(content: String): List<String> =
        listOf("tag1", "tag2", "auto-generated")
}
```

#### `adapters/ai/src/main/kotlin/com/biuea/aiwiki/adapter/ai/MockEmbeddingAdapter.kt`

```kotlin
package com.biuea.aiwiki.adapter.ai

import com.biuea.aiwiki.domain.document.port.EmbeddingPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("mock", "default")
class MockEmbeddingAdapter : EmbeddingPort {
    override fun embed(content: String): List<Float> =
        List(768) { 0.0f }
}
```

---

### NAW-BE-007: SSE 엔드포인트

**모듈**: `apps/api`

> SSE 기능은 `DocumentController`에 통합되어 있으며, 아래 코드 참조.

- `SseEmitter` 관리: `ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>`
- `@EventListener onAiStatusChanged()`: `AiStatusChangedEvent` 수신 시 해당 documentId의 모든 emitter에 push
- Timeout: 60초
- COMPLETED/FAILED 시 `emitter.complete()` 호출 후 정리

---

### NAW-BE-008: REST Controller + Security

**모듈**: `apps/api`

#### `apps/api/src/main/kotlin/com/biuea/aiwiki/api/document/DocumentController.kt`

```kotlin
package com.biuea.aiwiki.api.document

import com.biuea.aiwiki.application.document.AiPipelineService
import com.biuea.aiwiki.application.document.AiStatusChangedEvent
import com.biuea.aiwiki.application.document.CreateDraftCommand
import com.biuea.aiwiki.application.document.DocumentCommandService
import com.biuea.aiwiki.application.document.DocumentQueryService
import com.biuea.aiwiki.application.document.UpdateDraftCommand
import jakarta.validation.constraints.NotBlank
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Validated
@RestController
@RequestMapping("/api/v1/documents")
class DocumentController(
    private val documentCommandService: DocumentCommandService,
    private val documentQueryService: DocumentQueryService,
    private val aiPipelineService: AiPipelineService,
) {
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    @PostMapping
    fun createDraft(@RequestBody request: CreateDraftRequest): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.createDraft(
            CreateDraftCommand(
                title = request.title,
                content = request.content,
                tags = request.tags,
                userId = request.userId,
            )
        )
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PostMapping("/{documentId}/analyze")
    fun analyze(@PathVariable documentId: Long): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.requestAnalysis(documentId)
        aiPipelineService.processDocument(documentId)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PostMapping("/{documentId}/activate")
    fun activate(
        @PathVariable documentId: Long,
        @RequestParam(defaultValue = "1") userId: Long,
    ): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.activate(documentId, userId)
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @PatchMapping("/{documentId}")
    fun updateDraft(
        @PathVariable documentId: Long,
        @RequestBody request: UpdateDraftRequest,
    ): ResponseEntity<DocumentResponse> {
        val document = documentCommandService.updateDraft(
            UpdateDraftCommand(
                documentId = documentId,
                title = request.title,
                content = request.content,
                tags = request.tags,
                userId = request.userId,
                expectedUpdatedAt = request.expectedUpdatedAt,
            )
        )
        return ResponseEntity.ok(DocumentResponse.from(document))
    }

    @GetMapping("/health")
    fun health(@RequestParam(required = false) q: String?): ResponseEntity<Map<String, String?>> =
        ResponseEntity.ok(mapOf("service" to "ai-wiki-api", "query" to q))

    @GetMapping
    fun search(
        @RequestParam(defaultValue = "1") userId: Long,
        @RequestParam(defaultValue = "") q: String,
    ): ResponseEntity<List<DocumentResponse>> =
        ResponseEntity.ok(documentQueryService.searchActiveDocuments(userId, q).map(DocumentResponse::from))

    @GetMapping("/{documentId}")
    fun getDocument(@PathVariable documentId: Long): ResponseEntity<DocumentResponse> =
        ResponseEntity.ok(DocumentResponse.from(documentQueryService.getDocument(documentId)))

    @GetMapping("/{documentId}/ai-status/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAiStatus(@PathVariable documentId: Long): SseEmitter {
        val emitter = SseEmitter(60_000L)
        val document = documentQueryService.getDocument(documentId)

        emitter.send(SseEmitter.event().name("ai-status").data(mapOf("status" to document.aiStatus.name)))

        emitters.computeIfAbsent(documentId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion { removeEmitter(documentId, emitter) }
        emitter.onTimeout { removeEmitter(documentId, emitter) }
        emitter.onError { removeEmitter(documentId, emitter) }

        return emitter
    }

    @EventListener
    fun onAiStatusChanged(event: AiStatusChangedEvent) {
        val documentEmitters = emitters[event.documentId] ?: return
        val deadEmitters = mutableListOf<SseEmitter>()

        for (emitter in documentEmitters) {
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("ai-status")
                        .data(mapOf("status" to event.status.name))
                )
                if (event.status.name == "COMPLETED" || event.status.name == "FAILED") {
                    emitter.complete()
                    deadEmitters.add(emitter)
                }
            } catch (_: Exception) {
                deadEmitters.add(emitter)
            }
        }

        documentEmitters.removeAll(deadEmitters.toSet())
        if (documentEmitters.isEmpty()) {
            emitters.remove(event.documentId)
        }
    }

    private fun removeEmitter(documentId: Long, emitter: SseEmitter) {
        emitters[documentId]?.remove(emitter)
        if (emitters[documentId]?.isEmpty() == true) {
            emitters.remove(documentId)
        }
    }
}

data class CreateDraftRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    val tags: List<String> = emptyList(),
    val userId: Long = 1L,
)

data class UpdateDraftRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    val tags: List<String> = emptyList(),
    val userId: Long = 1L,
    val expectedUpdatedAt: Instant,
)

data class DocumentResponse(
    val id: Long?,
    val title: String,
    val status: String,
    val aiStatus: String,
    val excerpt: String,
    val tags: List<String>,
    val updatedAt: String,
) {
    companion object {
        fun from(document: com.biuea.aiwiki.domain.document.model.Document): DocumentResponse =
            DocumentResponse(
                id = document.id,
                title = document.title,
                status = document.status.name,
                aiStatus = document.aiStatus.name,
                excerpt = document.content.take(100),
                tags = document.tags,
                updatedAt = (document.updatedAt ?: document.createdAt)?.toString() ?: "",
            )
    }
}
```

#### `apps/api/src/main/kotlin/com/biuea/aiwiki/api/config/SecurityConfig.kt`

```kotlin
package com.biuea.aiwiki.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .headers { it.frameOptions { fo -> fo.disable() } }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
}
```

#### `apps/api/src/main/kotlin/com/biuea/aiwiki/api/config/AsyncConfig.kt`

```kotlin
package com.biuea.aiwiki.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class AsyncConfig
```

---

### NAW-BE-009: 문서 검색 기능

**모듈**: `core/application`, `adapters/persistence-jpa`

> 현재 `DocumentQueryService.searchActiveDocuments()` + `DocumentPersistenceAdapter.searchActiveOwnedByUser()`에서 구현 완료.
> 인메모리 필터링 방식. 운영 환경에서는 PostgreSQL Full-text search (`tsvector`) 적용 예정.

---

### NAW-BE-010: Revision 자동 생성

**모듈**: `core/application`

> `DocumentCommandService.updateDraft()`에서 ACTIVE 문서 수정 시 `saveRevision()` 호출로 자동 생성.
> `DocumentCommandService.activate()` 및 `requestAnalysis()`에서도 Revision 생성.

---

## 설정 파일

### `apps/api/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:aiwiki;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  security:
    enabled: false
```

### `apps/api/build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":core:application"))
    implementation(project(":core:domain"))
    implementation(project(":adapters:persistence-jpa"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
}
```

---

## 빌드 검증

```bash
cd /Users/biuea/feature/flag_project/ai-orchestrator-lab/be
./gradlew :apps:api:compileKotlin
# BUILD SUCCESSFUL
```

---

## FE 연동을 위한 DocumentResponse 필드 매핑

FE `DocumentCard` 타입과의 호환성:

| FE 필드 | BE DocumentResponse 필드 | 매핑 |
|---------|------------------------|------|
| `id` | `id` | `document.id` |
| `title` | `title` | `document.title` |
| `status` | `status` | `document.status.name` |
| `aiStatus` | `aiStatus` | `document.aiStatus.name` |
| `excerpt` | `excerpt` | `document.content.take(100)` |
| `tags` | `tags` | `document.tags` |
| `updatedAt` | `updatedAt` | `document.updatedAt?.toString()` |
