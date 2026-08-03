package com.freshlink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI freshLinkOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("FreshLink API")
						.version("v1")
						.description("""
								B2B fish supply-chain API connecting suppliers with cafes.

								**Authenticating:** call `POST /api/v1/auth/login`, copy the access
								token from the response, then click **Authorize** above and paste it.
								The token is short-lived; use `POST /api/v1/auth/refresh` to renew.

								**Roles:** endpoints are grouped by the role that may call them.
								A valid token for the wrong role returns 403.

								**Note on 404:** a resource that exists but belongs to another
								account also returns 404, so ids cannot be enumerated.""")
						.contact(new Contact()
								.name("Sathieskumar Sudeshkar")
								.email("sudeshkar008sk@gmail.com"))
						.license(new License().name("Proprietary")))
				// Applied globally; the auth endpoints opt out individually.
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
				.components(new Components().addSecuritySchemes(BEARER_SCHEME,
						new SecurityScheme()
								.name(BEARER_SCHEME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Access token from /api/v1/auth/login")));
	}
}
