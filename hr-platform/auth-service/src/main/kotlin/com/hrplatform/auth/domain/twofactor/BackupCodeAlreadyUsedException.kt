package com.hrplatform.auth.domain.twofactor

import com.hrplatform.core.exception.BusinessException

class BackupCodeAlreadyUsedException : BusinessException(
    errorCode = "BACKUP_CODE_ALREADY_USED",
    message = "이미 사용된 백업 코드입니다",
)
