package fi.metropolia.canopy.security

import fi.metropolia.canopy.exception.UnauthorizedException
import org.springframework.security.core.Authentication

fun Authentication.currentUserId(): Int =
    name.toIntOrNull() ?: throw UnauthorizedException("Unauthorized")
