package com.book.bookflow.common.config;

import com.book.bookflow.common.auth.AuthInterceptor;
import com.book.bookflow.common.auth.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/admin/**");
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profile/student-card/**").addResourceLocations("file:./uploads/private-deny/");
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:./uploads/");
    }
}
