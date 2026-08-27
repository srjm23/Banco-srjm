package com.bancoprogramacao.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI bankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banco SRJM API")
                        .version("1.0.0")
                        .description("Operações de conta, depósito, saque, PIX interno e extrato.")
                        .license(new License().name("Projeto acadêmico")));
    }
}
