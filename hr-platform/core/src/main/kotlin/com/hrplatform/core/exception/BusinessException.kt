package com.hrplatform.core.exception

open class BusinessException(
    val errorCode: String,
    message: String,
) : RuntimeException(message)
