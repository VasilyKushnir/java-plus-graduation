package client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientImpl implements StatsClient {

    private final DiscoveryClient discoveryClient;
    private String baseUrl;

    private String getBaseUrl() {
        if (baseUrl == null) {
            RetryTemplate retryTemplate = new RetryTemplate();

            FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
            fixedBackOffPolicy.setBackOffPeriod(3000L);
            retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

            MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
            retryPolicy.setMaxAttempts(3);
            retryTemplate.setRetryPolicy(retryPolicy);

            ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());

            this.baseUrl = "http://" + instance.getHost() + ":" + instance.getPort();
        }

        return this.baseUrl;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances("stats-server")
                    .getFirst();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Ошибка обнаружения адреса сервиса статистики с id: stats-server",
                    exception
            );
        }
    }

    private RestTemplate getRestTemplate(String url) {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
        return restTemplate;
    }

    /**
     * POST /hit
     */
    @Override
    public void hit(EndpointHitDto endpointHit) {
        RestTemplate restTemplate = getRestTemplate(getBaseUrl());
        String url = UriComponentsBuilder
                .fromHttpUrl(getBaseUrl())
                .path("/hit")
                .toUriString();

        HttpEntity<EndpointHitDto> request = new HttpEntity<>(endpointHit);
        restTemplate.postForEntity(url, request, Void.class);
    }

    /**
     * GET /stats
     */
    @Override
    public List<ViewStatsDto> getStats(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            Boolean unique
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(getBaseUrl())
                .path("/stats")
                .queryParam("start", start.format(formatter).replace(" ", "%20"))
                .queryParam("end", end.format(formatter).replace(" ", "%20"));

        if (uris != null && !uris.isEmpty()) {
            uris.forEach(uri -> builder.queryParam("uris", uri));
        }

        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        URI uri = builder.build(true).toUri();
        log.info("getStats URI: {}", uri);

        RestTemplate restTemplate = getRestTemplate(getBaseUrl());

        ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        return response.getBody() != null
                ? response.getBody()
                : Collections.emptyList();
    }
}
