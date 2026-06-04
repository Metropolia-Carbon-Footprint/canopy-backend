package fi.metropolia.canopy.repository

import fi.metropolia.canopy.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Int> {
    fun findAllByDeletedAtIsNullOrderByEmailAsc(): List<User>

    fun findByUserIdAndDeletedAtIsNull(userId: Int): User?

    @Query("select u from User u where lower(u.email) = lower(:email)")
    fun findByEmail(@Param("email") email: String): User?

    @Query("select u from User u where lower(u.email) = lower(:email) and u.deletedAt is null")
    fun findByEmailAndDeletedAtIsNull(@Param("email") email: String): User?

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    fun existsByEmail(@Param("email") email: String): Boolean

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email) and u.userId <> :userId")
    fun existsByEmailAndUserIdNot(
        @Param("email") email: String,
        @Param("userId") userId: Int,
    ): Boolean
}
