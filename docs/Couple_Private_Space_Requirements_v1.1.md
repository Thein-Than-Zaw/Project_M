# Couple Private Space
## Development Requirement Document v1.1

### 1. Document Information

Project Name: Us, always — Couple Private Space (အမည်သည် Design အတွက် ယာယီအမည်ဖြစ်သည်)

System / Module Name: Responsive Private Couple Web Application

Requirement Version: 1.1 | Document Status: Draft — Owner Decisions Incorporated

Prepared By: Codex | Requirement Owner: Thein Than Zaw

Created Date / Last Updated Date: 31 October 2026

ရည်ရွယ်ချက်: Couple နှစ်ယောက်သာ အသုံးပြုနိုင်သော Private Space တွင် စကားပြောခြင်း၊ Video Call ခေါ်ခြင်း၊ Emoji နှင့် Virtual Gift ပို့ခြင်း၊ Memories စုခြင်း၊ Relationship Date နှင့် Anniversary များ ကြည့်ရှုခြင်းတို့ ဆောင်ရွက်နိုင်ရန် ဖြစ်သည်။

ဤ Requirement သည် Software တစ်ခုလုံးအတွက်ဖြစ်ပြီး Login UI တစ်ခုတည်းအတွက် မဟုတ်ပါ။ ဤဖိုင်နှင့် အမည်တူ Word ဖိုင်သည် Requirement တစ်စောင်၏ Format နှစ်မျိုးသာ ဖြစ်သည်။

#### 1.1 Requirement authority and evidence

- [C] Confirmed: User က Couple Private Site၊ Mobile/Laptop View၊ Chat၊ Video Call၊ Emoji/Gift၊ Memories၊ Relationship Day Count နှင့် Login ကို တောင်းဆိုထားသည်။ Development အတွက် Codex၊ Java၊ Spring Boot၊ WebSocket နှင့် JavaScript ကို သုံးမည်ဟု သတ်မှတ်ထားသည်။
- [P] Proposed default: အောက်ပါအသေးစိတ် Validation၊ Security၊ Limits၊ Database၊ Hosting architecture နှင့် မတောင်းဆိုရသေးသော Supporting Features များသည် Development အတွက် အကြံပြုထားသော Default ဖြစ်သည်။ [P] ကို Client Approved အဖြစ် မယူရ။
- [C] Owner Decisions (31 October 2026): Couple တစ်တွဲ/Account နှစ်ခုသာ; Sign Up မှာ Relationship Date required; Memory ကို နှစ်ယောက်လုံး Edit/Delete နိုင်; Server-readable private access; Local development first; Site ဖွင့်ထားချိန် In-App Notification only. Section 6 တွင် Decision Register နှင့် ကျန်အသေးစိတ် Questions ကို ခွဲထားသည်။
- [Q] Open decision: Day-count label၊ Leap-day policy၊ Retention နှင့် Upload limits စသည့် မရွေးရသေးသော အသေးစိတ်များသည် Proposed Defaults ဖြစ်နေဆဲ ဖြစ်သည်။
- Source priority: User ၏ နောက်ဆုံးအတည်ပြုချက် → ဤ Requirement ၏ အတည်ပြုပြီး Version → API/DB Design → Figma UI။ UI က Business Rule ကို အလိုအလျောက် အတည်မပြုပါ။
- Figma Design: https://www.figma.com/design/cQoVyyhDpjoE0AWhuZipPQ/Untitled
- Existing prototype references: Laptop start 4:2; Mobile start 7:38; Pairing start 8:396။ ယခင်လုပ်ဆောင်မှုတွင် Prototype ချိတ်ထားပြီးဖြစ်သော်လည်း Production Functionality မဟုတ်ပါ။ Authentication UI မထည့်ရသေးပါ။ ဤစာတမ်းအတွက် Live Figma ကို ထပ်မံ Audit မလုပ်ထားပါ။
- English UI labels ကို အသုံးပြုပြီး Chat/Caption များတွင် Myanmar Unicode အပါအဝင် Unicode text လက်ခံရမည် [P]။

#### 1.2 Scope and technical direction

[C — D01] ဤ Site တွင် Couple တစ်တွဲ၊ Account နှစ်ခုသာ အသုံးပြုမည်။ Public multi-couple registration သည် Current Scope မဟုတ်ပါ။ [P] ပထမ Account အတွက် Bootstrap Token၊ ဒုတိယ Account အတွက် Partner Invite ဖြင့် Registration ကို ကန့်သတ်ရန် အကြံပြုသည်; Concurrent Registration တွင်ပါ Account limit 2 မကျော်ရ။ Disabled accounts/recovery lifecycle အသေးစိတ်သည် Q03 ဖြစ်သည်။

[P] Virtual Gift ကို Free digital gift အဖြစ် သတ်မှတ်သည်။ ငွေပေးချေခြင်း၊ ပစ္စည်းပို့ခြင်း၊ Gift Store ဝယ်ယူခြင်း မပါ။

[C — D04] Account နှစ်ခုသာ Private Content ကို ဝင်ကြည့်နိုင်ပြီး Application Server က Chat/Memory Data ကို ဖတ်/process လုပ်နိုင်သော ပုံစံကို ရွေးထားသည်။ E2EE သည် Current Scope မဟုတ်ပါ။ UI တွင် “Only you can decrypt” ဟု မဖော်ပြရ။

[C — D05/D06] Local Development ကို အရင်လုပ်မည်; Online Hosting/Provider/Budget ကို နောက်မှရွေးမည်။ Site ဖွင့်ထားချိန် In-App Notification ကိုသာ သုံးမည်; Browser Push/closed-site notification မပါ။ Password Reset Email သည် Authentication အတွက် Proposed Supporting Integration ဖြစ်နေဆဲဖြစ်ပြီး Local Mail Testing Inbox ဖြင့် စမ်းနိုင်သည် [P]။

### 2. System Process Map

Process Summary: User သည် Account ဝင်ပြီး Partner နှင့် Space ချိတ်ဆက်ကာ Private Features ကို အသုံးပြုမည်။ မချိတ်ဆက်ရသေးသူသည် Pairing Screen မှတစ်ဆင့်သာ ဆက်သွားနိုင်မည်။

Main Flow: Start → Sign Up / Login → Authentication valid? → Paired? → Create invitation / Join partner → Couple Active → Home → Chat / Gifts / Calls / Memories / Our Dates → Logout → End.

Alternative Flow: Invalid login → Error and retry; Expired invitation → Regenerate; Space full → Reject join; Session expired → Stop protected access and Login; Network lost → Reconnecting state and recover history.

Main Roles: Guest (Account မဝင်ရသေးသူ); Unpaired User (Account ရှိ၊ Partner မချိတ်ရသေးသူ); Couple Member (Space တစ်ခုတည်းရှိ Member A/B); System (Notification/Expiry Scheduler)။ Deployment Operator သည် App ထဲက Couple Member Role မဟုတ်ပါ။

Related Modules: Authentication; Couple Space; Home/Profile; Chat; Emoji/Gifts; Calls; Memories; Dates; Notifications; Privacy/Settings.

Main Statuses [P]: Account ACTIVE/DISABLED; Space WAITING/ACTIVE; Invitation ACTIVE/USED/EXPIRED/REVOKED; Message SENDING/SENT/DELIVERED/READ/FAILED; Call RINGING/CONNECTING/ACTIVE/DECLINED/MISSED/CANCELLED/ENDED/FAILED; Upload PENDING/READY/FAILED/DELETED.

### 3. Feature Status Summary

| Feature ID | Feature | Client Status | UI Status | Development Status | QA Status |
|---|---|---|---|---|---|
| F01 | Account & Authentication | Draft | Not Started | Not Ready | Not Started |
| F02 | Partner Pairing | Draft | UI Review | Not Ready | Not Started |
| F03 | Home & Profile | Draft | UI Review | Not Ready | Not Started |
| F04 | Private Chat | Draft | UI Review | Not Ready | Not Started |
| F05 | Emoji & Virtual Gifts | Draft | UI Review | Not Ready | Not Started |
| F06 | Video Call | Draft | UI Review | Not Ready | Not Started |
| F07 | Shared Memories | Draft | UI Review | Not Ready | Not Started |
| F08 | Relationship Dates | Draft | UI Review | Not Ready | Not Started |
| F09 | Notifications & Recovery | Draft | Not Started | Not Ready | Not Started |
| F10 | Privacy & Settings | Draft | Not Started | Not Ready | Not Started |

Status Note: Core feature requests နှင့် D01–D06 Owner Decisions အတည်ပြုပြီးဖြစ်သော်လည်း ကျန် Detailed Rules နှင့် Supporting UI ကို မအတည်ပြုရသေးသဖြင့် Client Approved / Ready for Development ဟု မသတ်မှတ်ထားပါ။ ဤအခြေအနေသည် Implementation လုပ်ပြီးဖြစ်ကြောင်း မဆိုလိုပါ။

### 4. Team Reading Guide

- Owner/PM: Feature တစ်ခုချင်း၏ Client Side၊ Out of Scope နှင့် Open Questions ကို စစ်ဆေးရန်။
- UI: Workflow၊ Screen List၊ Form Fields၊ Error/Empty/Loading/Success State များကို ကြည့်ရန်။
- Codex/Developer: Feature Development Side၊ Section 5.11 Technical Requirements၊ Acceptance Criteria နှင့် Open Questions ကို တွဲဖတ်ရန်။
- QA: Requirement ID ကို Test Case နှင့်ချိတ်၍ Positive၊ Negative၊ Unauthorized၊ Persistence နှင့် Reconnect Case များ စမ်းရန်။
- အောက်ပါ Feature တစ်ခုချင်းတွင် [C] နှင့် D01–D06 Decision များကို Confirmed အဖြစ် ဖတ်ရမည်။ ၎င်းတို့နှင့် မသက်ဆိုင်သော Detailed Defaults ကို [P] အဖြစ် ဖတ်ရမည်။ အတည်မပြုရသေးသော အချက်ကို အတည်ပြုပြီးဟု မပြောင်းရ။
- Form Field notation: Required = Y/N; C = Create; E = Edit; RO = Read-only; Default/Source ကို စာကြောင်းအတွင်း ဖော်ပြထားသည်။ Internal identifier နှင့် Owner/Couple ID ကို Server က သတ်မှတ်ရမည်။

### 5. Feature Requirements

## Feature 1 — F01: Account & Authentication

Feature Status: Client Draft; UI Not Started; Development Not Ready; QA Not Started.

### 1.1 Client Side
#### 1.1.1 Business Understanding

1. Feature Name / Module: Account & Authentication / Identity.

2. Purpose [C]: မိမိ Account ဖြင့် Login ဝင်ပြီး Private Space ကို အသုံးပြုနိုင်ရန်။

3. User Roles: Guest သည် Register/Login/Reset ကို သုံးနိုင်သည်။ Account Owner သည် မိမိ Password ပြောင်းနိုင်ပြီး Logout လုပ်နိုင်သည်။ အခြားသူ၏ Session ကို ကြည့်မရ။

4. Business Workflow: Sign Up (Relationship Date required) → Validate → Account create → Login → Paired ဆို Home၊ မချိတ်ရသေးဆို Pairing သို့ သွားရမည်။ Login failure တွင် အကြောင်းကြားပြီး ထပ်ဝင်နိုင်ရမည်။ Forgot Password တွင် Email ပေးပြီး Single-use Link ဖြင့် Password အသစ်ပြောင်းရမည်။ Logout ပြီး Protected Page ပြန်ဖွင့်လျှင် Login သို့ သွားရမည်။

#### 1.1.2 Scope Control

15. Out of Scope [P]: Social Login၊ Phone OTP၊ MFA၊ Native Mobile App Login မပါ။ Browser မှ Password Reset Link ကို Copy ပြသခြင်းသည် Local Development Only ဖြစ်ရမည်။ Registration scope သည် D01 အတိုင်း Couple တစ်တွဲ/Account နှစ်ခုသာ ဖြစ်ရမည်။

### 1.2 UI Side
#### 1.2.1 UI and Data Definition

5. Screen List: Login (Submit, Show/Hide Password, Forgot Password, Sign Up); Sign Up; Forgot Password; Reset Password; Change Password; Session Expired State။ Mobile/Laptop နှစ်မျိုးလုံး၊ Keyboard Navigation၊ Field Error၊ Submitting State ပါရမည်။

6. Form Fields:

- Display Name: text; Y; default blank; source User; C Y/E Y (Profile); 1–60 characters.
- Email: email; Y; blank; User; C Y/E RO in V1; normalized unique identifier.
- Relationship Date [C — D02]: LocalDate; Y at Sign Up; blank; User input; C Y/E shared date workflow after pairing; Account မဖန်တီးမီ Validate/Save ရမည်။ [P] Unpaired account တွင် pendingRelationshipDate အဖြစ်သိမ်းပြီး Space create လုပ်ချိန် Initialize လုပ်ရန်။
- Password: password; Y; blank; User; C Y/E Change Password only; 12–128 characters [P].
- Confirm Password: password; Y on registration/reset; blank; User; C Y/E Y; မသိမ်းရ။
- Reset Token: opaque string; Y for reset; source emailed URL; C RO/E RO; Database တွင် Token hash သာသိမ်းရန်။
- Current Password / New Password: password; Y on change; blank; User; C N/E Y; Password hash ကို Client ဆီ မပို့ရ။

### 1.3 Development Side
#### 1.3.1 System Control

7. Validation Rules — AUTH-01: Email syntax/uniqueness စစ်ရန်။ Relationship Date မပါလျှင် Registration ကို reject လုပ်ရမည် [C — D02]။ Valid date/today-or-past rule သည် DATE-01 အတိုင်း [P]။ Confirm Password တူရမည်။ Password ကို Trim မလုပ်ရ။ Invalid Email/Password အတွက် အတူတူ Generic Error သုံးရန်။ Reset Token သည် 30 minutes အတွင်း၊ Single-use ဖြစ်ရမည် [P]။ Login ကို Account/IP အလိုက် configurable rate limit လုပ်ပြီး 429 တွင် Retry-After ပေးရန်။

8. Business Rules — AUTH-02: Spring Security Session + HttpOnly Cookie ကို Default သုံးရန်။ Password ကို adaptive hash ဖြင့်သာ သိမ်းရန်။ Login အောင်မြင်လျှင် Session ID rotate လုပ်ရန်။ Password Reset/Change ပြီး Existing Sessions နှင့် WebSockets ပိတ်သိမ်း၍ ပြန် Login လိုအပ်စေရန်။ Password Reset request သည် Account ရှိ/မရှိ မဖော်ပြရ။

9. Permission Rules — AUTH-03: Login/Reset ကို unauthenticated အသုံးပြုနိုင်ရမည်။ Register သည် Session မလိုသော်လည်း [P] Bootstrap/Partner Invite Token နှင့် [C — D01] Account count limit ကို စစ်ပြီးမှ Create ခွင့်ပြုရမည်။ Member-only APIs/Pages/WS ကို Session မရှိလျှင် ပိတ်ရန်။ Logout သည် POST ဖြစ်ပြီး CSRF စစ်ရမည်။ Session idle expiry 30 minutes [P]၊ Activity/absolute expiry အသေးစိတ် config မှ ထိန်းရန်။

13. Error Messages: Invalid credentials — “Email or password is incorrect.”; Too many requests — “Too many attempts. Please try again later.”; Invalid reset — “This reset link has expired or is invalid.”; Session expired — “Your session has expired. Please sign in again.”

#### 1.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Reset requested → Account email သို့ Link ပို့ရန်; Message “Reset your password”; Reset/change completed → Account Owner သို့ Security Email ပို့ရန် [P]။ Sensitive password/token ကို Log မထုတ်ရ။

12. Integration Impact: SMTP/Email Provider; Trigger Reset request; Send Recipient/Template/Single-use Link; Receive Provider delivery acceptance; Failure တွင် Generic UI response ကို ထိန်းထားပြီး restricted operational log ထုတ်ရန်။ Mail job ကို အများဆုံး 3 attempts [P]၊ job key ဖြင့် duplicate enqueue တားရန်။

### 1.4 Testing Side
#### 1.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F01-01 Valid signup ကို save ပြီး Name/Email ရရှိရမည်၊ Plain Password မရှိရ။ AC-F01-02 Wrong Login သည် Session မဖန်တီးရ။ AC-F01-03 Logout/expiry/reset ပြီး REST နှင့် WS access နှစ်ခုလုံး ပိတ်ရမည်။ AC-F01-04 Used/expired reset link ကို ပယ်ချရမည်။ AC-F01-05 Browser cookie/CSRF မပါသော protected write ကို ပယ်ချရမည်။ AC-F01-06 Reset request တွင် Email ရှိ/မရှိ အတူတူ UI response ပြရမည်။ AC-F01-07 Sign Up တွင် Relationship Date မပါလျှင် Account မဖန်တီးရ၊ Field Error ပြရမည်; Valid date ပါလျှင် Account/Space initialization အတွက် Date ကို persist လုပ်ရမည်။ AC-F01-08 Third Account registration နှင့် Concurrent signup limit bypass ကို Server က ပယ်ချရမည်။

## Feature 2 — F02: Partner Pairing & Private Space

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 2.1 Client Side
#### 2.1.1 Business Understanding

1. Feature Name / Module: Partner Pairing / Couple Space.

2. Purpose [C-derived]: Couple နှစ်ယောက်ကို Space တစ်ခုထဲ ချိတ်ဆက်ပြီး မသက်ဆိုင်သူ ဝင်မရစေရန်။

3. User Roles: Unpaired User သည် Space စဖန်တီးခြင်း၊ Invite ဖန်တီးခြင်း သို့မဟုတ် Invite ဖြင့် Join နိုင်သည်။ Member နှစ်ဦးသည် Active Space တစ်ခုတည်း၏ Data ကို အသုံးပြုနိုင်သည်။

4. Business Workflow: Account Owner → Create space → Sign Up မှ Relationship Date ကို ပြန်ပြ/Timezone confirm → Invite generate → Partner Sign Up (Relationship Date required) / Login → Code submit → Space identity/Inviter name/Canonical Relationship Date ကြည့် → Confirm Join → ACTIVE။ Expired/revoked code တွင် Join ပယ်ချရမည်။ Code Regenerate လုပ်လျှင် Old Code အသုံးမရတော့ရ။

#### 2.1.2 Scope Control

15. Out of Scope: Group/Third Member၊ Automatic Re-pairing၊ Break-up and ownership transfer မပါ [P/Q03]။ Operator က DB edit လုပ်ကာ Member ပြောင်းခြင်းကို Normal Workflow အဖြစ် မသုံးရ။

### 2.2 UI Side
#### 2.2.1 UI and Data Definition

5. Screen List: Create Our Space; Invite Partner; Join with Code; Join Confirmation; Waiting for Partner; Invite Expired/Space Full; Paired Success။ Prototype ၏ Share Invite → Home တန်းသွားခြင်းကို Real Join logic ဖြင့် အစားထိုးရမည်။

6. Form Fields: Relationship Start Date — date/Y/from Sign Up pendingRelationshipDate/User-confirmed/C initialize/E F08; Couple Timezone — IANA timezone/Y/browser suggestion/User confirm/C Y/E Y; Invite Code — opaque/Y/Server-generated/C RO/E regenerate only; Join Code — string/Y/blank/User/C Y/E N; Partner Display Name — string/RO/Server/C RO/E N.

### 2.3 Development Side
#### 2.3.1 System Control

7. Validation Rules — PAIR-01: Invite code non-empty၊ active၊ unexpired ဖြစ်ရမည်။ Self-join မရ။ Already paired user ထပ် Join မရ။ Start Date သည် Couple local date ထက် နောက်မကျရ။ Registration အပါအဝင် Account အများဆုံး2 [C — D01] ကို Concurrent request များ၌ပါ Enforcement လုပ်ရန်။ Code ကို Cryptographically random၊ brute-force rate-limited ဖြစ်စေရန်။ Code ကို Storage တွင် hash ဖြင့် သိမ်းရန်။

8. Business Rules — PAIR-02: Invite expires after 24 hours [P]; တစ်ကြိမ်သုံးသာ; Membership limit 2 ကို Transaction/Lock/Constraint ဖြင့် enforcement လုပ်ရန်။ Concurrent join နှစ်ခုဝင်လာလျှင် အများဆုံး Member နှစ်ဦးပဲ ရှိရမည်။ Code ရုံဖြင့် Login မကျော်နိုင်ရ။ [P] Shared Space ၏ Canonical Relationship Date ကို Creator ၏ Sign Up date ဖြင့် Initialize လုပ်ရန်။ Partner ၏ Sign Up date မတူလျှင် Join Confirmation တွင် နှစ်ခုလုံးပြ၍ Existing Shared Date ကို explicit confirm လုပ်စေရမည်; silently overwrite မလုပ်ရ။ နောက်ပိုင်း Date change သည် F08 workflow ကို သုံးရမည်။

9. Permission Rules — PAIR-03: Server သည် authenticated user မှ Space scope ကို resolve လုပ်ရမည်။ Request body ထဲ User ID/Member Role ကို ယုံမထားရ။ Space full ဖြစ်လျှင် Invite revoke လုပ်၍ registration lock policy ကို D01 နှင့် align လုပ်ရန်။

13. Error Messages: Invalid invite — “This invitation is invalid or expired.”; Full space — “This space already has two members.”; Already paired — “You already belong to a couple space.”

#### 2.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Partner joined → Inviter သို့ In-App event “Your person has joined.”; Waiting Screen သည် reload မလုပ်ဘဲ Active ပြောင်းရမည်။

12. Integration Impact: Authentication + DB + WS; Send Invitation/Join request; Receive Space state; Commit အောင်မြင်မှ publish; Network timeout တွင် GET current membership ဖြင့် reconcile; Idempotency key ဖြင့် join repeat ကို မထပ်ဖန်တီးရ။ No external integration required.

### 2.4 Testing Side
#### 2.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F02-01 Valid logged-in partner Join ပြီး နှစ်ဦးလုံး Space တူရမည်။ AC-F02-02 Third user/used code/self join ပယ်ချရမည်။ AC-F02-03 Concurrent Join စမ်းရာ Member count 2 ထက်မပိုရ။ AC-F02-04 Waiting Screen မှ ACTIVE သို့ update ရမည်။ AC-F02-05 API/WS တွင် guessed space ID ဖြင့် Data မရရ။ AC-F02-06 Partner signup date မတူလျှင် Shared Date confirmation ပြပြီး မသိမသာ ပြောင်းမသွားရ [P]။

## Feature 3 — F03: Home Dashboard & Profile

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 3.1 Client Side
#### 3.1.1 Business Understanding

1. Feature Name / Module: Home Dashboard & Profile / Couple Home.

2. Purpose [C]: Couple ၏ နေ့ရေ၊ Partner၊ Recent Memories နှင့် Feature Shortcuts ကို တစ်နေရာတည်း ကြည့်နိုင်ရန်။

3. User Roles: Couple Member နှစ်ဦးသည် Shared Summary ကို ကြည့်နိုင်သည်။ မိမိ Profile ကိုသာ Edit နိုင်သည်။

4. Business Workflow: Login → Membership check → Home load → Days together/Upcoming date/Recent memories ပြ → Chat/Call/Add Memory/Gift သို့ ကူးရန်။ Empty memories တွင် First Memory CTA ပြရန်။ API failed လျှင် Retry action ပြပြီး Fabricated count မပြရ။

#### 3.1.2 Scope Control

15. Out of Scope: Public Profile၊ Follower၊ Like Count၊ Analytics/Business Dashboard မပါ။

### 3.2 UI Side
#### 3.2.1 UI and Data Definition

5. Screen List: Home; Edit My Profile; Empty/Loading/Error states; Mobile bottom navigation; Laptop sidebar။ Desktop main content နှင့် Mobile layout ကို Reference Design အတိုင်း responsive ဖြစ်စေရန်။

6. Form Fields: Display Name — text/Y/current/User/C F01/E Y; Avatar — image/N/initials/User/C N/E Y; Partner Name — text/RO/Server/E N; Start Date and Days Together — date/integer/RO/Server from F08; Recent Memory Title/Thumbnail/Date — RO/from F07. Profile avatar image limit 2 MiB [P].

### 3.3 Development Side
#### 3.3.1 System Control

7. Validation Rules — HOME-01: Name 1–60 characters၊ whitespace-only မရ။ Avatar JPEG/PNG/WebP only; bytes/MIME validation နှင့် private access ကို F07 rules အတိုင်း လုပ်ရန်။

8. Business Rules — HOME-02: Home counter ကို F08 date rule တစ်ခုတည်းဖြင့်တွက်ရန်။ Recent memories latest 3 [P]။ Partner name ကို User က မိမိ Profile မှ ပြောင်းလဲခြင်းဖြင့်သာ update လုပ်နိုင်ရမည်။

9. Permission Rules — HOME-03: Unpaired user → Pairing; Active member → own space Home; Account Owner သည် မိမိ Name/Avatar Edit နိုင်ပြီး Partner Account မပြင်နိုင်ရ။

13. Error Messages: Summary failed — “We could not load your space. Try again.”; Invalid avatar — “Choose a supported image under 2 MiB.”

#### 3.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Profile updated → Partner connected session သို့ internal profile.updated event; Toast “Profile updated.” ကို User ထံ ပြရန်။

12. Integration Impact: F02/F07/F08 summaries; Couple scope verified query; fail ဖြစ်လျှင် independent card error ပြနိုင်သည်; avatar upload failure တွင် Old avatar ကို ထိန်းထားရန်; duplicate submission ကို User profile version ဖြင့် ကာကွယ်ရန်။ No external integration required.

### 3.4 Testing Side
#### 3.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F03-01 Reload ပြီး Name/Avatar persist ဖြစ်ရမည်။ AC-F03-02 Relationship date တူလျှင် Home နှင့် Our Dates count တူရမည်။ AC-F03-03 No memories တွင် Empty CTA ပြရမည်။ AC-F03-04 Partner profile update/guessed file URL မှ မသက်ဆိုင်သော Data မရရ။ AC-F03-05 390 px Mobile နှင့် 1440 px Laptop တွင် CTA မဖြတ်တောက်ရ။

## Feature 4 — F04: Private Real-time Chat

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 4.1 Client Side
#### 4.1.1 Business Understanding

1. Feature Name / Module: Private Chat / Messaging.

2. Purpose [C]: Couple နှစ်ဦးကြား Message ကို အချိန်နှင့်တစ်ပြေးညီ ပို့နိုင်ပြီး History ကို ပြန်ကြည့်နိုင်ရန်။

3. User Roles: Active Couple Members နှစ်ဦးသာ History/View/Send/Read acknowledgement လုပ်နိုင်သည်။ System က message ID/order/time ကို သတ်မှတ်သည်။

4. Business Workflow: Open Chat → Latest history → Subscribe own queue → Compose → Send → SENDING → Server commit/ACK → SENT → Partner client receives/ACK → DELIVERED → Conversation visible/read ACK → READ။ Offline ဖြစ်လျှင် Pending အဖြစ်ပြပြီး reconnect တွင် Retry same clientMessageId နှင့် history catch-up လုပ်ရန်။

#### 4.1.2 Scope Control

15. Out of Scope: Group Chat၊ Message Edit/Delete၊ Forward၊ Message attachment upload၊ Full-text search မပါ [P]။ Attachment အစား Memories ကို သုံးရန်။ Chat content E2EE သည် [C — D04] Current Scope မဟုတ်ပါ။

### 4.2 UI Side
#### 4.2.1 UI and Data Definition

5. Screen List: Private Chat; Older history loading; Empty conversation; Pending/Failed message with Retry; Connection lost/Reconnecting; Unread badge; Online/Offline/Typing [P]. Search control ကို မလုပ်ဆောင်သေးလျှင် disabled explanation ပေးရန်၊ Demo data အဖြစ် မထားရ။

6. Form Fields: Message body — text/Y unless gift/blank/User/C Y/E N; Client Message ID — UUID/Y/Client generated/C hidden/E N; Sender/Recipient/Couple ID — RO/Server/C hidden; Sent Time/Sequence/Status — RO/Server and ACK; Last Read Sequence — integer/Server validated/hidden. History limit 50 per cursor page [P].

### 4.3 Development Side
#### 4.3.1 System Control

7. Validation Rules — CHAT-01: Whitespace-only message မရ; Unicode code points အများဆုံး 4,000 [P]; text ကို Plain Text render လုပ်၍ HTML/JS မ execute ရ။ Authenticated sender၊ recipient membership၊ payload size၊ message rate ကို စစ်ရန်။ Client timestamp ကို Ordering အတွက် မယုံရ။

8. Business Rules — CHAT-02: DB commit ပြီးမှ SENT ACK/event ပို့ရန်။ Unique (senderId, clientMessageId) ဖြင့် duplicate တားရန်။ Conversation sequence သည် stable/increasing ဖြစ်ရမည်; same key different payload တွင် 409 conflict ပြရမည်။ Broadcast failure ဖြစ်လည်း committed message ကို reconnect/history မှ ပြန်ရနိုင်ရမည်။ Delivered သည် WS emit လုပ်ရုံမဟုတ်; Recipient ACK ရမှ update ရမည်။ Read cursor သည် ရှေ့သို့သာ ရွေ့ရမည်။

9. Permission Rules — CHAT-03: REST history နှင့် WS SEND/SUBSCRIBE/ACK တစ်ခုချင်းစီတွင် Membership စစ်ရန်။ Sender ID ကို Session Principal မှယူရန်။ Client ကို Broker topic တိုက်ရိုက် message publish မခွင့်ပြုရ။ Non-member subscribe တွင် Content မရ၊ Connection/Subscription ပယ်ချရမည်။

13. Error Messages: Empty — “Write a message first.”; Too long — “Message is too long.”; Offline — “Connection lost. Reconnecting…”; Failed — “Message not sent. Retry.”; Access denied — “You cannot access this conversation.”

#### 4.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Committed new message → Partner active sessions → WS message.created; unread badge → increase once per logical message; conversation visible တွင် read clear; Typing/Presence သည် ephemeral ဖြစ်ပြီး Offline recipient အတွက် retain မလုပ်ရ။

12. Integration Impact: Spring WebSocket/STOMP + DB; User queue သို့ authoritative message envelope ပို့ရန်။ Reconnect exponential backoff (1, 2, 4, 8… up to 30 seconds with jitter) [P]; lastSequence မှ catch-up ပြီး messageId ဖြင့် deduplicate; Notification failure ကြောင့် Message ကို rollback မလုပ်ရ။

### 4.4 Testing Side
#### 4.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F04-01 Browser နှစ်ခုတွင် A ပို့သည့် message ကို B ရပြီး refresh ပြီးလည်း History ရှိရမည်။ AC-F04-02 Same clientMessageId ကို 3 ကြိမ် retry လုပ်လျှင် row/bubble တစ်ခုသာ ရှိရမည်။ AC-F04-03 DB failure တွင် SENT မပြရ။ AC-F04-04 Internet ပိတ်ပြီး reconnect တွင် missing messages ကို sequence အစဉ်လိုက် ပြန်ဖြည့်ရမည်။ AC-F04-05 Other user/couple ID spoofing နှင့် unauthorized subscription မှ text မပေါက်ကြားရ။ AC-F04-06 Hidden tab သို့ရောက်ရုံဖြင့် READ မသတ်မှတ်ရ။

## Feature 5 — F05: Emoji & Virtual Gifts

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 5.1 Client Side
#### 5.1.1 Business Understanding

1. Feature Name / Module: Emoji & Virtual Gifts / Messaging Extension.

2. Purpose [C]: ခံစားချက်ကို Emoji နှင့် Gift ပုံစံဖြင့် Partner ဆီ ပို့နိုင်ရန်။

3. User Roles: Couple Member သည် Emoji insert၊ Gift select၊ Note add၊ Send၊ Sent/Received history view လုပ်နိုင်သည်။

4. Business Workflow: Chat → Emoji Picker → Emoji insert into composer → Send; Gifts → Choose → Preview + optional note → Confirm Send → Chat gift bubble → Partner receives။ Cancel လုပ်လျှင် Message မဖန်တီးရ။

#### 5.1.2 Scope Control

15. Out of Scope [P]: Paid gifts၊ Currency/Coins၊ Payment Gateway၊ Physical delivery၊ Third-party sticker marketplace မပါ။ Reaction-to-specific-message နှင့် Custom sticker upload ကို Future Scope ထားရန်။

### 5.2 UI Side
#### 5.2.1 UI and Data Definition

5. Screen List: Emoji Picker; Gift Catalog; Gift Preview; Gift Send Success/Error; Gift Bubble; Sent/Received filters။ Unsupported Sticker tab ကို disabled/remove လုပ်ရန်။

6. Form Fields: Emoji — Unicode/N/picker/User/C Y/E N; Gift Code — enum/Y/catalog/User selected/C Y/E N; Note — text/N/blank/User/C Y/E N/max500 code points; Client Message ID — UUID/Y/generated/C hidden; Sender/Receiver/Time — RO/Server. Catalog defaults: Warm Hug, Flowers, Magic, Coffee Date, Goodnight, Always Yours [P].

### 5.3 Development Side
#### 5.3.1 System Control

7. Validation Rules — GIFT-01: Active catalog code ဖြစ်ရမည်; arbitrary asset URL မလက်ခံရ။ Note ကို Plain Text render လုပ်ရန်။ Emoji သည် Unicode sequence ကို မပျက်စီးဘဲ သိမ်း/ပြနိုင်ရမည်။

8. Business Rules — GIFT-02: Gift သည် message type GIFT ဖြစ်ပြီး selected giftCode/note ကို send အချိန်တွင် snapshot သိမ်းရမည်။ Selected Flower ကို Warm Hug အဖြစ် မပြောင်းရ။ Idempotency/history/unread rules သည် CHAT-02 နှင့် တူရမည်။ Gift asset မဖွင့်နိုင်လျှင် text fallback ပြရမည်။

9. Permission Rules — GIFT-03: မိမိ Partner ကိုသာ Send လုပ်နိုင်သည်; recipient ကို Server resolve လုပ်ရန်။ Other space gift history ကို ကြည့်မရ။

13. Error Messages: Invalid gift — “This gift is not available.”; Note too long — “Keep your note within 500 characters.”; Failed send — “Your gift was not sent. Retry.”

#### 5.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Gift committed → Partner သို့ In-App “You received a little gift.”; Retry ကြောင့် Notification ထပ်မဖန်တီးရ။

12. Integration Impact: Internal Chat + Catalog + User queue; commit gift message → dispatch; failure/retry/dedup သည် F04 အတိုင်း။ No external integration required.

### 5.4 Testing Side
#### 5.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F05-01 Myanmar text + Emoji ကို reload ပြီး မပျက်ရ။ AC-F05-02 Flower + Note ပို့လျှင် နှစ်ဦးလုံး Flower + same note ကြည့်ရမည်။ AC-F05-03 Duplicate retry တွင် Gift တစ်ခုသာ ရှိရမည်။ AC-F05-04 Preview cancel တွင် Send/Unread မတိုးရ။ AC-F05-05 Unsupported Gift code/Recipient spoof ကို ပယ်ချရမည်။

## Feature 6 — F06: One-to-one Video Call

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 6.1 Client Side
#### 6.1.1 Business Understanding

1. Feature Name / Module: Video Call / Real-time Communication.

2. Purpose [C]: Couple နှစ်ဦးသည် Mobile/Laptop Browser မှ အသံနှင့် ဗီဒီယို ဆက်သွယ်နိုင်ရန်။

3. User Roles: Caller သည် Call/Cancel၊ Callee သည် Accept/Decline၊ နှစ်ဦးလုံး Mic/Camera toggle/End call လုပ်နိုင်သည်။

4. Business Workflow: Call → Media permission request → RINGING → Partner Accept → Signaling/ICE → CONNECTING → ACTIVE → End → ENDED → Tracks/PeerConnection close။ Permission denied၊ Device missing၊ Partner offline၊ Busy၊ Declined၊ Timeout နှင့် Connection failed များအတွက် Screen State သီးခြားပြရန်။

#### 6.1.2 Scope Control

15. Out of Scope: Group call၊ Recording၊ Screen Sharing၊ Background/closed-browser incoming call guarantee မပါ [P]။ Browser app ဖွင့်ထားပြီး connection ရှိသူကိုသာ Ring ပို့ရန်။

### 6.2 UI Side
#### 6.2.1 UI and Data Definition

5. Screen List: Outgoing Ringing; Incoming Accept/Decline; Connecting; Active Call; Permission Help; Missed/Ended/Failed summary။ Self-preview၊ Partner video၊ Timer၊ Mic/Camera status နှင့် End button ပါရမည်။ Autoplay blocked တွင် Tap to play audio ပြရန်။

6. Form Fields: Call ID — UUID/RO/Server; Caller/Callee — RO/Principal and membership; State — RO/Server; Accepted/Connected/Ended At — UTC/RO; Mic Enabled/Camera Enabled — boolean/User control/default true subject to permission; ICE candidate/SDP — transient/Client WebRTC; Reason — enum/Server validated. Raw media/SDP ကို Normal Logs တွင် မသိမ်းရ။

### 6.3 Development Side
#### 6.3.1 System Control

7. Validation Rules — CALL-01: Authenticated active members only; valid state transition only; stale/wrong call ID signal ကို drop/reject လုပ်ရန်။ Ring timeout 30 seconds [P]။ Current active call ရှိလျှင် Busy ပြရန်။ Simultaneous call နှစ်ခုတွင် Server က Conversation အလိုက် တစ်ခုသာ Active/Ringing ခွင့်ပြုရမည်။

8. Business Rules — CALL-02: WebRTC သည် audio/video transport ဖြစ်သည်။ WebSocket သည် invite/accept/offer/answer/ICE/end signaling သာ ဖြစ်သည်။ TURN relay fallback မပါဘဲ Production call complete ဟု မယူရ။ Accept နှင့် Actual Connected ကို ခွဲထားပြီး Call duration ကို connectedAt မှ endedAt အထိတွက်ရန်။ ICE arriving before remote description ကို queue လုပ်ရန်။ Disconnect အချိန်တွင် Reconnecting status ပြပြီး bounded recovery 15 seconds [P] နောက် Failed/Ended သတ်မှတ်ရန်။ End/Logout တွင် MediaStreamTrack.stop() နှင့် PeerConnection close လုပ်ရမည်။

9. Permission Rules — CALL-03: Call participant နှစ်ဦးသာ SDP/ICE ကို send/receive လုပ်နိုင်သည်။ TURN credentials သည် authenticated request မှ short-lived credentials ပေးရန်။ Long-lived TURN secret ကို JS ထဲ မထည့်ရ။

13. Error Messages: Permission denied — “Allow camera and microphone access to start a call.”; Offline — “Your partner is currently offline.”; Busy — “Your partner is on another call.”; Failed — “We could not connect your call. Try again.”

#### 6.3.2 Dependency and Communication

10. Report Impact: No report impact in this version. Member-facing call outcome/timing summary သာ ရှိမည် [P]။

11. Notification Rules: New call → Partner active browser sessions → incoming ring; one session accepts → other ringing tabs stop; Missed call → F09 In-App notification once per call.

12. Integration Impact: Browser WebRTC + STUN/TURN + Spring signaling; Send SDP/ICE + call ID; Receive peer signals; Signaling events idempotent by eventId; ICE duplicates tolerate; Retry သည် Current state နှင့် Call ID အတွင်းသာ; media failure တွင် User-facing error နှင့် Resource cleanup လုပ်ရန်။ HTTPS/WSS လိုအပ်သည် (localhost development exception)။

### 6.4 Testing Side
#### 6.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F06-01 Phone/PC browser နှစ်ခု၊ မတူသော Network များတွင် Bidirectional audio/video ဆက်သွယ်ရမည်။ AC-F06-02 Forced TURN relay test အောင်မြင်ရမည်။ AC-F06-03 Mic off တွင် Remote audio track မကြားရ၊ Camera off တွင် မိမိ Video ပို့ခြင်းရပ်ရမည်။ AC-F06-04 Decline/30s timeout/busy ကို မှန်ကန်ပြရမည်။ AC-F06-05 Either side End ပြီးနှစ်ဖက် camera/mic resource ရပ်ရမည်။ AC-F06-06 Non-participant/stale-call signal ဖြင့် Call ကိုမထိခိုက်နိုင်ရ။

## Feature 7 — F07: Shared Memories

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 7.1 Client Side
#### 7.1.1 Business Understanding

1. Feature Name / Module: Shared Memories / Private Scrapbook.

2. Purpose [C]: Couple ၏ ဓာတ်ပုံ၊ ဗီဒီယိုနှင့် အမှတ်တရစာသားများကို စုပေါင်း သိမ်းဆည်းနိုင်ရန်။

3. User Roles: Member နှစ်ဦး Create/View/Favorite နိုင်သည်။ [C — D03] Couple Member နှစ်ဦးလုံးသည် တစ်ဦးတင်ထားသော Memory ကိုပါ Edit/Delete နိုင်သည်။ Creator identity သည် Authorship အတွက်ဖြစ်ပြီး Edit/Delete permission ကို မကန့်သတ်ရ။

4. Business Workflow: Add Memory → Select actual device file(s) → Validate/upload progress → Title/Date/Caption → Save READY assets → List/Detail → Optional Edit/Favorite/Delete confirmation။ Upload fail တွင် Retry/Remove နှင့် Old content ထိန်းထားရန်။

#### 7.1.2 Scope Control

15. Out of Scope: Public sharing link၊ AI tagging/face recognition၊ External cloud photo import၊ Unlimited storage မပါ။ Unpair/Account deletion ပြီး Data disposition ကို Q03 တွင် ဆုံးဖြတ်ရန်။

### 7.2 UI Side
#### 7.2.1 UI and Data Definition

5. Screen List: Memories Grid; Add/Edit Memory; Upload Progress/Failed; Detail Gallery with Video Controls; Delete Confirmation; All/Photos/Videos/Favorites filters; Empty/Loading/Error။

6. Form Fields: Title — text/Y/blank/User/C Y/E both members Y/max120; Memory Date — LocalDate/Y/today/User/C Y/E Y; Caption — text/N/blank/User/C Y/E Y/max2000; Assets — image/video/Y/min1 max10/User/C Y/E Y; Favorite — boolean/N/false/per-user/C N/E Y; Creator/Created At/Updated By/Updated At/Version/Asset status — RO/Server. List fields: thumbnail, title, memory date, asset count, creator, favorite; Date sort, type/favorite filter [P].

### 7.3 Development Side
#### 7.3.1 System Control

7. Validation Rules — MEM-01: Images JPEG/PNG/WebP ≤10 MiB each; videos MP4 ≤100 MiB and ≤120 seconds each [P/Q05]; maximum10 files; title not blank; Memory Date not future. MIME/extension နှင့် file signature စစ်ရန်၊ HTML/SVG/executable မလက်ခံရ။ Playback support အတွက် baseline H.264/AAC compatibility စစ်ပြီး Unsupported file ကို ရှင်းလင်းစွာ reject လုပ်ရန်။

8. Business Rules — MEM-02: Binary files ကို DB BLOB ထဲ မသိမ်းဘဲ private storage တွင်သိမ်း၊ metadata/reference ကို DB ထဲသိမ်းရန်။ Asset READY ဖြစ်ပြီးမှ Memory publish လုပ်ရန်။ Failed transaction/orphaned upload cleanup ကို Scheduled Job ဖြင့် 24 hours နောက် ဆောင်ရွက်ရန် [P]။ Pagination 24/cursor page; favorite သည် User တစ်ဦးချင်း Preference ဖြစ်သည်။ Delete သည် soft-delete immediately hide, binary purge after30 days [P/Q03]; backup expiry မှသာ backup copies ရှင်းမည်။

9. Permission Rules — MEM-03: Every metadata/file/thumbnail request တွင် Couple membership စစ်ရန်။ Public bucket/direct permanent URL မထုတ်ရ။ V1 default authenticated streaming endpoint; storage private. [C — D03] Active Couple Members နှစ်ဦးလုံး Edit/Delete ကို Server က ခွင့်ပြုရမည်; Non-member ကို ပိတ်ရမည်။ [P] Concurrent edit/delete အတွက် version conflict 409 ကို သုံးပြီး silent overwrite မလုပ်ရ; Original Creator၊ Last Updated By နှင့် Deleted By ကို မှတ်တမ်းတင်ရမည်။

13. Error Messages: Invalid file — “This file type is not supported.”; Too large — “This file exceeds the upload limit.”; Upload failed — “Upload failed. Retry or remove this file.”; No file — “Add at least one photo or video.”

#### 7.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Memory published → Partner → In-App “A new memory was added.”; Draft/failed upload တွင် notify မလုပ်ရ။

12. Integration Impact: Private filesystem in Dev; private object storage or mounted persistent storage in Production [P/Q05]. Send validated bytes/key; Receive stored metadata/status; failure တွင် publish မလုပ်ရ; retry uses uploadId and idempotencyKey; playback uses authenticated byte-range requests; video controls/mobile fallback ပံ့ပိုးရန်။

### 7.4 Testing Side
#### 7.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F07-01 Photo/video တကယ် Upload လုပ်ပြီး Browser restart နောက် List/Detail တွင် ပြန်ဖွင့်နိုင်ရမည်။ AC-F07-02 Unsupported/oversized file reject ရမည်။ AC-F07-03 Non-member သည် guessed asset/thumbnail URL မှ Data မရရ။ AC-F07-04 Failed upload သည် public Memory မပေါ်ရ။ AC-F07-05 Member A တင်ထားသော Memory ကို Member B က Edit/Delete လုပ်နိုင်ရမည်; Non-member request ကို reject ရမည်; Creator ကို မပြောင်းဘဲ Updated/Deleted By ကို မှတ်တမ်းတင်ရမည်။ AC-F07-06 Favorite/filter/reload က user-specific ဖြစ်ရမည်။ AC-F07-07 Deleted memory က list/file endpoint မှ မရ၊ retention job က metadata policy နှင့်ညီရမည်။ AC-F07-08 Concurrent stale edit/delete တွင် 409/reload state ပြရမည် [P]။

## Feature 8 — F08: Relationship Counter & Special Dates

Feature Status: Client Draft; UI UI Review; Development Not Ready; QA Not Started.

### 8.1 Client Side
#### 8.1.1 Business Understanding

1. Feature Name / Module: Our Dates / Relationship Calendar.

2. Purpose [C]: ချစ်သူဖြစ်သည့် နေ့မှစ၍ ဘယ်နှရက်မြောက်ဖြစ်သည်၊ နှစ်ပတ်လည်နှင့် အထူးရက်များ မည်မျှကျန်သည်ကို ကြည့်နိုင်ရန်။

3. User Roles: Couple Member နှစ်ဦး Shared dates View/Create/Edit နိုင်သည်; Event Creator သာ Delete နိုင်သည် [P]။ System က Day count/Upcoming/Reminder တွက်သည်။

4. Business Workflow: Set Start Date/Timezone → Home/Our Dates count ပြ → Add Date → Title/Date/Repeat/Reminder → Save → Upcoming list → Due reminder; Edit Shared Start Date တွင် Both members ထိခိုက်မည်ကို Confirmation ပြရန်။

#### 8.1.2 Scope Control

15. Out of Scope: Google Calendar sync၊ Myanmar Lunar Calendar၊ recurring rules beyond yearly၊ Financial subscription reminders မပါ။

### 8.2 UI Side
#### 8.2.1 UI and Data Definition

5. Screen List: Our Dates; Add/Edit Special Date; Start Date Change Confirmation; Reminder Settings; Empty/Validation States။ Screen တွင် days together နှင့် ordinal day ကို သီးခြား Label ပြရန်။

6. Form Fields: Relationship Start Date — LocalDate/Y/User/C F01 Sign Up then initialize in F02/E both members; Timezone — IANA/Y/browser suggestion confirmed/C F02/E both; Event Title — text/Y/blank/User/C Y/E Y/max100; Event Date — LocalDate/Y/today/User/C Y/E Y; Repeat Yearly — boolean/N/false/User/C Y/E Y; Reminder Enabled — boolean/N/true/User/C Y/E Y; Reminder Lead — enum0/1/7 days/default1; Reminder Local Time — time/default09:00 [P]. List fields: title, next date, days until, repeat, reminder status.

### 8.3 Development Side
#### 8.3.1 System Control

7. Validation Rules — DATE-01: Start Date must be today or past in Couple Timezone; Timezone valid; Event date valid; repeat non-yearly event in past allowed as history but no past reminder; Server validates title/reminder settings.

8. Business Rules — DATE-02: Proposal: daysTogether = DAYS.between(startDate, todayInCoupleTimezone); Start day value0; ordinalDay=daysTogether+1; UI label “ရက်မြောက်” သုံးလျှင် ordinalDay ကိုသာပြရန် [Q02]။ Sample: 2025-10-31 → 2026-10-31 =365 days together /366th day. Yearly anniversary = same month/day in Couple Timezone; Feb29 event ကို Non-leap year တွင် Feb28 ဟု defaultယူထားသည် [P/Q02]။ Upcoming countdown သည် Next LocalDate − today; today=0/“Today”; negative upcoming မပြရ။

9. Permission Rules — DATE-03: Couple scope only; both members Edit shared date၊ Owner Delete event။ Start Date ပြောင်းလျှင် Summary/counter/scheduled reminders အားလုံး Recalculate လုပ်ရမည်။ Version conflict တွင် overwriting မလုပ်ဘဲ Reload ပြရန်။

13. Error Messages: Future start — “Your relationship start date cannot be in the future.”; Conflict — “This date was updated by your partner. Reload and try again.”; Invalid title — “Enter a name for this special date.”

#### 8.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Upcoming date at configured local time → Both members → In-App; yearly/anniversary occurrence အလိုက် တစ်ကြိမ်သာ။ Start Date edit → Partner သို့ “Your relationship date was updated.”

12. Integration Impact: DB + Time API + Scheduler + F09; Use UTC for event timestamps and LocalDate for relationship dates; job key=(eventId, occurrenceDate, receiverId, reminderType); restart/retry ကြောင့် reminder မထပ်ရ။ No external integration required.

### 8.4 Testing Side
#### 8.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F08-01 Start day/365-day/ordinal samples အထက်ပါအတိုင်းဖြစ်ရမည်။ AC-F08-02 Timezone midnight change သည် reload မလုပ်ဘဲ counter refresh ရမည်။ AC-F08-03 Leap-day/year-end cases မှန်ရမည်။ AC-F08-04 Start Date Edit ပြီး Home နှင့် Our Dates တူညီရမည်။ AC-F08-05 Same scheduler job ကို ထပ်မောင်းလျှင် Notification တစ်ကြိမ်သာရမည်။ AC-F08-06 Other space date edit ကို reject ရမည်။

## Feature 9 — F09: Notifications, Presence & Connection Recovery

Feature Status: Client Draft; UI Not Started; Development Not Ready; QA Not Started.

### 9.1 Client Side
#### 9.1.1 Business Understanding

1. Feature Name / Module: Notifications & Recovery / Shared Experience.

2. Purpose [P]: Chat၊ Gift၊ Memory၊ Incoming Call နှင့် Special Date အကြောင်းကို မလွတ်စေရန်နှင့် Network ပြတ်လျှင် State ပြန်ရရန်။

3. User Roles: Member သည် မိမိ Notifications View/Mark Read လုပ်နိုင်သည်; System သည် eligible event များကို ဖန်တီးသည်။

4. Business Workflow: Business event committed → Notification stored if persistent → Active browser receives → Badge/List update → Click target feature → Read mark; Offline browser → Next Login REST catch-up; Socket lost → Banner/backoff → Re-authenticate if needed → Resubscribe/history reconcile.

#### 9.1.2 Scope Control

15. Out of Scope [C — D06]: Browser Web Push၊ SMS၊ Native Push နှင့် Site ပိတ်ထားချိန် Notification/Live Ring မပါ။ Password Reset Email သည် F01 သီးခြား Integration ဖြစ်သည်။

### 9.2 UI Side
#### 9.2.1 UI and Data Definition

5. Screen List: Notification Bell/Unread Badge; Notification List; Connection Banner; Partner Online/Offline; Notification target unavailable state။

6. Form Fields: Notification ID/Type/Created At/Target — RO/Server; Read At — nullable/default null/User mark action; Presence — online/offline/Server ephemeral; Connection State — connecting/connected/reconnecting/offline/Client; Per-event duplicate key — hidden/Server. Sensitive message content ကို Notification preview တွင် default မဖော်ပြရန် [P].

### 9.3 Development Side
#### 9.3.1 System Control

7. Validation Rules — NOTIFY-01: Mark-read request သည် Recipient ကို စစ်ရမည်။ Target resource deleted/unauthorized ဖြစ်လျှင် Content မပြရ။ Heartbeat interval20s၊ absence60s နောက် offline [P]။

8. Business Rules — NOTIFY-02: All active tabs/devices မှ Session connection တစ်ခုရှိလျှင် Online; one tab close ရုံဖြင့် Offline မသတ်မှတ်ရ။ Multi-device notifications deduplicate by eventId; persistent notification retention90 days [P/Q03]. Typing expires after5s [P]; presence/typing များကို long-term store မလုပ်ရ။

9. Permission Rules — NOTIFY-03: Own recipient queue/list only; server routes notifications; Client က Other user notifications publish/mark read မလုပ်နိုင်ရ။

13. Error Messages: Unavailable target — “This item is no longer available.”; Reconnect — “Reconnecting… Your saved messages are safe.” ကို DB committed messages အတွက်သာ သုံးရန်။

#### 9.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: New message/gift/memory/missed call/date reminder/partner joined → designated partner/both members → In-App + user queue; failure → saved inbox from REST; Notification identity=(sourceEventId, recipientId, type) unique.

12. Integration Impact: Internal Scheduler + WebSocket + DB. Persist business event first; after-commit dispatch; retry/backoff; durable inbox catches event while offline; dedup on server and client. Reconnect with expired Session → Login, not infinite unauthenticated retries. No external integration required.

### 9.4 Testing Side
#### 9.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F09-01 Offline recipient သည် Login ပြန်ဝင်လျှင် saved Notification ရရမည်။ AC-F09-02 Same event replay တွင် Badge မထပ်တိုးရ။ AC-F09-03 Other user's notification read API deny ရမည်။ AC-F09-04 Browser tabs နှစ်ခုအနက်တစ်ခုပိတ်လျှင် Online ဆက်ပြရမည်။ AC-F09-05 Session expired reconnect သည် Login သို့ ပြန်ပို့ရမည်။

## Feature 10 — F10: Privacy, Settings & Session Control

Feature Status: Client Draft; UI Not Started; Development Not Ready; QA Not Started.

### 10.1 Client Side
#### 10.1.1 Business Understanding

1. Feature Name / Module: Privacy & Settings / Account and Space Controls.

2. Purpose [C-derived/P]: Private Data ကို အခြားသူများမကြည့်နိုင်စေရန်နှင့် User က Session/Settings ကို ထိန်းနိုင်ရန်။

3. User Roles: Account Owner သည် မိမိ Profile/Password/Session ကို ထိန်းနိုင်သည်။ Both Members သည် Shared Date/Timezone ကို Confirmation ဖြင့် ပြောင်းနိုင်သည်။ Operator အတွက် App-level content browsing Role မဖန်တီးရ။

4. Business Workflow: Settings → My Profile / Security / Our Dates / Logout → Submit → Validate → Save + reauthorize if needed; destructive action နှင့် ownership policy မအတည်ပြုသေးလျှင် Action မဖွင့်ရ။

#### 10.1.2 Scope Control

15. Out of Scope [P/Q03]: Self-service Unpair၊ Account Delete၊ Full Data Export၊ Recovery transfer သည် Owner decision ရသည့်နောက် Version တစ်ခုအဖြစ် သတ်မှတ်ရန်။ UI တွင် Working button အဖြစ် မဖော်ပြရ။

### 10.2 UI Side
#### 10.2.1 UI and Data Definition

5. Screen List: Settings; Profile; Security/Change Password; Logout Confirmation; Shared Setting Change Confirmation; Access Denied; Session Expired. Prototype ၏ Settings → Pairing shortcut ကို Production တွင် Real Settings ဖြင့် အစားထိုးရမည်။

6. Form Fields: Profile fields — F03; Password fields — F01; Couple Timezone/Start Date — F08; Session state — RO/Server; Logout All action — authenticated POST/current credential confirmation [P].

### 10.3 Development Side
#### 10.3.1 System Control

7. Validation Rules — PRIV-01: Every write validates ownership/membership server-side; cookie/session/CSRF/origin checks; shared setting update uses optimistic version; private content ကို third-party analytics/session replay ဆီ မပို့ရ။

8. Business Rules — PRIV-02: TLS transport + protected storage; at-rest encryption ကို Hosting/storage configuration ဖြင့် သတ်မှတ်ရန် [P]. [C — D04] App server သည် text/media ကို ဖတ်/process လုပ်နိုင်သည်; E2EE မပါသဖြင့် E2EE claim မလုပ်ရ။ Secrets၊ passwords၊ session IDs၊ reset codes၊ chat contents၊ raw SDP/ICE ကို application logs ထဲ မထုတ်ရ။ Account disable ဖြစ်လျှင် session/WS/call access ရပ်ရန်။ Logs သည် requestId၊ event type၊ opaque identifiers၊ outcome သာ ပါရမည်။

9. Permission Rules — PRIV-03: Deny by default; hidden UI သည် permission enforcement မဟုတ်။ Authenticated but forbidden access တွင် 403 သို့မဟုတ် resource enumeration ကာကွယ်ရန် consistent404 policy သုံးရန်။ Attachments အပါအဝင် requests အားလုံးကို စစ်ရန်။

13. Error Messages: Forbidden — “You do not have access to this item.”; Save failed — “Your changes were not saved. Try again.”; Confirm — “This change affects both of you.”

#### 10.3.2 Dependency and Communication

10. Report Impact: No report impact in this version.

11. Notification Rules: Password/session security → Account Owner; Shared settings update → Partner; Body တွင် secret/personal content မပါရ။

12. Integration Impact: Spring Security + WS connection registry + private storage + backup policy; logout/disable → invalidate sessions, disconnect WS, end active media; failure/retry must be safe to repeat; revoke state ကို Subsequent requests တွင် အမြဲစစ်ရန်။ No external integration required.

### 10.4 Testing Side
#### 10.4.1 Acceptance and Verification

14. Acceptance Criteria: AC-F10-01 Other account IDs၊ media IDs၊ WS destinations များ guess လုပ်သော်လည်း Private Data မရရ။ AC-F10-02 Logout All ပြီး device sessions/WS အားလုံးပိတ်ရမည်။ AC-F10-03 Logs ထဲ password/token/chat body မပါရ။ AC-F10-04 Settings conflict တွင် Partner change ကို မသိမသာ overwrite မလုပ်ရ။ AC-F10-05 Browser Back/Cache မှ Logout နောက် Protected Content ပြန်မပေါ်ရ (private response cache policy ကို စစ်ရန်)။

### 5.11 Cross-feature Technical & Codex Implementation Requirements

[C] Java/Spring Boot/WebSocket/JavaScript၊ Local-first Development နှင့် D01–D06 Rules ကို ထိန်းရမည်။ အောက်ပါ Engineering Selections သည် တခြားအတည်ပြုချက်မရှိသရွေ့ [P] ဖြစ်သည်။

#### 5.11.1 Technology responsibilities

| Layer | Proposed implementation | Responsibility |
|---|---|---|
| Backend | Java 21 + compatible stable Spring Boot | Business rules, authentication, API, persistence |
| Security | Spring Security; server-side session | Login, CSRF, authorization, logout/revocation |
| Persistence | PostgreSQL + Spring Data JPA + Flyway | Transactional records, constraints, versioned migrations |
| Realtime | Spring WebSocket + STOMP + JavaScript STOMP client | Chat events, presence, notifications, call signaling |
| Media calling | Browser WebRTC + STUN/TURN (coturn or provider) | Bidirectional audio/video and relay fallback |
| Frontend | HTML5/CSS3 + JavaScript ES modules | Responsive UI, Fetch API, realtime handling, WebRTC UI |
| Assets | Private filesystem in Dev; private persistent storage in Prod | Memories/avatars, authorized streaming |
| Build/Run | Maven Wrapper; Docker Compose for supporting services | Reproducible setup and local DB/TURN/mail testing |
| Tests | JUnit/Spring Boot Test, Testcontainers, browser E2E | Rules, real DB constraints, WS/security, critical UI journeys |

Java/Spring Boot/WebSocket/JavaScript နှင့် Local-first Environment သည် [C] ဖြစ်ပြီး အခြား selections သည် [P] ဖြစ်သည်။ Local အတွက် Maven Wrapper၊ PostgreSQL persistent volume၊ Private upload folder၊ Mail testing inbox နှင့် Development TURN config ကို README တွင် သတ်မှတ်ရန်။ Online domain/hosting/provider ကို ယခုအဆင့်တွင် မဝယ်/မတပ်ဆင်ရ; Hosting နောက်မှရွေးမည် [C — D05]။ Bootstrap အချိန်တွင် Java-compatible stable Boot/dependency versions ကို verify/pin လုပ်ပြီး README နှင့် lock/build files တွင် မှတ်တမ်းတင်ရမည်။ Pre-release ကို default မသုံးရ။ React/TypeScript/Node backend သို့ မိမိသဘောဖြင့် stack မပြောင်းရ။ Frontend သည် Spring Boot static assets မှ Same-origin ဖြင့် Serve လုပ်ရန် အကြံပြုသည်။

#### 5.11.2 Architecture boundaries

Browser UI → HTTPS REST for Auth/History/CRUD/Uploads → Spring Boot modular monolith → PostgreSQL/private asset storage.

Browser UI → WSS/STOMP → Authenticated per-user queues for chat/call/presence events.

Browser A ↔ WebRTC media ↔ Browser B; connection မတည်နိုင်လျှင် TURN relay သုံးရမည်။ TURN သည် media path; Spring signaling သည် media relay မဟုတ်ပါ။

Frontend/Backend Same-origin Session Cookie ကို default သုံးရန်။ Cookie Secure (Production), HttpOnly, SameSite=Lax; HTTPS; explicit allowed WS origins; CONNECT/unsafe HTTP writes အတွက် required CSRF protections လုပ်ရန်။ Browser WebSocket handshake တွင် arbitrary Authorization header ထည့်နိုင်မည်ဟု မယူဆရ။ JWT/localStorage ကို default auth မလုပ်ရ။ Auth changes are design changes, not a silent implementation choice.

Single application instance + built-in simple broker ကို Initial deployment အတွက်သာ အကြံပြုသည်။ Horizontal scaling လိုပါက Shared sessions၊ external broker၊ distributed scheduler locks နှင့် presence coordination လိုအပ်မှုကို ပြန်ဒီဇိုင်းလုပ်ရန်။ Simple broker ကို durable offline message store ဟု မယူရ။

#### 5.11.3 Logical data responsibilities

User; CoupleSpace; CoupleMembership; Invitation; Conversation; Message; ReadCursor; GiftCatalog; CallSession; Memory; MemoryAsset; MemoryFavorite; SpecialDate; Notification; PasswordResetToken; operational security audit.

Entity list သည် Logical Direction ဖြစ်ပြီး approved physical schema မဟုတ်ပါ။ Implementation မတိုင်မီ fields/keys/FK/index/nullability၊ message sequence allocation၊ unique idempotency keys၊ version locking နှင့် migrations ကို ရေးရန်။ Invite consumption + membership update နှင့် Message insert + sequence allocation သည် Transactional ဖြစ်ရမည်။ Dates ကို LocalDate; event timestamps ကို UTC Instant သုံးရန်။ Enum values ကို String သို့မဟုတ် documented reference tables ဖြင့်သိမ်း၍ ordinal migration drift မဖြစ်စေရ။

#### 5.11.4 Proposed REST surface (contracts must be finalized before feature coding)

| Requirement | API group | Purpose |
|---|---|---|
| AUTH-01–03 | POST /api/auth/register, /login, /logout; GET /api/me; GET /api/csrf | Account, session and CSRF bootstrap |
| AUTH-02 | POST /api/auth/password-reset/request, /confirm; POST /api/me/password, /logout-all | Recovery and session revocation |
| PAIR-01–03 | POST /api/spaces, /api/invitations, /api/invitations/join; GET /api/space | Pairing and current membership |
| HOME-01–03 | GET /api/home; PATCH /api/me/profile; PUT /api/me/avatar | Summary and own profile |
| CHAT-01–03 | GET /api/messages?afterSeq=... or beforeSeq=...&limit=50 | History and reconnect catch-up |
| GIFT-01–03 | GET /api/gifts | Active virtual gift catalog |
| CALL-01–03 | GET /api/calls/current, /api/calls/{id}; GET /api/rtc/config | Reconcile call and short-lived ICE config |
| MEM-01–03 | POST /api/uploads; GET /api/uploads/{id}; POST/GET/PATCH/DELETE /api/memories[/{id}] | Upload status and memory lifecycle |
| MEM-03 | GET /api/assets/{id}/content; PUT/DELETE /api/memories/{id}/favorite | Authorized range streaming and favorite |
| DATE-01–03 | GET/PATCH /api/space/relationship; GET/POST/PATCH/DELETE /api/dates[/{id}] | Shared counter and events |
| NOTIFY-01–03 | GET /api/notifications; POST /api/notifications/{id}/read | Personal persistent inbox |

Path shorthand တွင် Collection create/list နှင့် Resource update/delete ကို သက်ဆိုင်ရာ Route အဖြစ် သီးခြားသတ်မှတ်ရမည်။ JSON error: code, message, fieldErrors, requestId; sensitive internal stack trace မပို့ရ။ 400 validation; 401 unauthenticated; 403 forbidden; 404 missing/concealed resource; 409 conflict; 413 upload too large; 429 rate limit; 5xx recoverable failure. No hard-coded userId/coupleId in JS.

#### 5.11.5 WebSocket/STOMP contract direction

- Handshake endpoint: /ws; authenticate by existing HTTP Session; enforce trusted origin and required CONNECT CSRF token.
- Application destinations: /app/chat.send, /app/chat.delivered, /app/chat.read, /app/chat.typing; /app/call.invite, /accept, /decline, /cancel, /offer, /answer, /ice, /end; Presence heartbeat.
- Client subscriptions: /user/queue/messages; /user/queue/calls; /user/queue/notifications; /user/queue/errors။ Client-supplied routing to arbitrary recipient queues ကို reject လုပ်ရန်။
- Client envelope: eventId, clientMessageId if durable message, type, payload, optional callId. userId/senderId/coupleId ကို authorization authority အဖြစ် မသုံးရ။
- Server envelope: eventId, type, serverTimestamp, resourceId, sequence where applicable, authoritative payload. Error envelope ကို originating user/session scope အတွင်းသာ ပြန်ပို့ရန်။
- Chat/gift writes သည် STOMP command တစ်နေရာမှ service ကို ဝင်ရန်; REST fallback ထည့်လျှင် same service/idempotency ကို ပြန်သုံးရမည်၊ double message path မဖြစ်ရ။
- Disconnect/logout/session revoke ဖြစ်လျှင် Permission revoked connection မှ Event ဆက်မရရ။ SUBSCRIBE နှင့် SEND ကို action တစ်ခုချင်း authorization လုပ်ရန်။

#### 5.11.6 Non-functional requirements and operational defaults

- UI: 360/390 px mobile၊ 768 px tablet၊ 1366/1440 px laptop widths မှာ horizontal overflow မရှိရ။ Controls ≥44 px tap target; labels/focus states; semantic inputs; keyboard navigation; readable contrast; loading/empty/error/disabled states။
- Browsers: Implementation အချိန် stable Chrome/Edge (Windows), Chrome (Android), Safari (iOS/macOS) ကို Version/date အတိအကျ မှတ်တမ်းတင်ပြီး စမ်းရန်။ Background tab throttling/autoplay/permission behavior ကို device အလိုက် စမ်းရန်။
- Performance targets [P]: agreed staging network တွင် Normal REST p95 ≤500ms excluding upload/external services; DB commit-to-partner Chat event p95 ≤1 second for connected clients; media connection target ≤10s after accept, network dependent; metrics/test conditions ကို Report ရေးရန်။
- Load envelope [P/Q06]: one couple/two accounts, up to4 browser sessions/account; history dataset10,000 messages; quotas/storage budget ကို Q05 ဖြင့် သတ်မှတ်ရန်။ Test envelope ကို Target infrastructure သတ်မှတ်ပြီးမှ measurable acceptance အဖြစ် finalize လုပ်ရန်။
- Reliability: durable save before ACK; cursor pagination; timeout/retry; idempotency; background job uniqueness; DB/storage failure states; application restart နောက် DB/file state ပြန်ရရမည်။
- Operations: persistent DB/storage volumes; secrets environment configuration; migrations on clean and existing DB; controlled schema evolution; health/readiness endpoint without secrets; structured requestId logs; bounded uploads/rate limits။
- Backup [P/Q03]: daily encrypted DB + asset backup, retention30 days, separate access; restore drill to isolated environment; target RPO24h/RTO4h after infrastructure approval။ Database backup သာရှိပြီး assets မရှိလျှင် Complete restore ဟု မယူရ။
- No production secrets/sample passwords in repository. .env.example တွင် placeholders only. Third-party code/assets licenses ကို မှတ်တမ်းတင်ရန်။

#### 5.11.7 Codex implementation sequence and completion evidence

အောက်ပါအစီအစဉ်သည် Proposed task order ဖြစ်ပြီး Business Approval အစား မဟုတ်ပါ။ Task တစ်ခုစီတွင် Requirement IDs၊ Changed files၊ Migration impact၊ Tests နှင့် Remaining issues ကို Return လုပ်ရမည်။

1. DEV-01 Foundation: Maven/Spring project, Java/dependency version pinning, PostgreSQL/Flyway, environment examples, health check, runnable README.
2. DEV-02 Authentication: F01 + F10 permission foundation; session/CSRF/reset/email tests; Mobile/Laptop Login/Sign Up/Reset UI; Relationship Date required at registration; maximum2 accounts.
3. DEV-03 Pairing: F02; strict2-member transaction constraints; own-space route guard; concurrency/security tests.
4. DEV-04 Home/Profile/Dates: F03/F08; date calculations and timezone/leap tests; responsive navigation.
5. DEV-05 Chat: F04/F09 base; authenticated WS destinations; persist/ACK/reconnect/idempotency/read tests.
6. DEV-06 Emoji/Gifts: F05; real catalog + selection snapshot + chat integration; double-send/Unicode tests.
7. DEV-07 Memories: F07; private uploads/streaming, CRUD, pagination/filters, both-members Edit/Delete, authorization/concurrency and storage cleanup tests.
8. DEV-08 Calls: F06; incoming/outgoing state machine; actual media tracks; TURN credentials/config; real two-device test.
9. DEV-09 Notifications/Settings: F09/F10; reminders, presence, revocation, permission review and missing supporting UI.
10. DEV-10 Integration & delivery: end-to-end signup→pair→chat→gift→memory→date→call→logout; two-user/unauthorized cases; deployment runbook and known-issue list.

Completion Definition: Demo navigation/Toast/LocalStorage-only persistence သည် Feature Complete မဟုတ်ပါ။ Data သည် actual DB/storage တွင် persist ရမည်; permission checks/negative paths ပါရမည်; automated tests နှင့် browser/manual media evidence ရှိရမည်။ Video Call တွင် Figma-style toggles သာပြောင်းပြီး Remote track မပြောင်းလျှင် မအောင်မြင်ပါ။

Repository documents [P]: requirement file; architecture notes; ERD/schema/migrations; API/WS contracts; README startup commands; test evidence; known issues. ၎င်းတို့ကို Same requirement IDs နှင့် trace လုပ်ရန်။ Source Code implementation ကို ဤ request တွင် မဖန်တီးထားပါ။

#### 5.11.8 Official technical references

Checked 31 October 2026. Recommendations နှင့် Version choices ကို အတည်ပြု specification ဟု မယူရ။

- Spring Framework: WebSocket/STOMP authentication — https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication.html
- Spring Security: WebSocket security — https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html
- WebRTC: Peer connections, signaling, ICE/STUN/TURN — https://webrtc.org/getting-started/peer-connections
- MDN: Camera/microphone permission and secure context — https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia

### 6. Owner Decision Register & Remaining Open Questions

#### 6.1 Confirmed Owner Decisions — 31 October 2026

| Decision | Affected features | Confirmed requirement |
|---|---|---|
| D01 | F01/F02/F10 | Couple တစ်တွဲ၊ Account နှစ်ခုသာ အသုံးပြုမည်။ Public Multi-couple signup မပါ။ |
| D02 | F01/F02/F03/F08 | Account Sign Up လုပ်ချိန်မှာ Relationship Date မဖြစ်မနေ ထည့်ရမည်။ |
| D03 | F07/F10 | Memory ကို Couple Member နှစ်ဦးလုံး Edit/Delete လုပ်နိုင်မည်။ |
| D04 | F04/F07/F10 | Account နှစ်ခုကသာ App မှဝင်ကြည့်နိုင်မည်; Server က Data ကို ဖတ်နိုင်မည်; E2EE မပါ။ |
| D05 | All | Local Development အရင်လုပ်မည်; Online Hosting/Provider/Budget ကို နောက်မှရွေးမည်။ |
| D06 | F06/F09 | Site ဖွင့်ထားချိန် In-App Notification သာ; Browser Push/closed-site ring မပါ။ |

Confirmed By: Thein Than Zaw, direct conversation selections. D02 အဖြေသည် Relationship Date ထည့်ရမည့်အချိန်ကို အတည်ပြုခြင်းသာဖြစ်ပြီး Day0/Day1 count ကို အတည်ပြုခြင်း မဟုတ်ပါ။ D03 သည် Edit/Delete လုပ်ပိုင်ခွင့်ကိုသာ အတည်ပြုပြီး Purge/Backup Retention ကို မဆုံးဖြတ်ရသေးပါ။

#### 6.2 Remaining details — Proposed, not silently approved

| Question | Affected features | Decision still needed | Proposed default |
|---|---|---|---|
| Q02 | F03/F08 | Day-count label / Feb29 anniversary convention | Start day:0 days together /1st day; Feb29→Feb28 in non-leap year. |
| Q03 | F02/F07/F09/F10 | Unpair/account lifecycle, deleted file purge, notification/backup retention | No self-service unpair/account delete in V1; immediate hide,30-day purge; notification90 days; backup30 days. Memory permissions already resolved by D03. |
| Q05 | F03/F07 | Upload size/duration/count and total storage quota | Image10 MiB; video100 MiB/120s;10 files per memory; total quota to define. Online provider/budget deferred by D05. |
| Q06 | UI/All | UI language, Couple timezone and target devices/load envelope | English UI; Myanmar Unicode content; confirm timezone;2 accounts/up to8 sessions. Notification channel already resolved by D06. |

Invitation token lifecycle၊ Partner signup date conflict handling၊ SMTP recovery နှင့် Engineering Defaults များသည် [P] အဖြစ် ဆက်ရှိသည်။ Relevant unresolved details ရှိသော Feature ကို Full Development Ready ဟု မသတ်မှတ်ရ။ အတည်ပြုထားသော D01–D06 ကို ထပ်မေးရန် မလိုပါ။

### 7. Requirement Change Record

v1.1 တွင် User ၏ D01–D06 ဆုံးဖြတ်ချက်များကို Incorporate လုပ်ထားသည်။ Full specification approval မရှိသေးသော်လည်း အောက်ပါ ဆုံးဖြတ်ချက်များကို အတည်ပြုပြီး Record အဖြစ် ထိန်းထားရမည်။

- CR-001: Affected F01/F02/F03/F08; Requested By Thein Than Zaw; Date31 October2026; Change: Sign Up တွင် Relationship Date required. Reason: Account ဖွင့်ချိန်ကတည်းက Date ပါလိုခြင်း. UI Impact: Mobile/Laptop signup field/error; Dev Impact: Account pending date၊ validation၊ Space initialization/confirmation; QA Impact: missing date/persistence/conflict tests; Timeline Impact: Not estimated; Decision: Current Version; Confirmed By: Thein Than Zaw.
- CR-002: Affected F07/F10; Requested By Thein Than Zaw; Date31 October2026; Change: Both members Edit/Delete shared memories. Reason: Shared ownership workflow. UI Impact: Both members actions/delete confirmation; Dev Impact: Membership authorization၊ Updated/Deleted By၊ concurrency checks; QA Impact: Partner allowed/non-member denied/conflict tests; Timeline Impact: Not estimated; Decision: Current Version; Confirmed By: Thein Than Zaw.
- CR-003: Affected All; Requested By Thein Than Zaw; Date31 October2026; Change: One couple/two accounts၊ server-readable private access၊ Local-first၊ In-App only. UI Impact: Signup limit/session and notification states; Dev Impact: Account cap/access control/local config/no Push; QA Impact: third-account/revocation/offline tests; Timeline Impact: Not estimated; Decision: Current Version; Confirmed By: Thein Than Zaw.

နောက်ပိုင်း Change တစ်ခုစီတွင်: Change ID; Affected Requirement/Feature; Requested By/Date; Requested Change; Reason; UI Impact; Development/API/DB Impact; QA Impact; Timeline Impact; Decision (Current Version/Future Version/Rejected); Approved By/Date ကို မှတ်တမ်းတင်ရမည်။ Accepted change တွင် Version တိုး၍ Tests/UI/contracts ပြန် align လုပ်ရန်။

### 8. Approval & Handover Status

Client Confirmation: D01–D06 = Confirmed by Thein Than Zaw,31 October2026. Full Feature Approval = Not yet recorded; Remaining = Q02/Q03/Q05/Q06 + Proposed Engineering Details. D01–D06 ကို Pending ဟု မသတ်မှတ်ရ။

UI Review: Existing Couple Design/Prototype ရှိသည်; UI Approved Features = None recorded; Revisions needed = Authentication, production Settings, Notification UI, state/error screens, real save/selection semantics; UI Link = Section1; Reviewed By/Date = [TBC].

Development Handover: This document = Review Candidate; Ready for Development Features = None formally recorded; Not Ready = F01–F10 pending relevant decisions/UI/contract finalization; Handover By/Received By/Date = [TBC]. Confirmed Owner Decisions ကို Implementation context အဖြစ် ထည့်ထားပြီးဖြစ်သည်။

QA Verification: Implementation မစစ်ရသေး; Passed Features = None; Failed/Retest = None recorded; Verified By/Date = [TBC]. Acceptance Criteria သည် ရေးပြီးသော Testable Expectations ဖြစ်ပြီး Test Passed အထောက်အထား မဟုတ်ပါ။

### 9. Revision History

| Version | Date | Updated By | Affected Feature | Change Summary | Status |
|---|---|---|---|---|---|
| 1.0 | 31 October 2026 | Codex | F01–F10 | Initial requirement draft and proposed technical direction | Draft |
| 1.1 | 31 October 2026 | Codex | F01/F02/F03/F04/F06/F07/F08/F09/F10 | D01–D06 incorporated; signup Relationship Date; shared-memory Edit/Delete; privacy/local/notification decisions; AC and change records updated | Owner Decisions Incorporated |
