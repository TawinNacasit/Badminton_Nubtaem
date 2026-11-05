# Compatibility Gate

ตรวจล่าสุด: 4 พฤศจิกายน 2026 (Asia/Bangkok)

## VERIFIED FROM DOC / SAMPLE

| เรื่อง | สถานะและหลักฐาน |
|---|---|
| สถาปัตยกรรม | Huawei ระบุ Lite Wearable architecture และ official JS sample ใช้ HML/CSS/JS + P2P messaging |
| Android requirements | Huawei guide ระบุ Android 6.0+, `minSdk 23`, `compileSdk 34` (ขั้นต่ำตาม guide), Huawei Health; non-Huawei phone ต้องมี HMS Core และอนุญาต Huawei Health ทำงานเบื้องหลัง |
| Service onboarding | ต้องมี Huawei developer identity, phone app/App ID, package name + SHA-256 signing fingerprint ที่ตรงกัน และ Wear Engine service approval ก่อน debug API |
| Android SDK | Huawei integration guide แสดง Maven repo และ `com.huawei.hms:wearengine:5.0.2.306` |
| P2P APIs | Official/community samples ใช้ `HiWear.getDeviceClient`, `HiWear.getP2pClient`, `P2pClient`, `Message`, `Receiver`, `SendCallback` |
| Android 11+ | targetSdk 30+ ต้องประกาศ `<queries><package android:name="com.huawei.health"/></queries>` ซึ่งเพิ่มแล้วใน manifest |
| Lite SDK | Official sample ให้ดาวน์โหลด Wear Engine JS SDK และวางใน source ก่อน build; repo นี้ไม่สร้างไฟล์ดังกล่าวเอง |
| Windows | หน้า DevEco Studio ปัจจุบันระบุ Windows 10/11 64-bit, RAM 16 GB+, disk 100 GB+, 1280×800+ |
| Fit 4 route | Community Explore-In-HMOS sample ระบุ Huawei Sport (Lite) Fit 3/4 และใช้ framework `5.0.0(12)` / DevEco Studio 6.0.0; เป็นหลักฐานประกอบ ไม่ใช่การรับรอง configuration ของเครื่องนี้ |

แหล่งอ้างอิง:

- [Huawei Wear Engine](https://developer.huawei.com/consumer/en/hms/huawei-wearengine)
- [Huawei Wear Engine getting started](https://developer.huawei.com/consumer/es/doc/connectivity-Guides/dev-process-0000001051058039)
- [Huawei Wear Engine Android SDK integration](https://developer.huawei.com/consumer/cn/doc/connectivity-guides/integrating-phone-sdk-0000001051137958)
- [Huawei official Lite Wearable sample](https://github.com/HMS-Core/hms-wear-engine-demo/tree/master/Lite%20wearable%20devices)
- [Huawei wearable development](https://developer.huawei.com/consumer/en/multidevice/wearables/)
- [Community Lite Wearable P2P sample](https://github.com/Explore-In-HMOS/sportwatch-wear-engine-lite-wearable-to-mobile)
- [Community Android Compose sample](https://github.com/Explore-In-HMOS/wear-engine-mobile-to-watch)
- [DevEco Studio requirements](https://developer.huawei.com/consumer/en/deveco-studio/)

## VERIFIED LOCALLY

- Node.js v24 และ npm ใช้ติดตั้ง/build mock ได้
- Unit tests ของ reducer/sync ผ่าน 9 tests
- Vite production build ผ่าน

## UNVERIFIED — ต้องพิสูจน์บนเครื่องจริง

- WATCH FIT 4 firmware/region ของผู้ใช้เปิด developer/test-install และ Wear Engine app-to-app ได้หรือไม่
- DevEco `5.0.0(12)` project profile และ SDK JS ที่ดาวน์โหลดปัจจุบันเข้ากับ firmware จริงหรือไม่
- Huawei account ของผู้ใช้สมัคร service ได้และ permission scope ถูกอนุมัติหรือไม่
- Android package/App ID/signing fingerprint และ peer fingerprint จับคู่ครบหรือไม่
- ขนาด payload/lifecycle/retry behavior ของ firmware จริง
- watch source build/install, ping, message round trip, background/reconnect และ latency จริง

ห้ามเปลี่ยนรายการข้างบนเป็น VERIFIED ON DEVICE จนมีผลทดสอบและบันทึก version/อุปกรณ์

## ข้อมูลที่ยังต้องขอจากผู้ใช้ก่อน hardware test

- ยืนยันโทรศัพท์เป็น Android, รุ่น, Android version และ Huawei/non-Huawei
- Huawei Health version และ region/account
- WATCH FIT 4 firmware version
- DevEco Studio version ที่ติดตั้งได้
- Huawei developer/Wear Engine approval status

