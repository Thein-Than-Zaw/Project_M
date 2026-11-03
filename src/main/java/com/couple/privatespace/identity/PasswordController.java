package com.couple.privatespace.identity;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class PasswordController {
 private final PasswordService passwords;public PasswordController(PasswordService passwords){this.passwords=passwords;}
 @PostMapping("/api/auth/change-password") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void change(@AuthenticationPrincipal AccountPrincipal actor,@RequestBody PasswordService.Change body,HttpServletRequest request,HttpServletResponse response){
  passwords.change(actor.accountId(),actor.securityRevision(),body);
  var authentication=SecurityContextHolder.getContext().getAuthentication();new SecurityContextLogoutHandler().logout(request,response,authentication);new CookieClearingLogoutHandler("JSESSIONID").logout(request,response,authentication);
 }
}
