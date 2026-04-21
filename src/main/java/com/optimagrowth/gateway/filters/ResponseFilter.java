package com.optimagrowth.gateway.filters;

import com.optimagrowth.gateway.utils.FilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Configuration
public class ResponseFilter {

    final Logger LOG = LoggerFactory.getLogger(ResponseFilter.class);

    @Autowired
    FilterUtils filterUtils;

    @Bean
    public GlobalFilter postGlobalFilter() {
        return (exchange, chain) ->
                chain.filter(exchange).then(Mono.fromRunnable(() -> {
                    HttpHeaders requestHeaders = exchange.getRequest().getHeaders();

                    // Grabs the correlation ID that was passed in to the original HTTP request
                    String correlationId = filterUtils.getCorrelationId(requestHeaders);

                    LOG.debug("Adding the correlation id to the outbound headers. {}", correlationId);

                    // Injects the correlation ID into the response
                    exchange.getResponse().getHeaders().add("correlation-id", correlationId);

                    // Logs the outgoing request URI so that you have “bookends” that show the incoming and outgoing
                    // entry of the user’s request into the gatewayserver
                    LOG.debug("Completing outgoing request for {}.", exchange.getRequest().getURI());
                }));
    }
}
