package fi.metropolia.canopy.service

import fi.metropolia.canopy.dto.user.normalizedEmail
import fi.metropolia.canopy.repository.LocalCredentialRepository
import fi.metropolia.canopy.repository.UserRepository
import fi.metropolia.canopy.security.AuthenticatedUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CanopyUserDetailsService(
    private val userRepository: UserRepository,
    private val localCredentialRepository: LocalCredentialRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmailAndDeletedAtIsNull(username.normalizedEmail())
            ?: throw UsernameNotFoundException("Invalid email or password")

        val credential = localCredentialRepository.findByUserUserId(user.userId)
            ?: throw UsernameNotFoundException("Invalid email or password")

        return AuthenticatedUser(
            userId = user.userId,
            email = user.email,
            passwordHash = credential.passwordHash,
            role = user.role,
        )
    }
}
