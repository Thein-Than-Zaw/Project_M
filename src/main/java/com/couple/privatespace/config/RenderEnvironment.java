package com.couple.privatespace.config;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** HOST-01: only the Render profile maps Neon URI credentials to JDBC settings. */
public final class RenderEnvironment {
    private RenderEnvironment() { }
    public static Map<String,Object> properties(org.springframework.core.env.Environment environment){
        var values=new HashMap<String,String>();
        for(String key:List.of("DATABASE_URL_UNPOOLED","PORT","APP_PUBLIC_ORIGIN","RENDER_EXTERNAL_URL")){
            String value=environment.getProperty(key);if(value!=null)values.put(key,value);
        }
        return properties(values);
    }
    public static Map<String,Object> properties(Map<String,String> env){
        try{
            URI uri=URI.create(Objects.requireNonNull(env.get("DATABASE_URL_UNPOOLED")));
            String raw=uri.getRawUserInfo();
            if(!Set.of("postgres","postgresql").contains(uri.getScheme())||uri.getHost()==null||uri.getHost().contains("-pooler.")||raw==null||!raw.contains(":"))throw new IllegalArgumentException();
            var query=new HashMap<String,String>();
            for(String part:Optional.ofNullable(uri.getRawQuery()).orElse("").split("&")){
                String[] pair=part.split("=",2);if(pair.length==2)query.put(pair[0],pair[1]);
            }
            if(!Set.of("require","verify-ca","verify-full").contains(query.get("sslmode")))throw new IllegalArgumentException();
            String[] credentials=raw.split(":",2);
            if(credentials[0].isBlank()||credentials[1].isBlank()||uri.getRawPath()==null||uri.getRawPath().length()<2)throw new IllegalArgumentException();
            String port=env.getOrDefault("PORT","10000");
            int number=Integer.parseInt(port);if(number<1||number>65535)throw new IllegalArgumentException();
            String jdbc="jdbc:postgresql://"+uri.getHost()+(uri.getPort()<0?"":":"+uri.getPort())+uri.getRawPath()+"?sslmode="+query.get("sslmode");
            String binding=query.getOrDefault("channelBinding",query.get("channel_binding"));
            if(binding!=null){if(!Set.of("disable","prefer","require").contains(binding))throw new IllegalArgumentException();jdbc+="&channelBinding="+binding;}
            String origin=env.getOrDefault("APP_PUBLIC_ORIGIN",env.get("RENDER_EXTERNAL_URL"));
            URI publicUri=URI.create(Objects.requireNonNull(origin));
            if(!"https".equals(publicUri.getScheme())||publicUri.getHost()==null||publicUri.getUserInfo()!=null||publicUri.getQuery()!=null||publicUri.getFragment()!=null||!(publicUri.getPath().isEmpty()))throw new IllegalArgumentException();
            return Map.of("DB_URL",jdbc,"DB_USERNAME",decode(credentials[0]),"DB_PASSWORD",decode(credentials[1]),"server.port",port,"app.websocket.allowed-origins",origin);
        }catch(Exception error){throw new IllegalStateException("Render requires a valid port, HTTPS public origin and TLS-enabled DATABASE_URL_UNPOOLED; credentials must remain private.");}
    }
    private static String decode(String value){return URLDecoder.decode(value.replace("+","%2B"),StandardCharsets.UTF_8);}
}
