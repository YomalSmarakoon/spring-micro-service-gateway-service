package com.optimagrowth.gateway.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Order(1)
@Component
public class TrackingFilter implements GlobalFilter { //Global filters implement the GlobalFilter interface and must override the filter() method.

    private static final Logger LOG = LoggerFactory.getLogger(TrackingFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        LOG.debug("The authentication name from the token is: {}, ", getUsername(requestHeaders));

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

    private String getUsername(HttpHeaders requestHeaders) {
        String username = "";

        String authHeader = filterUtils.getAuthToken(requestHeaders);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String authToken = authHeader.replace("Bearer ", "");

            try {
                Map<String, Object> jwtBody = decodeJWT(authToken);

                Object preferredUsername = jwtBody.get("preferred_username");

                if (preferredUsername != null) {
                    username = preferredUsername.toString();
                }

            } catch (Exception e) {
                LOG.debug("Unable to decode JWT username: {}", e.getMessage());
            }
        }

        return username;
    }

    private Map<String, Object> decodeJWT(String jwtToken) throws Exception {
        String[] tokenParts = jwtToken.split("\\.");

        if (tokenParts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        String base64EncodedBody = tokenParts[1];

        byte[] decodedBody = Base64.getUrlDecoder().decode(base64EncodedBody);

        String body = new String(decodedBody, StandardCharsets.UTF_8);

        return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
        });
    }
}
