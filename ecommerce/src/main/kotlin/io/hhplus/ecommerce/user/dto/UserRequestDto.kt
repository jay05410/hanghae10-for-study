package io.hhplus.ecommerce.user.dto

data class CreateUserRequest(
    val name: String,
    val email: String
)

data class UpdateUserRequest(
    val name: String?,
    val email: String?
)