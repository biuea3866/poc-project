package com.hrplatform.core.exception

class ForbiddenException(
    errorCode: String,
    message: String,
) : BusinessException(errorCode, message)
