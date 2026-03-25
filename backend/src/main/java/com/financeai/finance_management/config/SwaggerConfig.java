package com.financeai.finance_management.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "JWT Bearer",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class SwaggerConfig {
  @Value("${server.url}")
  private String serverUrl;

  @Bean
  public OpenAPI apiInfo() {
    var server = new Server().url(serverUrl).description("Dynamic Server URL");

    return new OpenAPI()
        .addServersItem(server)
        .info(
            new Info()
                .title("Assign Finance Core API")
                .version("1.0.0")
                .description("AssignFinance  APIs")
                .contact(new Contact().name("Chris").email("tuanphat17edu@gmail.com")))
        .addSecurityItem(new SecurityRequirement().addList("JWT Bearer"));
  }

  @Bean
  public GroupedOpenApi groupedOpenApi() {
    return GroupedOpenApi.builder()
        .group("api-service")
        .packagesToScan("com/financeai/finance_management/controller")
        .build();
  }
}
