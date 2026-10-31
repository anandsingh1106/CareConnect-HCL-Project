package com.careconnect.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html. Click "Authorize" and paste an access token from /api/auth/login. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI careConnectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareConnect EHR API")
                        .version("1.0.0")
                        .description("Patient-Provider Electronic Health Record: patients, clinical documentation, "
                                + "CPOE, medication management, patient portal, scheduling and audit.")
                        .contact(new Contact().name("CareConnect project team")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
