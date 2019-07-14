package com.marcosbarbero.security.oauth;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@EnableConfigurationProperties(Oauth2ClientApplication.AppProperties.class)
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class Oauth2ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oauth2ClientApplication.class, args);
    }

    // Using Spring Boot AutoConfiguration
    @Bean
    @Qualifier("autoconfig")
    public OAuth2RestTemplate oAuth2RestTemplate(final OAuth2ProtectedResourceDetails details) {
        return new OAuth2RestTemplate(details);
    }

    // Custom OAuth2 binding
    @ConfigurationProperties(prefix = "app")
    static class AppProperties implements BeanFactoryAware, InitializingBean {
        private BeanFactory beanFactory;

        private Map<String, ClientCredentialsResourceDetails> oauth2 = new HashMap<>();

        public Map<String, ClientCredentialsResourceDetails> getOauth2() {
            return oauth2;
        }

        public void setOauth2(Map<String, ClientCredentialsResourceDetails> oauth2) {
            this.oauth2 = oauth2;
        }

        /**
         * Callback that supplies the owning factory to a bean instance.
         * <p>Invoked after the population of normal bean properties
         * but before an initialization callback such as
         * {@link InitializingBean#afterPropertiesSet()} or a custom init-method.
         * @param beanFactory owning BeanFactory (never {@code null}).
         * The bean can immediately call methods on the factory.
         * @throws BeansException in case of initialization errors
         * @see BeanInitializationException
         */
        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        /**
         * Invoked by the containing {@code BeanFactory} after it has set all bean properties
         * and satisfied {@link BeanFactoryAware}, {@code ApplicationContextAware} etc.
         * <p>This method allows the bean instance to perform validation of its overall
         * configuration and final initialization when all bean properties have been set.
         */
        @Override
        public void afterPropertiesSet() {
            getOAuth2RestTemplates().forEach(((DefaultListableBeanFactory) beanFactory)::registerSingleton);
        }

        private Map<String, OAuth2RestTemplate> getOAuth2RestTemplates() {
            return getOauth2()
                    .entrySet()
                    .stream()
                    .collect(
                            toMap(
                                    Map.Entry::getKey,
                                    entry -> new OAuth2RestTemplate(entry.getValue())
                            )
                    );
        }

    }

    @RestController
    class ProtectedController {

        private final OAuth2RestTemplate oAuth2RestTemplate;
        private final Map<String, OAuth2RestTemplate> oAuth2RestTemplates;

        ProtectedController(@Qualifier("autoconfig") final OAuth2RestTemplate oAuth2RestTemplate,
                            final Map<String, OAuth2RestTemplate> oAuth2RestTemplates) {
            this.oAuth2RestTemplate = oAuth2RestTemplate;
            this.oAuth2RestTemplates = oAuth2RestTemplates;
        }

        @GetMapping("/auto-config")
        public ResponseEntity<String> secured() {
            return oAuth2RestTemplate.getForEntity("http://localhost:9001/profile/protected", String.class);
        }

        @GetMapping("/custom-config")
        public ResponseEntity<String> custom() {
            return oAuth2RestTemplates.get("serviceId").getForEntity("http://localhost:9001/profile/protected", String.class);
        }

    }

}
