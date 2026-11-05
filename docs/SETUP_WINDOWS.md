# Setup on Windows 11

## Mock MVP

ติดตั้ง Node.js LTS/Current แล้วรัน:

```powershell
cd mock
npm install
npm test
npm run dev
```

## Android

1. ติดตั้ง Android Studio รุ่นที่รองรับ AGP 9.4, JDK 17 และ Android SDK API 37
2. เปิดโฟลเดอร์ `android/` แล้วรอ Gradle sync
3. โปรเจกต์ใช้ Compose BOM `2026.09.00`, AGP `9.4.0` และ Huawei Wear Engine `5.0.2.306`
4. ถ้า checkout นี้ยังไม่มี Gradle wrapper ให้ Android Studio ใช้/สร้าง wrapper `9.6.0` ตาม compatibility ของ AGP 9.4
5. รันแอปโดยไม่ตั้งค่า Huawei ได้: แอปเริ่มที่ `MockTransport`

Build จาก terminal หลัง Android Studio สร้าง wrapper และ SDK พร้อม:

```powershell
cd android
.\gradlew.bat assembleDebug
```

เครื่องที่ใช้สร้าง repo นี้ไม่มี Android SDK/Gradle จึงยังไม่ได้ยืนยัน build ดังกล่าว

## DevEco Studio / Watch

1. ติดตั้ง DevEco Studio จากหน้า Huawei (Windows 10/11 64-bit; เอกสารปัจจุบันแนะนำ RAM 16 GB และพื้นที่ 100 GB)
2. ติดตั้ง HarmonyOS Lite Wearable SDK ที่รองรับ WATCH FIT 4/firmware จริง
3. เปิด `watch/` แล้วตรวจ project profile กับ Lite Wearable template ของ DevEco รุ่นนั้น
4. ดาวน์โหลด **Wear Engine Lite Wearable JavaScript SDK จาก Huawei** และวาง `wearengine.js` ใน `watch/entry/src/main/js/MainAbility/wearenginesdk/`
5. ทำ signing ตาม [WATCH_INSTALL.md](WATCH_INSTALL.md)

DevEco project metadata เปลี่ยนได้ตามรุ่น IDE; ให้ยึด template ที่ IDE สร้างและย้าย `entry/src/main` เข้าไปถ้า sync profile ไม่ผ่าน อย่าแก้ด้วย API ที่เดาขึ้นเอง

