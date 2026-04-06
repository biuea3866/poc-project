package com.closet.common.featureflag

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * Feature Toggle 엔티티.
 *
 * DB에 key-value 형태로 Feature Toggle을 저장한다.
 * 배포 없이 DB 값 변경으로 기능을 런타임 on/off 할 수 있다.
 */
@Entity
@Table(name = "feature_toggle")
class FeatureToggle(
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    val configKey: String,
    @Column(name = "config_value", nullable = false, length = 500)
    var configValue: String,
    @Column(name = "description", length = 500)
    val description: String? = null,
) : BaseEntity() {
    fun toBooleanValue(): Boolean {
        return configValue.equals("true", ignoreCase = true) || configValue == "1"
    }

    fun updateValue(newValue: String) {
        this.configValue = newValue
    }
}
