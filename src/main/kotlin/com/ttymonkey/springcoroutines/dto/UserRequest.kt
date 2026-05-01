package com.ttymonkey.springcoroutines.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UserRequest(
    val email: String,
    val name: String,
    val age: Int,
    @param:JsonProperty("company_id") val companyId: Int,
)
