# Nubtaem

เวอร์ชันปัจจุบัน: **0.2.0** — แอปโทรศัพท์แสดงชื่อ **Nubtaem**

โปรเจกต์นับคะแนนแบดมินตันสำหรับ HUAWEI WATCH FIT 4 และ Android companion โดย Android เป็นแหล่งข้อมูลหลัก (authoritative state) และนาฬิกาส่งคำสั่งพร้อม `commandId`/`revision` เพื่อป้องกันแต้มซ้ำและตรวจ conflict

> สถานะ ณ 4 พฤศจิกายน 2026: **Mock MVP รันและทดสอบแล้วใน Windows 11**; Android/Watch เป็น source ที่เตรียมสำหรับ import แต่ **ยังไม่ได้ build ด้วย Android SDK/DevEco Studio และยังไม่ได้ทดสอบบน WATCH FIT 4 จริง**

## เริ่มเร็วที่สุดบน Windows 11

```powershell
cd mock
npm install
npm run dev
```

เปิด URL ที่ Vite แสดง (ปกติ `http://localhost:5173`) แล้วทดลอง:

- เพิ่ม/ลด/Undo, ตั้งชื่อทีม, สลับฝั่ง และเริ่มแมตช์ใหม่
- Badminton 21: ดิวซ์, แต้มสูงสุด 30 และ best-of-3
- กด “ตัดการเชื่อมต่อ” แล้วกดคะแนนจากนาฬิกา จากนั้นเชื่อมใหม่
- ส่งข้อความซ้ำและจำลอง revision conflict

ทดสอบและ build mock:

```powershell
cd mock
npm test
npm run build
```

## โครงสร้าง

- `mock/` — เว็บจำลอง Watch + Android และ network controls ใช้งานได้จริง
- `shared/` — schema, scoring reducer, authoritative sync engine และ test vectors
- `android/` — Kotlin/Jetpack Compose; เริ่มด้วย mock transport และมี Wear Engine adapter
- `watch/` — Lite Wearable JavaScript/HML/CSS; ต้องเติม SDK/signing config ก่อน build
- `docs/` — compatibility, setup, pairing, install และ troubleshooting
- `server/` — ยังไม่ทำตามเงื่อนไข Phase 2 หลังพิสูจน์ watch ↔ Android

## สิ่งที่ยืนยันได้แล้ว

- Huawei ระบุเส้นทาง Lite Wearable เป็น JavaScript และ Wear Engine P2P ใช้ส่งข้อความสองทาง ไม่ใช่ Wear OS/Bluetooth socket ทั่วไป
- เอกสาร Huawei ฝั่ง Android ระบุ `minSdkVersion 23`, `compileSdkVersion 34` เป็นขั้นต่ำใน guide ปัจจุบัน, Huawei Health และ HMS Core สำหรับโทรศัพท์ non-Huawei
- Huawei repository แนะนำ `com.huawei.hms:wearengine:5.0.2.306`; โปรเจกต์ Android pin รุ่นนี้
- Community sample ระบุ Fit 3/4 เป็น Lite Wearable ที่รองรับ แต่ยังถือเป็น **UNVERIFIED ON THIS DEVICE** จนกว่าจะทดสอบ firmware/region/account จริง

อ่าน [COMPATIBILITY.md](docs/COMPATIBILITY.md) ก่อนเชื่อมอุปกรณ์ และ [SETUP_WINDOWS.md](docs/SETUP_WINDOWS.md) สำหรับเครื่องมือทั้งหมด

## ข้อมูลที่เจ้าของเครื่องต้องเตรียม

1. รุ่น/เวอร์ชัน Android และยืนยันว่าใช้ Android (โปรเจกต์รอบนี้ไม่ได้ทำ iOS)
2. WATCH FIT 4 firmware, Huawei Health version และ region ของ Huawei ID
3. Huawei developer account ที่ยืนยันตัวตนและได้รับอนุมัติ Wear Engine
4. package name, App ID และ SHA-256 fingerprint ของ signing certificate ที่ต้องตรงกัน
5. DevEco Studio และ Android Studio/Android SDK ตามคู่มือ

ห้าม commit keystore, private key หรือ secret ลง repository; `.gitignore` ป้องกันนามสกุล keystore ที่พบบ่อยไว้แล้ว

