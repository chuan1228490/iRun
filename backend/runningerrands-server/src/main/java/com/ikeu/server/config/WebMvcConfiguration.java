package com.ikeu.server.config;

import com.ikeu.common.json.JacksonObjectMapper;
import com.ikeu.server.interceptor.JwtTokenAdminInterceptor;
import com.ikeu.server.interceptor.JwtTokenUserInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                        .title("小i跑腿接口文档")
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
     * 配置跨域资源共享（测试环境 ngrok 内网穿透需要）。
     * 鉴权使用自定义请求头（token / authentication），不依赖 Cookie，
     * 因此 credentials=false + 通配 origin 是安全的。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    /**
     * 配置静态资源映射，暴露接口文档页面和 WebJar 资源，
     * 并为 Vue Router History 模式提供 SPA fallback。
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/imgs/**")
                .addResourceLocations("classpath:/static/imgs/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // SPA fallback: 非 API / 非文件路径 → index.html（Controller 未匹配时生效）
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        return location.createRelative("index.html");
                    }
                });
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

    /** 注入安全响应头：CSP, HSTS, X-Frame-Options, X-Content-Type-Options */
    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter((request, response, chain) -> {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("X-Frame-Options", "DENY");
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            res.setHeader("Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; " +
                    "font-src 'self'; connect-src 'self' https: wss:;");
            chain.doFilter(request, response);
        });
        bean.setOrder(1);
        return bean;
    }
}
