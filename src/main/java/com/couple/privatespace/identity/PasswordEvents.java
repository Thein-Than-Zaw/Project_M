package com.couple.privatespace.identity;
import com.couple.privatespace.realtime.SocketRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class PasswordEvents {
 private final SocketRegistry sockets;public PasswordEvents(SocketRegistry sockets){this.sockets=sockets;}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void committed(PasswordService.Changed event){sockets.closeAccount(event.accountId());}
}
