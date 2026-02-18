package com.biuea.wiki.domain.user.exception

class UserNotFoundException(userId: Long) : RuntimeException("User not found: $userId")
