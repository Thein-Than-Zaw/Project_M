package com.couple.privatespace.game;
import com.couple.privatespace.identity.AccountPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
public class GameController {
 private final GameService games;public GameController(GameService games){this.games=games;}
 @GetMapping("/api/games/connect4") public GameService.Page current(@AuthenticationPrincipal AccountPrincipal actor){return games.current(actor.accountId());}
 @PostMapping("/api/games/connect4/{operation}") public GameService.Page command(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable String operation,@RequestBody GameService.Command c){return games.command(actor.accountId(),operation,c);}
}
