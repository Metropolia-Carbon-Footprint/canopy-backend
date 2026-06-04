package fi.metropolia.canopy.repository

import fi.metropolia.canopy.entity.LocalCredential
import org.springframework.data.jpa.repository.JpaRepository

interface LocalCredentialRepository : JpaRepository<LocalCredential, Int> {
    fun findByUserUserId(userId: Int): LocalCredential?
}
