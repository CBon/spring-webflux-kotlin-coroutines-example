package com.ttymonkey.springcoroutines.controllers

import com.ninjasquad.springmockk.MockkBean
import com.ttymonkey.springcoroutines.Application
import com.ttymonkey.springcoroutines.config.TestConfig
import com.ttymonkey.springcoroutines.models.Company
import com.ttymonkey.springcoroutines.services.CompanyService
import com.ttymonkey.springcoroutines.services.UserService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID
import kotlin.random.Random

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [Application::class]
)
@Import(TestConfig::class)
class CompanyControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @MockkBean
    private lateinit var companyService: CompanyService

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun `get company with no users by id`() {
        // given
        val id = Random.nextInt()
        val name = UUID.randomUUID().toString()
        val address = UUID.randomUUID().toString()
        val companyIdSlot = slot<Int>()

        coEvery { companyService.findCompanyById(id) } returns Company(id, name, address)
        coEvery { userService.findUsersByCompanyId(capture(companyIdSlot)) } returns flowOf()

        // when/then
        webClient.get()
            .uri("/api/companies/$id")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(id)
            .jsonPath("$.name").isEqualTo(name)
            .jsonPath("$.address").isEqualTo(address)
            .jsonPath("$.users").isEmpty

        coVerify(exactly = 1) { companyService.findCompanyById(id) }
        assert(companyIdSlot.captured == id)
    }
}
