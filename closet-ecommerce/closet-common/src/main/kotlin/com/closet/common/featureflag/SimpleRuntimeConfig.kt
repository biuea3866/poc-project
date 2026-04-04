package com.closet.common.featureflag

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 런타임 설정 엔티티.
 *
 * DB에 key-value 형태로 런타임 설정을 저장한다.
 * Feature Flag를 포함한 다양한 런타임 설정에 사용된다.
 */
@Entity
@Table(name = "simple_runtime_config")
class SimpleRuntimeConfig(

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
