package com.ikeu.server.config;

import com.ikeu.common.json.JacksonObjectMapper;
import com.ikeu.server.interceptor.JwtTokenAdminInterceptor;
import com.ikeu.server.interceptor.JwtTokenUserInterceptor;
import com.ikeu.server.service.impl.TaskOrderServiceImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

/**
 * Web MVC 配置，注册 JWT 拦截器、Knife4j 接口文档、静态资源映射和 JSON 消息转换器。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    private final JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册管理端和用户端的 JWT 拦截器。
     *
     * <p>管理端拦截器拦截 /admin/** 路径，排除 login、refresh、captcha、error 和文档页面。
     * 用户端拦截器拦截 /user/**、/runner/**、/task/**、/order/**、/review/**、
     * /notification/**、/transaction/**、/chat/** 路径，
     * 排除 send、login、register、refresh、wechat、common 等公开端点。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册WebMvc拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/admin/refresh",
                        "/admin/captcha",
                        "/error",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns(
                        "/user/**",
                        "/runner/**",
                        "/task/**",
                        "/order/**",
                        "/review/**",
                        "/notification/**",
                        "/transaction/**",
                        "/chat/**"
                )
                .excludePathPatterns(
                        "/user/send",
                        "/user/login/**",
                        "/user/register",
                        "/user/refresh",
                        "/user/wechat/**",
                        "/common/**",
                        "/error",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }

    /**
     * 配置 Knife4j 接口文档的 OpenAPI 元信息。
     *
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dorm2Dorm 接口文档")
                        .version("1.0")
                        .description("校园跑腿互助平台 API"));
    }

    /**
     * 配置接口文档分组，扫描 controller 包。
     *
     * @return 默认分组的 GroupedOpenApi 实例
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.ikeu.server.controller")
                .build();
    }

    /**
     * 配置静态资源映射，暴露接口文档页面和 WebJar 资源。
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 配置 JSON 消息转换器，使用自定义 JacksonObjectMapper 处理日期格式化等。
     *
     * @param converters HTTP 消息转换器列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new JacksonObjectMapper());
        converters.add(converter);
    }
}
