package com.biuea.batch.infrastructure.batch.common

/**
 * 6개 전략 공통 상수. 각 전략의 Job 이름은 BenchmarkRunner 가 전략 선택에 사용한다.
 */
object BatchConstants {
    const val JOB_SINGLE_CHUNK = "singleChunkJob"
    const val JOB_SINGLE_TASKLET = "singleTaskletJob"
    const val JOB_ASYNC = "asyncJob"
    const val JOB_MULTI_THREAD = "multiThreadStepJob"
    const val JOB_PARALLEL = "parallelStepJob"
    const val JOB_PARTITION = "partitionJob"

    /** 파티션 매니저 step 이름 — writeCount 이중 집계(매니저+워커) 방지용 필터에 사용. */
    const val PARTITION_MANAGER_STEP = "partitionManagerStep"

    const val CHUNK_SINGLE = 1000
    const val CHUNK_PARALLEL = 500
    const val TASKLET_FLUSH_SIZE = 1000

    /** 전체 user_id 범위 스캔용 경계(user_id 는 양수). */
    const val USER_ID_FROM_ALL = 0L
    const val USER_ID_TO_ALL = Long.MAX_VALUE

    /** 병렬 전략의 워커 스레드 수 — 가용 코어 수 기준. */
    val CPU_CORES: Int = Runtime.getRuntime().availableProcessors()
}
