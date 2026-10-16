package com.eskcti.algashop.billingscheduler.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Container PostgreSQL compartilhado por toda a JVM de teste.
 *
 * <p>O bean declara destroy method que nao para o container: com {@code destroyMethod = ""}
 * o Spring chama {@code close()} ao destruir qualquer contexto e derruba o container
 * compartilhado. {@code keepAlive()} mantem o container vivo ate o Ryuk encerra-lo.</p>
 */
@TestConfiguration
public class TestcontainerPostgreSQLConfig {

    private static final SharedPostgreSQLContainer POSTGRESQL_CONTAINER =
            new SharedPostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRESQL_CONTAINER.start();
    }

    @Bean(destroyMethod = "keepAlive")
    @ServiceConnection
    public PostgreSQLContainer<?> postgreSQLContainer() {
        if (!POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.start();
        }
        return POSTGRESQL_CONTAINER;
    }

    public static final class SharedPostgreSQLContainer
            extends PostgreSQLContainer<SharedPostgreSQLContainer> {

        SharedPostgreSQLContainer(String dockerImageName) {
            super(dockerImageName);
        }

        public void keepAlive() {
            // intencionalmente vazio: o container e compartilhado por todos os contextos
        }

    }

}
