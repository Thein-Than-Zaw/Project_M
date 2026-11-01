package com.couple.privatespace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PrivateSpaceApplication {
    public static void main(String[] args) {
        var application=new SpringApplication(PrivateSpaceApplication.class);
        application.addInitializers(context->{
            if(context.getEnvironment().acceptsProfiles(org.springframework.core.env.Profiles.of("render"))){
                context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
                    "renderNeon",com.couple.privatespace.config.RenderEnvironment.properties(context.getEnvironment())));
            }
        });
        application.run(args);
    }
}
