package ru.practicum.ewm.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AggregatorApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApplication.class, args);
        AggregatorStarter aggregator = context.getBean(AggregatorStarter.class);

        aggregator.start();
    }
}
