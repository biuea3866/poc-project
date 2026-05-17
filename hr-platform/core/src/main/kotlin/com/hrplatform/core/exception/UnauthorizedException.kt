package com.hrplatform.core.exception

class UnauthorizedException(
    errorCode: String,
    message: String,
) : BusinessException(errorCode, message)
