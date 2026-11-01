package com.couple.privatespace.home;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** CHAT-03: actor-owned writes, same-space reads, bounded decoded images; no public uploads. */
@RestController
public class ProfileMediaController {
    private final JdbcTemplate jdbc;
    private final PairingService pairing;
    public ProfileMediaController(JdbcTemplate jdbc,PairingService pairing){this.jdbc=jdbc;this.pairing=pairing;}
    private PairingService.Space active(UUID actor){var space=pairing.current(actor);if(!space.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return space;}
    @PutMapping("/api/profile/photo") @Transactional
    public Map<String,String> upload(@AuthenticationPrincipal AccountPrincipal actor,HttpServletRequest request)throws IOException {
        active(actor.accountId());
        if(request.getContentLengthLong()>2097152)throw new ApiFailure(413,"PHOTO_TOO_LARGE","Choose a JPG or PNG photo up to 2 MB.");
        byte[] raw=request.getInputStream().readNBytes(2097153);
        if(raw.length>2097152)throw new ApiFailure(413,"PHOTO_TOO_LARGE","Choose a JPG or PNG photo up to 2 MB.");
        byte[] normalized=normalize(raw);
        long version=jdbc.queryForObject("UPDATE app_user SET avatar_jpeg=?,avatar_version=avatar_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? RETURNING avatar_version",Long.class,normalized,actor.accountId());
        return Map.of("avatarUrl","/api/profiles/"+actor.accountId()+"/photo?v="+version);
    }
    static byte[] normalize(byte[] raw)throws IOException {
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(raw))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw invalid();var reader=readers.next();
            try{reader.setInput(input);String format=reader.getFormatName();if(!format.equalsIgnoreCase("JPEG")&&!format.equalsIgnoreCase("PNG"))throw invalid();
                int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>16000000)throw invalid();
                var source=reader.read(0);int side=Math.min(w,h);var result=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);var g=result.createGraphics();
                try{g.setColor(Color.WHITE);g.fillRect(0,0,256,256);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,256,256,(w-side)/2,(h-side)/2,(w+side)/2,(h+side)/2,null);}finally{g.dispose();}
                var out=new ByteArrayOutputStream();ImageIO.write(result,"JPEG",out);return out.toByteArray();
            }finally{reader.dispose();}
        }catch(ApiFailure error){throw error;}catch(Exception error){throw invalid();}
    }
    private static ApiFailure invalid(){return new ApiFailure(400,"INVALID_PHOTO","Choose a valid JPG or PNG photo, at most 16 megapixels.");}
    @GetMapping("/api/profiles/{id}/photo")
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id){
        var space=active(actor.accountId());if(space.members().stream().noneMatch(m->m.id().equals(id)))throw new ApiFailure(404,"PHOTO_NOT_FOUND","Photo unavailable.");
        byte[] bytes=jdbc.queryForObject("SELECT avatar_jpeg FROM app_user WHERE id=?",byte[].class,id);if(bytes==null)throw new ApiFailure(404,"PHOTO_NOT_FOUND","Photo unavailable.");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).body(bytes);
    }
    public record Presence(boolean active,Instant lastActiveAt,Instant serverTime){}
    @PostMapping("/api/presence") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@AuthenticationPrincipal AccountPrincipal actor){active(actor.accountId());jdbc.update("UPDATE app_user SET last_active_at=CURRENT_TIMESTAMP WHERE id=?",actor.accountId());}
    @GetMapping("/api/presence")
    public Presence presence(@AuthenticationPrincipal AccountPrincipal actor){var space=active(actor.accountId());var peer=space.members().stream().filter(m->!m.id().equals(actor.accountId())).findFirst().orElseThrow();
        return jdbc.queryForObject("SELECT last_active_at, CURRENT_TIMESTAMP AS now FROM app_user WHERE id=?",(r,n)->{var stamp=r.getTimestamp(1);var seen=stamp==null?null:stamp.toInstant();var now=r.getTimestamp(2).toInstant();return new Presence(seen!=null&&seen.isAfter(now.minusSeconds(45)),seen,now);},peer.id());}
}
