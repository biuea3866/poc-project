package com.closet.common.featureflag

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Feature Toggle 서비스.
 *
 * FeatureToggle(DB)에서 값을 조회하여 런타임 on/off를 제어한다.
 * 배포 없이 DB 값 변경으로 즉시 반영되며, 존재하지 않는 키는 기본값(false)을 반환한다.
 */
@Service
class FeatureToggleService(
    private val featureToggleRepository: FeatureToggleRepository,
) {
    /**
     * Feature Toggle이 활성화되어 있는지 확인한다.
     *
     * @param featureKey Feature Toggle 키
     * @return 활성화 여부 (DB에 없으면 기본값 반환)
     */
    @Transactional(readOnly = true)
    fun isEnabled(featureKey: FeatureKey): Boolean {
        val toggle = featureToggleRepository.findByConfigKey(featureKey.key).orElse(null)

        if (toggle == null) {
            logger.debug { "Feature Toggle 미등록 - 기본값 사용: key=${featureKey.key}, default=${featureKey.defaultValue}" }
            return featureKey.defaultValue
        }

        return toggle.toBooleanValue()
    }

    /**
     * Feature Toggle 값을 변경한다.
     *
     * @param featureKey Feature Toggle 키
     * @param enabled 활성화 여부
     */
    @Transactional
    fun setEnabled(
        featureKey: FeatureKey,
        enabled: Boolean,
    ) {
        val toggle = featureToggleRepository.findByConfigKey(featureKey.key).orElse(null)

        if (toggle != null) {
            toggle.updateValue(if (enabled) "true" else "false")
            featureToggleRepository.save(toggle)
        } else {
            val newToggle =
                FeatureToggle(
                    configKey = featureKey.key,
                    configValue = if (enabled) "true" else "false",
                    description = featureKey.description,
                )
            featureToggleRepository.save(newToggle)
        }

        logger.info { "Feature Toggle 변경: key=${featureKey.key}, enabled=$enabled" }
    }

    /**
     * 모든 Feature Toggle 상태를 조회한다.
     *
     * @return Feature Toggle 키 -> 활성화 여부 맵
     */
    @Transactional(readOnly = true)
    fun getAllToggles(): Map<String, Boolean> {
        return Phase2FeatureKey.entries.associate { featureKey ->
            featureKey.key to isEnabled(featureKey)
        }
    }
}
