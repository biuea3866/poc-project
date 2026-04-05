package com.closet.common.featureflag

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * FeatureToggle JPA 레포지토리.
 */
interface FeatureToggleRepository : JpaRepository<FeatureToggle, Long> {

    fun findByConfigKey(configKey: String): Optional<FeatureToggle>

    fun existsByConfigKey(configKey: String): Boolean
}
