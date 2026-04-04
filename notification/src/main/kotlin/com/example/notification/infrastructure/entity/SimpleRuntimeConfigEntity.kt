package com.example.notification.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "simple_runtime_config")
class SimpleRuntimeConfigEntity(
    @Id
    @Column(nullable = false)
    val key: String,

    @Column(nullable = false)
    var value: String,
)
