package com.closet.common.featureflag

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * SimpleRuntimeConfig JPA 레포지토리.
 */
interface SimpleRuntimeConfigRepository : JpaRepository<SimpleRuntimeConfig, Long> {

    fun findByConfigKey(configKey: String): Optional<SimpleRuntimeConfig>

    fun existsByConfigKey(configKey: String): Boolean
}
