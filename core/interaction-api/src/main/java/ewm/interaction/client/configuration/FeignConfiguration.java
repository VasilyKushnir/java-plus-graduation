package ewm.interaction.client.configuration;

import ewm.interaction.exception.NotFoundException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

    @Bean
    public ErrorDecoder eventClientErrorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) {
                return new NotFoundException(methodKey + " not found");
            }
            return new ErrorDecoder.Default().decode(methodKey, response);
        };
    }
}