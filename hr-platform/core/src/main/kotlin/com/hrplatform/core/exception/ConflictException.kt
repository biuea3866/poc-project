package com.hrplatform.core.exception

class ConflictException(
    errorCode: String,
    message: String,
) : BusinessException(errorCode, message)
