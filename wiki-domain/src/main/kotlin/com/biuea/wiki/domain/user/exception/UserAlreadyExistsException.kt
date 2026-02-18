package com.biuea.wiki.domain.user.exception

class UserAlreadyExistsException(email: String) : RuntimeException("User already exists: $email")
