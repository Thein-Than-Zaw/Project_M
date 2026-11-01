package com.couple.privatespace;

import java.util.*;
import com.couple.privatespace.config.RenderEnvironment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderEnvironmentTest {
    private Map<String,String> valid(){return new HashMap<>(Map.of("DATABASE_URL_UNPOOLED","postgresql://owner:p%40ss%3A%2Bword@ep-example.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require","PORT","10000","RENDER_EXTERNAL_URL","https://example.onrender.com"));}
    @Test void separatesSecretsAndUsesExactHttpsOrigin(){
        var properties=RenderEnvironment.properties(valid());
        assertEquals("p@ss:+word",properties.get("DB_PASSWORD"));
        assertFalse(properties.get("DB_URL").toString().contains("owner"));
        assertTrue(properties.get("DB_URL").toString().contains("channelBinding=require"));
        assertEquals("https://example.onrender.com",properties.get("app.websocket.allowed-origins"));
        assertEquals("10000",properties.get("server.port"));
    }
    @Test void rejectsInsecurePooledOrMissingConnectionWithoutLeakingInput(){
        for(String value:List.of("bad-secret-value","postgresql://owner:private@host/neondb?sslmode=disable","postgresql://owner:private@ep-example-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require")){
            var env=valid();env.put("DATABASE_URL_UNPOOLED",value);
            var error=assertThrows(IllegalStateException.class,()->RenderEnvironment.properties(env));
            assertFalse(error.getMessage().contains("private"+"@"));assertNull(error.getCause());
        }
    }
    @Test void rejectsUnboundedPortAndWildcardOrHttpOrigin(){
        for(String port:List.of("0","65536","not-a-port")){var env=valid();env.put("PORT",port);assertThrows(IllegalStateException.class,()->RenderEnvironment.properties(env));}
        for(String origin:List.of("*","http://example.com","https://example.com/path","https://example.com?query")){var env=valid();env.put("APP_PUBLIC_ORIGIN",origin);assertThrows(IllegalStateException.class,()->RenderEnvironment.properties(env));}
    }
    @Test void readsImportedSecretPropertiesWithEnvironmentOverridePrecedence(){
        var environment=new org.springframework.mock.env.MockEnvironment();
        var imported=new HashMap<String,Object>();valid().forEach(imported::put);
        environment.getPropertySources().addLast(new org.springframework.core.env.MapPropertySource("renderSecretFile",imported));
        environment.setProperty("PORT","12345");
        var properties=RenderEnvironment.properties(environment);
        assertEquals("12345",properties.get("server.port"));
        assertEquals("p@ss:+word",properties.get("DB_PASSWORD"));
    }
}
