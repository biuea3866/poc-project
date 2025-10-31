package com.biuea.feature_flag.infrastructure.feature

import com.biuea.feature_flag.domain.feature.entity.Feature
import com.biuea.feature_flag.domain.feature.entity.FeatureFlag
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagGroup
import com.biuea.feature_flag.domain.feature.repository.FeatureFlagGroupRepository
import com.biuea.feature_flag.domain.feature.repository.FeatureFlagRepository
import com.biuea.feature_flag.infrastructure.feature.jpa.FeatureFlagGroupJpaRepository
import com.biuea.feature_flag.infrastructure.feature.jpa.FeatureFlagJpaRepository
import com.biuea.feature_flag.infrastructure.feature.jpa.toDomain
import com.biuea.feature_flag.infrastructure.feature.jpa.toEntity
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class FeatureFlagAdaptor(
    private val featureFlagJpaRepository: FeatureFlagJpaRepository,
    private val featureFlagGroupJpaRepository: FeatureFlagGroupJpaRepository,
    private val cacheManager: CacheManager
): FeatureFlagRepository, FeatureFlagGroupRepository {
    override fun save(entity: FeatureFlag): FeatureFlag {
        return featureFlagJpaRepository.save(entity.toEntity()).toDomain()
    }

    override fun getFeatureFlags(): List<FeatureFlag> {
        return featureFlagJpaRepository.findAll().map { it.toDomain() }
    }

    override fun getFeatureFlagBy(feature: Feature): FeatureFlag {
        return featureFlagJpaRepository.findByFeatureIs(feature)?.toDomain()
            ?: throw NoSuchElementException("FeatureFlag not found for feature: $feature")
    }

    override fun getFeatureFlagOrNullBy(feature: Feature): FeatureFlag? {
        return featureFlagJpaRepository.findByFeatureIs(feature)?.toDomain()
    }

    override fun save(entity: FeatureFlagGroup): FeatureFlagGroup {
        val saved = featureFlagGroupJpaRepository.save(entity.toEntity())
            .toDomain(entity.featureFlag.toEntity())

        updateCache { cachedList ->
            val mutableList = cachedList.toMutableList()
            val existingIndex = mutableList.indexOfFirst { it.id == saved.id }

            if (existingIndex >= 0) {
                mutableList[existingIndex] = saved
            } else {
                mutableList.add(saved)
            }

            mutableList
        }

        return saved
    }

    override fun getFeatureFlagGroupOrNullBy(id: Long): FeatureFlagGroup? {
        val featureFlagGroupEntity = featureFlagGroupJpaRepository.findByIdOrNull(id) ?: return null
        val featureFlagEntity = featureFlagJpaRepository.findByIdOrNull(featureFlagGroupEntity.featureFlagId)
            ?: return null
        return featureFlagGroupEntity.toDomain(featureFlagEntity)
    }

    override fun getFeatureFlagGroupOrNullBy(feature: Feature): FeatureFlagGroup? {
        val featureFlag = featureFlagJpaRepository.findByFeatureIs(feature) ?: return null
        return featureFlagGroupJpaRepository.findByFeatureFlagId(featureFlag.id)
            ?.toDomain(featureFlag)
    }

    @Cacheable(cacheNames = ["featureFlagGroups"], key = "'allGroups'")
    override fun getFeatureFlagGroups(): List<FeatureFlagGroup> {
        val featureFlags = featureFlagJpaRepository.findAll().associateBy { it.id }
        return featureFlagGroupJpaRepository.findAll().mapNotNull { entity ->
            val featureFlag = featureFlags[entity.featureFlagId] ?: return@mapNotNull null
            entity.toDomain(featureFlag)
        }
    }

    override fun delete(entity: FeatureFlagGroup) {
        featureFlagGroupJpaRepository.delete(entity.toEntity())

        updateCache { cachedList -> cachedList.filter { it.id != entity.id } }
    }

    private fun updateCache(updater: (List<FeatureFlagGroup>) -> List<FeatureFlagGroup>) {
        val cache = cacheManager.getCache("featureFlagGroups")?: return
        val cachedList = (cache.get("allGroups")?.get() as List<FeatureFlagGroup>?) ?: emptyList()
        if (cachedList.isEmpty()) {
            cache.evict("allGroups")
        } else {
            val updatedList = updater(cachedList)
            cache.put("allGroups", updatedList)
        }
    }
}
