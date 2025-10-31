package com.biuea.feature_flag.domain.feature.entity

import com.biuea.feature_flag.domain.feature.entity.FeatureFlagGroup.Companion.MAX_SPECIFIC_SIZE

interface FeatureFlagAlgorithm {
    fun isEnabled(workspaceId: Int): Boolean
}

object FeatureFlagAlgorithmDecider {
    fun decide(
        algorithmOption: FeatureFlagAlgorithmOption,
        specifics: List<Int>?,
        percentage: Int?,
        absolute: Int?
    ): FeatureFlagAlgorithm {
        return when (algorithmOption) {
            FeatureFlagAlgorithmOption.SPECIFIC -> {
                if (specifics.isNullOrEmpty()) {
                    throw IllegalArgumentException("specifics is required for SPECIFIC option")
                }

                SpecificAlgorithm(specifics)
            }
            FeatureFlagAlgorithmOption.PERCENT -> {
                if (percentage == null) {
                    throw IllegalArgumentException("percentage is required for PERCENT option")
                }

                PercentAlgorithm(percentage)
            }
            FeatureFlagAlgorithmOption.ABSOLUTE -> {
                if (absolute == null) {
                    throw IllegalArgumentException("absolute is required for ABSOLUTE option")
                }

                AbsoluteAlgorithm(absolute)
            }
        }
    }
}

data class SpecificAlgorithm(
    val specifics: List<Int>
): FeatureFlagAlgorithm {
    override fun isEnabled(workspaceId: Int): Boolean {
        if (MAX_SPECIFIC_SIZE < this.specifics.size) {
            throw IllegalArgumentException("specifics size exceeds the maximum limit of $MAX_SPECIFIC_SIZE")
        }

        return this.specifics.contains(workspaceId)
    }
}

data class PercentAlgorithm(
    val percentage: Int
) : FeatureFlagAlgorithm {
    override fun isEnabled(workspaceId: Int): Boolean {
        return (workspaceId % 100) <= this.percentage
    }
}

class AbsoluteAlgorithm(
    val absolute: Int
) : FeatureFlagAlgorithm {
    override fun isEnabled(workspaceId: Int): Boolean {
        return workspaceId <= this.absolute
    }
}

enum class FeatureFlagAlgorithmOption {
    SPECIFIC,
    PERCENT,
    ABSOLUTE,
}
