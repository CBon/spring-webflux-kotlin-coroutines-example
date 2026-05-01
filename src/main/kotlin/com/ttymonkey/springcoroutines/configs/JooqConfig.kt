package com.ttymonkey.springcoroutines.configs

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderQuotedNames
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.r2dbc.core.DatabaseClient

@Configuration
class JooqConfig(private val connectionFactory: ConnectionFactory) {

    @Bean
    fun dslContext(databaseClient: DatabaseClient): DSLContext {
        val settings = Settings()
            .withRenderQuotedNames(RenderQuotedNames.NEVER)  // Отключаем кавычки
            .withRenderSchema(true)  // Включаем схему
            .withRenderCatalog(false)  // Отключаем каталог

        // Создаём DSLContext с явными настройками
        return DSL.using(
            TransactionAwareConnectionFactoryProxy(connectionFactory),
            SQLDialect.POSTGRES,
            settings
        )
    }
}

