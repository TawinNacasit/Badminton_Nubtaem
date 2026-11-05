# Watch Build, Signing and Install

สถานะ: source prepared; ยังไม่ได้ทำบนอุปกรณ์จริง

1. เปิด `watch/` ใน DevEco Studio ที่มี Lite Wearable SDK
2. ตรวจ `bundleName` ให้ตรงกับ app ที่ลงทะเบียน
3. ดาวน์โหลด official Lite Wearable `wearengine.js`; ห้ามใช้ placeholder/ไฟล์จากแหล่งไม่ทราบที่มา
4. แก้ `PHONE_APP_PACKAGE_NAME`, `PHONE_APP_FINGERPRINT` และ `supportLists`
5. สร้าง/นำเข้า Huawei watch app development certificate และตั้ง signing config ผ่าน DevEco Studio โดยเก็บ key นอก repo
6. เปิด developer/debug mode และติดตั้ง HAP ด้วยวิธี “Run Your App/Service on a Lite Wearable” ของเอกสารที่ตรง DevEco/firmware ณ วันทดสอบ
7. บันทึก DevEco version, SDK version, WATCH FIT 4 firmware, Huawei Health version และผล ping/send/receive

ตำแหน่งเมนู/วิธี side-load อาจต่างตาม firmware/region จึงไม่ระบุคำสั่งที่ยังไม่ได้ยืนยัน ให้ใช้คู่มือ official ที่ DevEco version นั้นลิงก์ให้ และหยุดหากอุปกรณ์ไม่ปรากฏเป็น target ที่รองรับ

