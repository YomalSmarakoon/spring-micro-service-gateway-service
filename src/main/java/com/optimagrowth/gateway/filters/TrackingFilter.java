package com.optimagrowth.gateway.filters;

import com.optimagrowth.gateway.utils.FilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(1)
@Component
public class TrackingFilter implements GlobalFilter { //Global filters implement the GlobalFilter interface and must override the filter() method.

    private static final Logger LOG = LoggerFactory.getLogger(TrackingFilter.class);

    @Autowired
    FilterUtils filterUtils; //Commonly used functions across your filters are encapsulated in the FilterUtils class.

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) { // Code that executes every time a request passes through the filter

        HttpHeaders requestHeaders = exchange.getRequest().getHeaders(); // Extracts the HTTP header from the request using the ServerWebExchange object passed by parameters to the filter() method

        if (isCorrelationIdPresent(requestHeaders)) {
            LOG.debug("correlation-id found in tracking filter: {}. ",
                    filterUtils.getCorrelationId(requestHeaders));

        } else {
            String correlationID = generateCorrelationId();
            exchange = filterUtils.setCorrelationId(exchange, correlationID);

            LOG.debug("correlation-id generated in tracking filter: {}.", correlationID);
        }
        return chain.filter(exchange);
    }

    // A helper method that checks if the correlation-id is present; it can also generate a correlation ID UUID value.
    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }

    // A helper method that checks if there’s a correlation ID in the request header
    private boolean isCorrelationIdPresent(HttpHeaders requestHeaders) {
        return filterUtils.getCorrelationId(requestHeaders) != null;
    }
}
