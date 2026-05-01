package com.ttymonkey.springcoroutines

import com.ttymonkey.springcoroutines.config.TestConfig
import com.ttymonkey.springcoroutines.dto.CompanyRequest
import com.ttymonkey.springcoroutines.dto.CompanyResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [Application::class]
)
@Import(TestConfig::class)
@Testcontainers
class IntegrationTests {

    @LocalServerPort
    private var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }
    @field:Autowired
    lateinit var databaseClient: DatabaseClient

    companion object {
        @Container
        val container: PostgreSQLContainer? = PostgreSQLContainer("postgres:16.3")
            .withDatabaseName("monkey")
            .withUsername("postgres")
            .withPassword("password")

        @JvmStatic
        @DynamicPropertySource
        fun datasourceConfig(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${container?.host}:${container?.getMappedPort(5432)}/${container?.databaseName}?schema=application"
            }
            registry.add("spring.r2dbc.username", container!!::getUsername)
            registry.add("spring.r2dbc.password", container::getPassword)

            registry.add("spring.flyway.url") {
                "jdbc:postgresql://${container.host}:${container.getMappedPort(5432)}/${container.databaseName}?schema=application"
            }
            registry.add("spring.flyway.schemas") { "application" }
            registry.add("spring.flyway.username", container::getUsername)
            registry.add("spring.flyway.password", container::getPassword)
        }
    }

    @AfterEach
    fun cleanup() {
        databaseClient.sql("TRUNCATE TABLE application.users, application.companies RESTART IDENTITY CASCADE;")
            .then()
            .subscribe()
    }

    @Test
    fun `testing if company can be created`() {
        // given
        val name = UUID.randomUUID().toString()
        val address = UUID.randomUUID().toString()

        val companyRequest = CompanyRequest(name, address)

        // when
        val createdResponse = webTestClient.post()
            .uri("/api/companies")
            .bodyValue(companyRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<CompanyResponse>()
            .returnResult()
            .responseBody

        // then
        val id = createdResponse?.id
        val companyResponse = webTestClient.get()
            .uri("/api/companies/$id")
            .exchange()
            .expectStatus().isOk
            .expectBody<CompanyResponse>()
            .returnResult()
            .responseBody

        assertThat(companyResponse).isNotNull
        assertThat(companyResponse?.id).isEqualTo(id)
        assertThat(companyResponse?.name).isEqualTo(name)
        assertThat(companyResponse?.address).isEqualTo(address)
    }
}
