package poc.titan.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the frontend (local dev, Vercel, and Render) to call the API from the browser.
 */
@Configuration
public class CorsConfig {

    /**
     * Enables CORS for /pnpl/** from the known frontend origins.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/pnpl/**")
                        .allowedOrigins("http://localhost:5173",
                                "https://titan-front-end-sigma.vercel.app",
                                "https://titan-api-pyts.onrender.com")
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("*");
            }
        };
    }
}