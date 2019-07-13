package com.marcosbarbero.security.oauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class Oauth2ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oauth2ClientApplication.class, args);
    }

    @Bean
    public OAuth2RestTemplate oAuth2RestTemplate(final OAuth2ProtectedResourceDetails details) {
        return new OAuth2RestTemplate(details);
    }

    @RestController
    class ProfileController {

        private final OAuth2RestTemplate oAuth2RestTemplate;

        ProfileController(OAuth2RestTemplate oAuth2RestTemplate) {
            this.oAuth2RestTemplate = oAuth2RestTemplate;
        }

        @GetMapping("/protected")
        public ResponseEntity<String> me() {
            return oAuth2RestTemplate.getForEntity("http://localhost:9001/profile/protected", String.class);
        }
    }

}
