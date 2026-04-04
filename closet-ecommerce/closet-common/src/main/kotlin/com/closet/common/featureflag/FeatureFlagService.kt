package com.closet.common.featureflag

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Feature Flag 서비스.
 *
 * SimpleRuntimeConfig(DB)에서 Feature Flag 값을 조회하여 런타임 on/off를 제어한다.
 * 배포 없이 DB 값 변경으로 즉시 반영되며, 존재하지 않는 키는 기본값(false)을 반환한다.
 */
@Service
class FeatureFlagService(
    private val simpleRuntimeConfigRepository: SimpleRuntimeConfigRepository,
) {

    /**
     * Feature Flag가 활성화되어 있는지 확인한다.
     *
     * @param featureKey Feature Flag 키
     * @return 활성화 여부 (DB에 없으면 기본값 반환)
     */
    @Transactional(readOnly = true)
    fun isEnabled(featureKey: BooleanFeatureKey): Boolean {
        val config = simpleRuntimeConfigRepository.findByConfigKey(featureKey.key).orElse(null)

        if (config == null) {
            logger.debug { "Feature Flag 미등록 - 기본값 사용: key=${featureKey.key}, default=${featureKey.defaultValue}" }
            return featureKey.defaultValue
        }

        return config.toBooleanValue()
    }

    /**
     * Feature Flag 값을 변경한다.
     *
     * @param featureKey Feature Flag 키
     * @param enabled 활성화 여부
     */
    @Transactional
    fun setEnabled(featureKey: BooleanFeatureKey, enabled: Boolean) {
        val config = simpleRuntimeConfigRepository.findByConfigKey(featureKey.key).orElse(null)

        if (config != null) {
            config.updateValue(if (enabled) "true" else "false")
            simpleRuntimeConfigRepository.save(config)
        } else {
            val newConfig = SimpleRuntimeConfig(
                configKey = featureKey.key,
                configValue = if (enabled) "true" else "false",
                description = featureKey.description,
            )
            simpleRuntimeConfigRepository.save(newConfig)
        }

        logger.info { "Feature Flag 변경: key=${featureKey.key}, enabled=$enabled" }
    }

    /**
     * 모든 Feature Flag 상태를 조회한다.
     *
     * @return Feature Flag 키 -> 활성화 여부 맵
     */
    @Transactional(readOnly = true)
    fun getAllFlags(): Map<String, Boolean> {
        return Phase2FeatureKey.entries.associate { featureKey ->
            featureKey.key to isEnabled(featureKey)
        }
    }
}
