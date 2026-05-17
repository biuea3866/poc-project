package com.hrplatform.core.exception

class NotFoundException(
    errorCode: String,
    message: String,
) : BusinessException(errorCode, message)
