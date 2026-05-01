package com.ttymonkey.springcoroutines.models

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(name = "users", schema = "application")
data class User(
    @Id val id: Int? = null,
    val email: String,
    val name: String,
    val age: Int,
    val companyId: Int,
)
