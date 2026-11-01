package com.couple.privatespace;

import java.util.*;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** DEV-06 upgrade rehearsal: a real pre-V15 TEXT row must survive the new payload CHECK/default. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class GiftMigrationTest {
    @Autowired DataSource dataSource;
    @Test void predecessorTextSurvivesNativeFlywayUpgrade()throws Exception{
        String schema="dev06_upgrade_"+UUID.randomUUID().toString().replace("-","");
        assertTrue(schema.matches("dev06_upgrade_[a-f0-9]{32}"));
        try(var connection=dataSource.getConnection();var s=connection.createStatement()){
            try(var r=s.executeQuery("SELECT current_database()")){r.next();assertTrue(r.getString(1).endsWith("_test"));}
        }
        try{
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("13").load().migrate();
            String before;
            try(var c=dataSource.getConnection();var s=c.createStatement()){
                String original=c.getSchema();try{
                c.setSchema(schema);
                s.execute("INSERT INTO app_user(id,account_slot,display_name,email,password_hash,signup_relationship_date) VALUES ('00000000-0000-0000-0000-000000000001',1,'Upgrade fixture','upgrade@example.invalid','synthetic-not-for-login',DATE '2025-01-01')");
                s.execute("INSERT INTO couple_space(id,deployment_slot,relationship_date,timezone) VALUES ('00000000-0000-0000-0000-000000000002',1,DATE '2025-01-01','Asia/Yangon')");
                s.execute("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001',1)");
                s.execute("INSERT INTO chat_conversation(space_id,last_sequence) VALUES ('00000000-0000-0000-0000-000000000002',1)");
                s.execute("INSERT INTO chat_message(id,space_id,sequence,sender_id,client_message_id,body,delivered_at,read_at) VALUES ('00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000002',1,'00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000004','Pre-upgrade မြန်မာစာ ❤️',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
                try(var r=s.executeQuery("SELECT row(id,space_id,sequence,sender_id,client_message_id,body,created_at,delivered_at,read_at)::text FROM chat_message")){assertTrue(r.next());before=r.getString(1);}
                }finally{c.setSchema(original);}
            }
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
            try(var c=dataSource.getConnection();var s=c.createStatement()){
                String original=c.getSchema();try{
                c.setSchema(schema);
                try(var r=s.executeQuery("SELECT row(id,space_id,sequence,sender_id,client_message_id,body,created_at,delivered_at,read_at)::text,type,gift_code,gift_name,gift_asset_path FROM chat_message")){assertTrue(r.next());assertEquals(before,r.getString(1));assertEquals("TEXT",r.getString(2));assertNull(r.getString(3));assertNull(r.getString(4));assertNull(r.getString(5));assertFalse(r.next());}
                try(var r=s.executeQuery("SELECT count(*) FROM gift_catalog")){assertTrue(r.next());assertEquals(363,r.getInt(1));}
                }finally{c.setSchema(original);}
            }
        }finally{
            // Only the unique, validated, test-owned rehearsal schema is removed; no app schema is reset.
            try(var c=dataSource.getConnection();var s=c.createStatement()){s.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");}
        }
    }
}
