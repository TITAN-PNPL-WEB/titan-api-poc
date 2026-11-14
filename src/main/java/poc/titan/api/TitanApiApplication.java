package poc.titan.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TitanApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TitanApiApplication.class, args);
    }
}