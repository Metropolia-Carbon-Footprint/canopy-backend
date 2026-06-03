package fi.metropolia.canopy.controller

import fi.metropolia.canopy.dto.user.CreateUserRequest
import fi.metropolia.canopy.dto.user.UpdateUserRequest
import fi.metropolia.canopy.dto.user.UserResponse
import fi.metropolia.canopy.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    fun getAllUsers(): List<UserResponse> =
        userService.getAllUsers()

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: Int): UserResponse =
        userService.getUserById(userId)

    @PostMapping
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
    ): ResponseEntity<UserResponse> {
        val user = userService.createUser(request)

        return ResponseEntity
            .created(URI.create("/api/users/${user.userId}"))
            .body(user)
    }

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Int,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserResponse = userService.updateUser(userId, request)

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable userId: Int) {
        userService.deleteUser(userId)
    }
}
