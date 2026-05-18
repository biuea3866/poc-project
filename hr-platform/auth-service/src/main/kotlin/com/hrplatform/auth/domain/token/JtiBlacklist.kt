package com.hrplatform.auth.domain.token

import java.time.Duration

interface JtiBlacklist {
    fun add(jti: String, ttl: Duration)
    fun addAll(jtis: List<String>, ttl: Duration)
    fun contains(jti: String): Boolean
}
