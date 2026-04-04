package com.example.notification.infrastructure.repository

import com.example.notification.infrastructure.entity.SimpleRuntimeConfigEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SimpleRuntimeConfigRepository : JpaRepository<SimpleRuntimeConfigEntity, String>
