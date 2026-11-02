package com.couple.privatespace.realtime;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.server.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.annotation.*;
import org.springframework.security.authorization.*;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.web.socket.server.CsrfTokenHandshakeInterceptor;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
public class PairingSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final SocketRegistry sockets;
    private final String[] origins;
    public PairingSocketConfig(SocketRegistry sockets,@Value("${app.websocket.allowed-origins}") String[] origins){this.sockets=sockets;this.origins=origins;}
    @Override public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.addEndpoint("/ws").setAllowedOrigins(origins).addInterceptors(new CsrfTokenHandshakeInterceptor(),new HandshakeInterceptor(){
            public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attrs){
                if(request instanceof ServletServerHttpRequest servlet){var session=servlet.getServletRequest().getSession(false);if(session!=null){attrs.put(SocketRegistry.HTTP_SESSION,session);return true;}}
                return false;
            }
            public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception error){ }
        });
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry broker){
        broker.enableSimpleBroker("/queue");broker.setApplicationDestinationPrefixes("/app");broker.setUserDestinationPrefix("/user");
        broker.setPreservePublishOrder(true);
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration transport){
        transport.setMessageSizeLimit(32768).setSendBufferSizeLimit(131072).setSendTimeLimit(10000)
            .addDecoratorFactory(handler->new WebSocketHandlerDecorator(handler){
                @Override public void afterConnectionEstablished(WebSocketSession session)throws Exception{sockets.add(session);super.afterConnectionEstablished(session);}
                @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status)throws Exception{try{super.afterConnectionClosed(session,status);}finally{sockets.remove(session.getId());}}
            });
    }
    @Bean org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean websocketContainer(){
        var container=new org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32768);return container; // 4000 Unicode emoji require up to16KiB plus STOMP/JSON.
    }
    @Bean AuthorizationManager<Message<?>> pairingMessages(){
        return (authentication,message)->{
            var type=SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
            var id=SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
            if(type==SimpMessageType.DISCONNECT||type==SimpMessageType.UNSUBSCRIBE)return new AuthorizationDecision(true);
            if(type==SimpMessageType.CONNECT||type==SimpMessageType.HEARTBEAT)return new AuthorizationDecision(sockets.valid(id,false));
            String destination=SimpMessageHeaderAccessor.getDestination(message.getHeaders());
            if(type==SimpMessageType.SUBSCRIBE&&"/user/queue/pairing".equals(destination))return new AuthorizationDecision(sockets.valid(id,true));
            boolean chatQueue=type==SimpMessageType.SUBSCRIBE&&"/user/queue/chat".equals(destination);
            boolean chatSend=type==SimpMessageType.MESSAGE&&Set.of("/app/chat.send","/app/chat.delivered","/app/chat.read").contains(destination==null?"":destination);
            boolean callQueue=type==SimpMessageType.SUBSCRIBE&&"/user/queue/calls".equals(destination);
            boolean callSend=type==SimpMessageType.MESSAGE&&Set.of("/app/call.invite","/app/call.accept","/app/call.decline","/app/call.cancel","/app/call.end","/app/call.resume","/app/call.connected","/app/call.disconnected","/app/call.heartbeat","/app/call.offer","/app/call.answer","/app/call.ice","/app/call.media","/app/call.reaction").contains(destination==null?"":destination);
            return new AuthorizationDecision((chatQueue||chatSend||callQueue||callSend)&&sockets.validChat(id)); // No raw broker or foreign destination.
        };
    }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration){
        registration.interceptors(new ChannelInterceptor(){
            @Override public Message<?> preSend(Message<?> message,MessageChannel channel){
                var id=SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                boolean content=SimpMessageHeaderAccessor.getMessageType(message.getHeaders())==SimpMessageType.MESSAGE;
                String destination=SimpMessageHeaderAccessor.getDestination(message.getHeaders());
                boolean chat=content&&destination!=null&&(destination.startsWith("/queue/chat")||destination.startsWith("/queue/calls"));
                return id!=null&&(chat?sockets.validChat(id):sockets.valid(id,content))?message:null;
            }
        });
    }
    @Bean ServletListenerRegistrationBean<HttpSessionListener> socketSessionListener(){
        return new ServletListenerRegistrationBean<>(new HttpSessionListener(){
            @Override public void sessionDestroyed(HttpSessionEvent event){sockets.closeHttp(event.getSession().getId());}
        });
    }
}
