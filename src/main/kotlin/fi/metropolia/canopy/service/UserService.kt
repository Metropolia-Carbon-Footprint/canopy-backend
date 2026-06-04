package fi.metropolia.canopy.service

import fi.metropolia.canopy.dto.user.CreateUserRequest
import fi.metropolia.canopy.dto.user.UpdateUserRequest
import fi.metropolia.canopy.dto.user.normalizedEmail
import fi.metropolia.canopy.dto.user.toEntity
import fi.metropolia.canopy.dto.user.toResponse
import fi.metropolia.canopy.dto.user.updateFrom
import fi.metropolia.canopy.entity.LocalCredential
import fi.metropolia.canopy.entity.User
import fi.metropolia.canopy.entity.UserRole
import fi.metropolia.canopy.exception.ConflictException
import fi.metropolia.canopy.exception.ResourceNotFoundException
import fi.metropolia.canopy.repository.LocalCredentialRepository
import fi.metropolia.canopy.repository.UserRepository
import fi.metropolia.canopy.security.RequireAdmin
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val localCredentialRepository: LocalCredentialRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @RequireAdmin
    fun getAllUsers() =
        userRepository.findAllByDeletedAtIsNullOrderByEmailAsc()
            .map(User::toResponse)

    @RequireAdmin
    fun getUserById(userId: Int) =
        findActiveUser(userId).toResponse()

    @Transactional
    @RequireAdmin
    fun createUser(request: CreateUserRequest) =
        createLocalUser(
            email = request.email,
            password = request.password,
            role = request.role,
        ).toResponse()

    @Transactional
    @RequireAdmin
    fun updateUser(userId: Int, request: UpdateUserRequest) =
        findActiveUser(userId)
            .also { user ->
                val normalizedEmail = request.email.normalizedEmail()
                if (userRepository.existsByEmailAndUserIdNot(normalizedEmail, userId)) {
                    throw ConflictException("Email is already in use")
                }
                user.updateFrom(request)
            }
            .let(userRepository::save)
            .toResponse()

    @Transactional
    @RequireAdmin
    fun deleteUser(userId: Int) {
        val user = findActiveUser(userId)
        user.deletedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    @Transactional
    fun createLocalUser(email: String, password: String, role: UserRole = UserRole.USER): User {
        val normalizedEmail = email.normalizedEmail()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw ConflictException("Email is already in use")
        }

        val user = userRepository.save(
            CreateUserRequest(
                email = normalizedEmail,
                password = password,
                role = role,
            ).toEntity(),
        )
        localCredentialRepository.save(
            LocalCredential(
                user = user,
                passwordHash = requireNotNull(passwordEncoder.encode(password)),
            ),
        )

        return user
    }

    fun findActiveUser(userId: Int): User =
        userRepository.findByUserIdAndDeletedAtIsNull(userId)
            ?: throw ResourceNotFoundException("User", userId)
}
