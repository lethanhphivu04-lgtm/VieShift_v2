package vn.vieshift.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path chatUploadsDir = Paths.get("uploads/chat").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/chat/**")
                .addResourceLocations("file:" + chatUploadsDir.toString() + "/");
    }
}
