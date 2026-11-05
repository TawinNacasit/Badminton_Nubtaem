# Project Brief for Codex — Huawei WATCH FIT 4 Badminton Live Score

> **คำสั่งหลัก:** ให้ Codex ทำงานเป็น Senior Full-stack / Android / HarmonyOS Lite Wearable Developer สร้างโปรเจกต์ที่รันได้จริงตามเอกสารนี้ โดยตรวจสอบเอกสาร SDK และข้อจำกัดของอุปกรณ์ก่อนใช้ API ใด ๆ อย่าสร้างชื่อ API หรือ SDK ขึ้นเอง
>
> **อุปกรณ์เป้าหมาย:** HUAWEI WATCH FIT 4 + โทรศัพท์ **Android (สมมติฐานเริ่มต้น—ยังไม่ได้ยืนยันจากผู้ใช้)** หากโทรศัพท์เป็น iPhone ให้หยุดส่วนการเชื่อมต่อจริงและอธิบายข้อจำกัด/ทางเลือกก่อน เพราะ Wear Engine ฝั่งมือถือรองรับ Android ตามเอกสาร Huawei

## 1. วัตถุประสงค์

สร้างแอปส่วนตัวสำหรับนับคะแนนแบดมินตันโดยกดเพิ่ม/ลดคะแนนบน Huawei WATCH FIT 4 แล้วคะแนนอัปเดตบนแอปมือถือ Android ที่จับคู่ไว้แบบเกือบเรียลไทม์ โดยสามารถเพิ่มจอแสดงคะแนนบนเว็บ/มือถือเครื่องอื่นผ่านเครือข่ายในเฟสต่อไปได้ ไม่จำเป็นต้องเผยแพร่สู่ AppGallery ในช่วงพัฒนา แต่ต้องใช้วิธีติดตั้งและลงนามแอปทดสอบที่ Huawei รองรับอย่างถูกต้อง

**MVP ที่ต้องสำเร็จ:**
1. มีหน้าจอนับแต้มสองฝั่ง (Team A / Team B) บนนาฬิกา
2. กด `+1`, `-1`, `Undo` ได้และป้องกันคะแนนติดลบ
3. โทรศัพท์ Android แสดงคะแนนเดียวกันทันทีเมื่อรับข้อความจากนาฬิกา
4. หลุดการเชื่อมต่อแล้วสถานะต้องบอกชัดเจน และซิงก์กลับได้เมื่อเชื่อมใหม่
5. มีโหมดจำลอง (mock transport) เพื่อทดสอบ UI/กติกาได้โดยยังไม่มีนาฬิกาหรือ SDK

## 2. แหล่งอ้างอิงที่ต้องอ่านก่อนเขียน integration

- Huawei Wear Engine Official: https://developer.huawei.com/consumer/en/hms/huawei-wearengine
- Huawei Wear Engine Codelab: https://developer.huawei.com/consumer/en/codelab/WearEngine/
- Huawei wearable development: https://developer.huawei.com/consumer/en/multidevice/wearables/get-started/
- Huawei official JS Lite Wearable sample: https://github.com/HMS-Core/hms-wear-engine-demo/tree/master/Lite%20wearable%20devices
- Community sample ที่ README ระบุรองรับ Huawei Fit 3/4: https://github.com/Explore-In-HMOS/sportwatch-wear-engine-lite-wearable-to-mobile
- ตัวอย่าง Android Companion (ค้นรายละเอียดการใช้งาน SDK เพิ่ม): https://github.com/Explore-In-HMOS/wear-engine-mobile-to-watch

**สำคัญ:** community sample เป็นตัวอย่าง ไม่ใช่การรับประกันว่าโทรศัพท์/เฟิร์มแวร์/บัญชีนักพัฒนาทุกชุดจะติดตั้งและสื่อสารได้จริง ต้องพิสูจน์ด้วยอุปกรณ์จริง ห้ามสมมติว่า WATCH FIT 4 เป็น Wear OS หรือ Smart Wearable แบบ ArkTS; ให้ตรวจสอบเส้นทาง **HarmonyOS Lite Wearable (JavaScript/HML/CSS)** สำหรับรุ่นนี้ก่อน ข้อมูลการเซ็นชื่อ, package name, fingerprint และการขอสิทธิ์ Wear Engine ให้ยึดเอกสารปัจจุบัน

## 3. เทคโนโลยีและโครงสร้างที่ต้องการ

- `watch/` — แอป Huawei Lite Wearable: JavaScript + HML + CSS ตาม SDK/DevEco Studio รุ่นที่เข้ากันได้
- `android/` — Android companion: Kotlin, Jetpack Compose (หากตัวอย่าง Wear Engine ที่ตรวจสอบแล้วรองรับ), Wear Engine SDK
- `shared/` — นิยาม message schema, กติกาคะแนน และ test fixtures ที่แปลงใช้งานข้ามแพลตฟอร์มได้ (ไม่บังคับแชร์ binary)
- `mock/` — หน้าเว็บ/โปรแกรมจำลองการกดคะแนนจากนาฬิกาและจอมือถือสำหรับทดสอบเร็วบน VS Code ด้วย JavaScript/TypeScript
- `docs/` — การติดตั้งเครื่องมือ, signing, permissions, ขั้นตอนทดสอบจริง, limitations และ troubleshooting
- `server/` — **เฟส 2 เท่านั้น**: Node.js + WebSocket หรือ Socket.IO + optional Angular viewer เพื่อเผยแพร่คะแนนให้หลายเครื่อง ไม่ทำจนกว่าจะได้ MVP ที่เชื่อม watch ↔ Android

ตัวอย่างโครงสร้าง:

```text
badminton-watch-score/
├── README.md
├── docs/
│   ├── COMPATIBILITY.md
│   ├── SETUP_WINDOWS.md
│   ├── WATCH_INSTALL.md
│   ├── ANDROID_PAIRING.md
│   └── TROUBLESHOOTING.md
├── watch/
├── android/
├── shared/
│   ├── protocol.md
│   └── test-vectors.json
├── mock/
└── server/               # เฟส 2 ยังไม่ต้อง implement
```

## 4. ฟีเจอร์และ UI/UX

### บนนาฬิกา
- หน้าหลัก: `TEAM A`, `TEAM B`, คะแนนใหญ่ มองง่าย ขณะเล่นกีฬา
- ปุ่ม `+1` แยกฝั่ง ขนาดกดง่าย สีต่างกันชัดเจน
- `-1`, `Undo`, `New Game` อยู่หน้าเมนูรองหรือกดค้างเพื่อกันกดผิด
- ยืนยันก่อน `New Game` หรือ reset match
- สัญลักษณ์เชื่อมต่อ: Connected / Disconnected / Syncing
- ออกแบบสำหรับจอเล็ก ทดสอบขนาดจริงของ WATCH FIT 4 ห้าม hardcode resolution โดยไม่มีการตรวจสอบ
- ไม่ใช้บริการตำแหน่ง, ข้อมูลสุขภาพ, ไมโครโฟน หรือสิทธิ์ไม่จำเป็น

### บน Android
- หน้าจอ Live Score คะแนนสองฝั่งตัวใหญ่ รองรับแนวตั้งและแนวนอน
- มีการแสดงสถานะเชื่อมต่อและเวลาซิงก์ล่าสุด
- ตั้งชื่อทีมได้, สลับฝั่งได้, เริ่มเกมใหม่ได้
- ควบคุมคะแนนจากมือถือได้เช่นกัน **โดยต้องส่ง state กลับไปนาฬิกา** และมีวิธีป้องกันข้อขัดแย้ง
- เก็บ match ล่าสุดใน local storage; เปิดแอปใหม่แล้วไม่สูญหาย
- รองรับ dark mode และปุ่มที่กดง่ายขณะเล่น

### กติกาแบดมินตัน (แยกจาก MVP นับแต้มพื้นฐาน)
- ค่าตั้งต้น 21 คะแนน, ชนะต้องห่างอย่างน้อย 2, ที่ 29–29 ใครได้ 30 ก่อนชนะ
- Best of 3 games, แสดงเกมที่ชนะ, สลับข้างได้
- ให้มี setting สำหรับ `Simple Counter` (ไม่บังคับกติกา) กับ `Badminton 21` (ตรวจจบเกม)
- เมื่อจบเกมห้ามเพิ่มคะแนนต่อโดยไม่ยืนยันเริ่มเกมใหม่
- ไม่ทำระบบนับคะแนนอัตโนมัติจากเซ็นเซอร์

## 5. การสื่อสารและการซิงก์ (สำคัญที่สุด)

**เส้นทางจริง:** Watch App ↔ Huawei Wear Engine P2P ↔ Android App ที่จับคู่กับนาฬิกาผ่าน Huawei Health (รายละเอียดการเชื่อมต่อให้ยึด SDK จริง; อย่าคิดว่าเป็น Bluetooth socket ทั่วไป)

กำหนด payload ใน `shared/protocol.md` เช่น:

```json
{
  "protocolVersion": 1,
  "type": "SCORE_COMMAND",
  "matchId": "local-match-001",
  "commandId": "uuid-unique-per-action",
  "senderId": "watch-01",
  "baseRevision": 12,
  "action": "INCREMENT",
  "team": "A",
  "timestamp": 1793780000000
}
```

และ state response:

```json
{
  "protocolVersion": 1,
  "type": "SCORE_STATE",
  "matchId": "local-match-001",
  "revision": 13,
  "teamA": 18,
  "teamB": 16,
  "gameNumber": 1,
  "gamesWonA": 0,
  "gamesWonB": 0,
  "lastCommandId": "uuid-unique-per-action"
}
```

**แนวทางความถูกต้อง:**
- ให้ **Android เป็น authoritative state** เมื่อ connected; นาฬิกาส่ง command และ Android ตอบ state + revision
- ใช้ `commandId` ป้องกันการเพิ่มคะแนนซ้ำจาก retry
- ใช้ `revision` และ `matchId` ตรวจข้อความเก่า/สลับแมตช์
- เมื่อ disconnected ให้ระบุ offline ชัดเจน: เลือก **queue คำสั่งที่ยังไม่ ack แบบมีขีดจำกัด** และแสดง pending state; reconnect แล้ว sync อย่างปลอดภัย ห้ามบวกคะแนนซ้ำ
- เมื่อเกิด concurrent updates หรือ baseRevision ไม่ตรง ให้ส่ง full authoritative state กลับมา แล้วออกแบบ conflict handling อย่างตรวจสอบได้; ห้ามเงียบ ๆ เขียนทับคะแนน
- ปุ่ม Undo ต้องย้อนคำสั่งล่าสุดที่ถูกยอมรับตาม history เดียวกันทั้งสองฝั่ง ไม่ใช่ลดเลขโดยเดาสุ่ม
- หาก Wear Engine ส่งข้อความได้จำกัดขนาดหรือมี lifecycle เฉพาะ ต้องปรับ schema และ handling ตาม API จริง
- แสดง sync status ไม่อ้างว่า realtime เมื่อ connection ไม่พร้อม

## 6. ลำดับพัฒนา — ทำทีละเฟส

### Phase 0 — Feasibility / Compatibility Gate (ต้องทำก่อน coding สำหรับเครื่องจริง)
1. ตรวจสอบ WATCH FIT 4, firmware ที่จำเป็น, Lite Wearable development, version DevEco Studio และ Windows support จากแหล่งอ้างอิง
2. ตรวจสอบการเปิด developer mode, การลงนามแอป, การติดตั้ง HAP ทดสอบผ่านวิธีทางการ และ account/บริการที่ต้องสมัคร
3. ตรวจสอบ Wear Engine Android requirements, Huawei Health, pairing, package name / fingerprint / service authorization, สิทธิ์ที่จำเป็น และตัวอย่าง P2P messaging
4. ระบุชัดว่าอะไร `VERIFIED FROM DOC`, `VERIFIED ON DEVICE`, `UNVERIFIED` โดยห้ามกล่าวว่าทำงานบนเครื่องจริงหากยังไม่ได้ลอง
5. สร้าง `docs/COMPATIBILITY.md` พร้อมลิงก์และรายการข้อมูลที่ยังต้องถามผู้ใช้ เช่น Android รุ่นใด, WATCH FIT 4 firmware อะไร

### Phase 1 — Mock runnable MVP (ทำทันที)
1. ทำ `mock/` เป็นเว็บจำลองหน้าจอนาฬิกา + มือถืออยู่ในหน้าต่างเดียวกัน
2. ใช้ state management และโปรโตคอลจริงในโหมดจำลอง ทดสอบ add/subtract/undo/reset/team names/match rules
3. เพิ่มการจำลอง disconnect/reconnect, duplicate message, out-of-order, conflict
4. มีคำสั่ง `npm install`, `npm run dev`, `npm test` ที่ใช้ได้จริง
5. เขียน unit tests สำหรับ scoring reducer + idempotency + reconciliation

### Phase 2 — Android Companion
1. สร้าง Android project ที่ build ได้พร้อม Kotlin + Compose
2. แสดง scoreboard ด้วย mock transport ก่อน
3. สร้าง `Transport` interface และ Wear Engine implementation ที่ใช้ API ตามตัวอย่างจริง โดยแยก configuration placeholders อย่างชัดเจน
4. อธิบาย signing credentials ที่ผู้ใช้ต้องตั้งเอง (ห้ามใส่ secret ลง git)
5. หลังขอสิทธิ์และเชื่อมอุปกรณ์ได้ ให้ทดสอบส่งข้อความจาก mock/watch และอัปเดตหน้าจอ Android

### Phase 3 — Watch Lite Wearable
1. สร้างโปรเจกต์ตาม Lite Wearable template **ที่รองรับ FIT 4** โดยอิงโครงสร้างจาก sample ที่ตรวจสอบ
2. ทำหน้าคะแนน + interaction ด้วย JS/HML/CSS และส่ง SCORE_COMMAND
3. รับ SCORE_STATE แล้วอัปเดต UI/แก้สถานะ sync
4. ถ้าการ build/install ต้องทำผ่าน DevEco Studio แทน VS Code ให้สร้าง source และคู่มือ import/build แบบละเอียด ไม่อ้างว่า VS Code build/watch deploy ได้เอง
5. ตรวจสอบ watch <-> Android ด้วยเครื่องจริง ถ้ายังไม่มีเครื่องให้จบที่ source + checklist และระบุว่ายังไม่ได้ทดสอบจริง

### Phase 4 — Optional Multi-screen
- Android ส่ง state ไป Backend ผ่าน WebSocket
- Angular web viewer สำหรับเปิดบนจอใหญ่/ทีวีหรือมือถือหลายเครื่องด้วย match code
- ผู้ชมเป็น read-only; ป้องกันการแก้คะแนนโดยไม่ได้รับอนุญาต
- Offline watch↔phone ต้องยังใช้ได้แม้ไม่มีอินเทอร์เน็ต (ตามความสามารถของ SDK ที่ตรวจสอบแล้ว)

## 7. Acceptance Criteria

- [ ] จำลองกด Watch +1 Team A แล้ว Mock Mobile อัปเดตอย่างถูกต้อง
- [ ] Android companion แสดงข้อมูลจาก transport ได้ และไม่ crash เมื่อไม่มีนาฬิกา
- [ ] คะแนนไม่ติดลบ
- [ ] Undo ทำงานถูกต้องอย่างน้อย 10 คำสั่งย้อนหลัง
- [ ] กดเร็วหลายครั้งไม่ทำคะแนนหาย/เพิ่มซ้ำ
- [ ] ส่ง command ซ้ำหลายรอบไม่เพิ่มคะแนนซ้ำ
- [ ] disconnect/reconnect ไม่ทำคะแนนหายโดยเงียบ ๆ และไม่มีการ sync ซ้ำ
- [ ] กติกา 20–20, 29–29, 30 คะแนน และ best-of-3 มี tests
- [ ] รีสตาร์ต Android แล้วยังเห็นสถานะเกมล่าสุด
- [ ] มี README ภาษาไทย บอกติดตั้ง/รัน/ทดสอบ/ข้อจำกัด
- [ ] เมื่อยังไม่ได้ทดสอบจริง ต้องไม่รายงานว่า Watch FIT 4 เชื่อมสำเร็จ

## 8. ข้อกำหนด Coding/Quality

- เขียน source code จริง ไม่ใช่ pseudocode และอย่าตอบแค่ roadmap
- ทำงานเป็นขั้น ตรวจสอบ build/test หลังแก้ โดยไม่ลบโค้ดผู้ใช้อื่นถ้ามี
- Dependency versions ให้ตรวจสอบจากเอกสารที่เข้าถึงได้ก่อนเลือก ห้ามสร้าง API giảหรือแพ็กเกจขึ้นเอง
- ถ้า SDK/Signing/Huawei Account ต้องทำด้วยมือ ให้เพิ่มขั้นตอนและตำแหน่งคลิกในคู่มือ พร้อมแยก tasks ที่ Codex ทำแทนไม่ได้
- ห้าม commit keystore, private key, API token, fingerprint ที่เป็นข้อมูลจริง หรือ `.env` ที่มี secrets
- โค้ด transport ต้อง mock ได้ เพื่อให้ทำงานต่อแม้ไม่มี environment สำหรับ SDK
- สรุปทุกเฟส: สิ่งที่ทำเสร็จ, คำสั่งทดสอบและผล, ไฟล์สำคัญ, สิ่งที่ติดเงื่อนไขอุปกรณ์จริง

## 9. สิ่งที่ให้ Codex ทำในรอบแรก — เริ่มเลย

1. สำรวจโฟลเดอร์ workspace ปัจจุบัน; หากว่างให้ตั้งชื่อโปรเจกต์ `badminton-watch-score` และสร้างโครงสร้างข้างต้น
2. อ่านลิงก์อ้างอิงเท่าที่เข้าถึงได้ ถ้าอินเทอร์เน็ต/SDK เข้าไม่ได้ ให้ระบุชัดและยังทำ mock ได้
3. สร้าง `docs/COMPATIBILITY.md`, `README.md` และ `shared/protocol.md`
4. **ลงมือเขียน `mock/` ให้ใช้งานได้จริงก่อน** พร้อมหน้าจอจำลอง Watch และ Android, ปุ่มกดคะแนน และสถานะ sync
5. เขียน tests แล้วเรียกทดสอบ; แก้จนผ่านหากเครื่องมือพร้อม
6. จากนั้นเตรียม Android Companion และ Watch source โดยไม่กุ API จริงขึ้นมาเอง
7. ตอบสรุปภาษาไทย พร้อมคำสั่งรันบน Windows 11 และสิ่งที่เจ้าของเครื่องต้องเตรียม

**ข้อควรระวัง:** ขอข้อมูลเพิ่มเฉพาะเรื่องที่จำเป็นต่อการเชื่อมกับฮาร์ดแวร์จริง (เช่น มือถือ Android/iPhone, firmware, developer account) แต่ **อย่าหยุดงาน mock MVP เพื่อรอคำตอบ**
