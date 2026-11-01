# Continuation prompt for the next account

Open the existing project folder `C:\Projects\Project_M` and paste the following into a new chat when ready to resume:

```text
ဒီ Project ရဲ့ DEV-07 (Private Memories) ကို ရပ်ထားတဲ့နေရာကနေ ဆက်လုပ်ပါ။
Workspace: C:\Projects\Project_M

အရင်ဆုံး repository instructions/AGENTS.md ရှိရင် ဖတ်ပါ။ ပြီးရင်
1. docs/Couple_Private_Space_Requirements_v1.1.md ကို အပြည့်ဖတ်ပါ။
2. docs/DEV-07_Checkpoint.md (current status) နဲ့ docs/DEV-07_Pause_Checkpoint.md (historical)
3. docs/DEV-07_Working_Spec.md
4. docs/DEV-06_Checkpoint.md နဲ့ docs/CHAT-UI-01_Checkpoint.md
ကိုဖတ်ပြီး actual git diff/source/test logs နဲ့ တိုက်စစ်ပါ။ UI reference က docs/UIs.pdf ပါ။

DEV-06/CHAT-UI-01 နဲ့ partial DEV-07 ဟာ uncommitted local changes တွေပါ။ git reset/clean မလုပ်ပါနဲ့။ Real database/account နှစ်ခုကို reset/reseed မလုပ်ပါနဲ့။ Credential ကို scripts/local-env.ps1 ကနေ load လုပ်ပြီး output/chat ထဲ မထုတ်ပါနဲ့။

DEV-07 namespace upload bug ပြင်ပြီး native tests တိုးစစ်ထားပါတယ်။ docs/DEV-07_Checkpoint.md ရဲ့ final execution facts နဲ့ လက်ရှိ test logs ကိုကြည့်ပါ။ မူလ 8/4 failure ဟာ historical ဖြစ်ပါတယ်။ Browser verification နဲ့ checkpoint ထဲက remaining checks ကို actual state အတိုင်း ဆက်စစ်ပါ။ Test schema dev07_fresh_20261101 က ရှိပြီးသားဖြစ်လို့ CREATE SCHEMA ကို မျက်စိမှိတ် ပြန်မလုပ်ပါနဲ့။

လက်ခံပြီးသား rules: Memory တစ်ခုလျှင် mixed files 10 ခု၊ photo JPG/JPEG/PNG/WebP 10MB၊ video 180 seconds/100MB၊ document PDF/DOCX/XLSX/PPTX 20MB။ နှစ်ယောက်လုံး edit/delete လုပ်နိုင်မယ်။ Trash 72 hours၊ Restore၊ expiry permanent purge၊ confirmed Empty Trash မှာ trash batch နဲ့ attached files အပြီးဖျက်မယ်။ Active memories မထိရပါ။ Myanmar time သုံးပါ။ ဒါတွေကို ပြန်မမေးဘဲ အတည်ပြုထားတဲ့အတိုင်း ဆက်လုပ်ပါ။

Java/Spring Boot/Security, PostgreSQL/Flyway, STOMP, HTML/CSS/JS stack အတိုင်း ဆက်လုပ်ပါ။ DEV-07 JAR/build/runtime နဲ့ migration status ကို current checkpoint/logs အတိုင်း ပြန်စစ်ပါ။ လိုအပ်တဲ့ implementation/meaningful tests/UI verification/docs ကို အဆင့်လိုက်ပြီးအောင်လုပ်ပါ။ Requirements ကို တိတ်တဆိတ် မပြောင်းပါနဲ့။ Run command၊ local URL၊ တကယ်ရရှိတဲ့ test results နဲ့ ကျန်အချက်တွေ ပြန်ပေးပါ။
```

Use the same local folder so ignored dependencies, local database configuration and diagnostic logs remain available. The remote repository alone does not contain all current uncommitted work.
