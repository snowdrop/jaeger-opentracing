package org.jboss.snowdrop;

import java.util.Collections;
import java.util.regex.Pattern;

import feign.Logger;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.opentracing.TracingClient;
import feign.opentracing.hystrix.TracingConcurrencyStrategy;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.WebTracingConfiguration;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracingConfiguration {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TracingConfiguration.class);

    @Autowired
    public Tracer tracer;

    @Bean
    public WebTracingConfiguration webTracingConfiguration() {
        return WebTracingConfiguration.builder()
                .withSkipPattern(Pattern.compile("/api/health"))
                .build();
    }

    /**
     *
     * This is were the "magic" happens: it creates a Feign, which is a proxy interface for remote calling a
     * REST endpoint with Hystrix fallback support.
     */
    @Bean
    public HolaService holaService(Tracer tracer) {
        // bind current span to Hystrix thread
        TracingConcurrencyStrategy.register();

        return HystrixFeign.builder()
                .client(new TracingClient(new ApacheHttpClient(HttpClientBuilder.create().build()), tracer))
                .logger(new Logger.ErrorLogger()).logLevel(Logger.Level.BASIC)
                .decoder(new JacksonDecoder())
                .target(HolaService.class, "http://hola:8080/",
                        () -> Collections.singletonList("Hola response (fallback)"));
    }
}
