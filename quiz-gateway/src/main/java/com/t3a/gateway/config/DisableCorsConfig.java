package com.t3a.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 禁用网关层面的CORS处理
 * 让后端微服务自己处理CORS
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class DisableCorsConfig {

    private static final Logger log = LoggerFactory.getLogger(DisableCorsConfig.class);

    /**
     * 覆盖默认的CorsWebFilter bean，提供空的CORS配置
     * 这样网关就不会添加任何CORS响应头
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info(">>> Registering custom CorsWebFilter with empty configuration to disable gateway CORS");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 不注册任何CORS配置，这样网关就不会添加CORS头
        return new CorsWebFilter(source);
    }

    /**
     * 添加一个过滤器来移除网关自动添加的CORS响应头
     * 使用最低优先级，确保在所有其他过滤器之后执行
     */
    @Bean
    public WebFilter removeCorsHeadersFilter() {
        log.info(">>> Registering WebFilter to remove gateway-added CORS headers");
        return new WebFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
                return chain.filter(exchange).doFinally(signalType -> {
                    var response = exchange.getResponse();
                    // 移除网关可能添加的CORS响应头
                    // 保留后端微服务设置的CORS头
                    var headers = response.getHeaders();

                    // 如果有重复的Access-Control-Allow-Origin头，只保留最后一个（来自后端微服务的）
                    var origins = headers.get("Access-Control-Allow-Origin");
                    if (origins != null && origins.size() > 1) {
                        log.debug(">>> Found {} Access-Control-Allow-Origin headers, removing duplicates", origins.size());
                        // 清除所有，只保留最后一个
                        String lastOrigin = origins.get(origins.size() - 1);
                        while (headers.get("Access-Control-Allow-Origin").size() > 0) {
                            headers.remove("Access-Control-Allow-Origin");
                        }
                        headers.add("Access-Control-Allow-Origin", lastOrigin);
                        log.debug(">>> Set Access-Control-Allow-Origin to: {}", lastOrigin);
                    }

                    // 同样处理其他可能的重复CORS头
                    var exposeHeaders = headers.get("Access-Control-Expose-Headers");
                    if (exposeHeaders != null && exposeHeaders.size() > 1) {
                        String lastValue = exposeHeaders.get(exposeHeaders.size() - 1);
                        while (headers.get("Access-Control-Expose-Headers").size() > 0) {
                            headers.remove("Access-Control-Expose-Headers");
                        }
                        headers.add("Access-Control-Expose-Headers", lastValue);
                    }
                });
            }
        };
    }
}
