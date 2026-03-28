package pro.api4.jsonapi4j.sampleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(SpringJsonApi4jConfig.class)
@SpringBootApplication
public class SpringJsonapi4jSampleApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringJsonapi4jSampleApp.class, args);
    }

}
