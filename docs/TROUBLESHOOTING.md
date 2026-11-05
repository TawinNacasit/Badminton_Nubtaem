# Troubleshooting

## Mock

- Port 5173 ถูกใช้: `npm run dev -- --port 5174`
- state เก่าค้าง: เปิด DevTools > Application > Local Storage แล้วลบ `badminton-score-state`
- dependency เพี้ยน: ลบ `mock/node_modules` และติดตั้งใหม่ (อย่าลบโฟลเดอร์อื่น)

## Android / Wear Engine

- `Scope unauthorized`: ตรวจ service approval, App ID, package, SHA-256 และ signed APK ให้ตรง; Huawei FAQ ระบุ cache อาจ refresh เป็นรอบ หาก approval เพิ่งผ่านให้ทำตาม FAQ ปัจจุบัน
- ไม่พบ Huawei Health บน Android 11+: ตรวจ `<queries>` ใน manifest (เพิ่มแล้ว)
- ไม่พบ bonded device: pair ผ่าน Huawei Health, อัปเดต Huawei Health/HMS Core และอนุญาต background บน non-Huawei phone
- `No connected wearable`: ใช้ Mock ได้ก่อน แล้วตรวจ `isConnected`/device list
- peer ping/send ไม่ผ่าน: package/fingerprint สองฝั่งต้องอ้างถึง app ฝั่งตรงข้ามที่ signed จริง
- result code ไม่ทราบ: ใช้ Huawei `WearEngineErrorCode.getErrorMsgFromCode`/result-code guide รุ่นปัจจุบัน อย่าตีความจากตัวเลขเอง

## Watch

- import `wearenginesdk/wearengine` ไม่พบ: ยังไม่ได้ดาวน์โหลด official Lite Wearable JS SDK
- DevEco sync ไม่ผ่าน: สร้าง Lite Wearable template ด้วย SDK ที่ติดตั้ง แล้วคัดลอก `entry/src/main`; project profile เป็นส่วนที่ขึ้นกับ IDE version
- install ไม่ได้: ตรวจ developer mode, certificate/profile, bundle name, firmware/region และ supported target
- reconnect แล้ว pending ไม่หมด: เก็บ log ของทุก `commandId`, revision และ full state; อย่ากด reset ก่อนเก็บหลักฐาน

