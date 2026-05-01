import org.jooq.meta.jaxb.Property

plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    id("org.jooq.jooq-codegen-gradle") version "3.21.2"
    id("org.flywaydb.flyway") version "12.5.0"  // Добавьте Flyway плагин
}

group = "com.ttymonkey"
version = "0.0.1-SNAPSHOT"

val postgresVersion = "42.7.10"
val flywayVersion = "12.5.0"
val jooqVersion = "3.21.2"
val mockkVersion = "1.13.5"
val springmockkVersion = "4.0.2"
val testcontainersVersion = "2.0.5"

repositories {
    mavenCentral()
}

dependencies {

    // Source: https://mvnrepository.com/artifact/org.testcontainers/testcontainers-bom
    implementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    // Source: https://mvnrepository.com/artifact/io.projectreactor/reactor-bom
    implementation(platform("io.projectreactor:reactor-bom:2025.0.5"))

    // webflux
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // kotlin and coroutines support

    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("io.projectreactor.netty:reactor-netty-core")
    implementation("io.projectreactor.netty:reactor-netty-http")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // r2dbc and postgresql
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // flyway
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:${flywayVersion}")

    // jooq
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jooq:jooq-kotlin:$jooqVersion")
    implementation("org.jooq:jooq:$jooqVersion")

    // Для генерации кода из SQL файлов нужны meta-extensions
    jooqCodegen("org.jooq:jooq-meta-extensions:$jooqVersion")  // Изменено с jooqGenerator на jooqCodegen
    // PostgreSQL драйвер для генерации (хотя при DDLDatabase он не обязателен, но может пригодиться)
    jooqCodegen("org.postgresql:postgresql:$postgresVersion")

    // test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")

    // testcontainers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    options.forkOptions.jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Настройка Flyway для миграций
flyway {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/monkey"  // Замените на вашу БД
    user = "postgres"
    password = "password"
    defaultSchema = "application"
    schemas = arrayOf("application")
    locations = arrayOf("classpath:db/migration")
}

// Настройка зависимостей между задачами
tasks.named("jooqCodegen") {
    // Генерируем код только после миграции Flyway
//    dependsOn("flywayMigrate")
    // Указываем входные файлы для инкрементальной сборки
    inputs.files(fileTree("src/main/resources/db/migration"))
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("server.jar")
}

jooq {
    configuration {
        logging = org.jooq.meta.jaxb.Logging.WARN

        jdbc = null  // DDLDatabase не требует подключения к БД

        generator {
            // Указываем генератор кода для Kotlin
            name = "org.jooq.codegen.KotlinGenerator"

            database {
                // Указываем DDLDatabase для генерации из SQL файлов
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"

                properties = listOf(
                    Property().apply {
                        key = "scripts"
                        value = "src/main/resources/db/migration"
                    },
                    Property().apply {
                        key = "sort"
                        value = "flyway"
                    },
                    Property().apply {
                        key = "defaultNameCase"
                        value = "lower"
                    },
                    Property().apply {
                        key = "inputSchema"
                        value = "application"
                    },
                    Property().apply {
                        key = "outputSchema"
                        value = "application"
                    },
                    // КРИТИЧЕСКИ ВАЖНО: Отключаем кавычки при генерации
                    Property().apply {
                        key = "renderQuotedNames"
                        value = "NEVER"
                    },
                    // Заменяем кавычки на точки
                    Property().apply {
                        key = "renderNameStyle"
                        value = "AS_IS"
                    }
                )
            }

            generate {
//                jooqVersionReference =
                isPojosAsKotlinDataClasses = true
                isKotlinNotNullPojoAttributes = true
                // Добавляем настройки генерации
                isDeprecated = false
                isGeneratedAnnotation = false
            }

            target {
                packageName = "com.ttymonkey.springcoroutines.jooq"
                // ВАЖНО: используйте абсолютный путь для directory
                // https://github.com/jOOQ/jOOQ/issues/14333
                directory = "${projectDir}/build/generated-src/jooq/main"
            }

            strategy {
                name = "org.jooq.codegen.DefaultGeneratorStrategy"
            }
        }
    }
}

// ✅ Подключаем сгенерированные jOOQ-классы к исходникам Kotlin
kotlin {
    sourceSets {
        main {
            kotlin {
                srcDir("${projectDir}/build/generated-src/jooq/main")
            }
        }
    }
}

// ✅ Гарантируем, что генерация кода выполняется ДО компиляции
tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

// ✅ Для инкрементальной сборки: отслеживаем изменения в миграциях
tasks.named("jooqCodegen") {
    inputs.files(fileTree("src/main/resources/db/migration"))
    outputs.dir("${projectDir}/build/generated-src/jooq/main")
}