# Android Pairing and Wear Engine

## 1. เตรียม Huawei service

1. สมัคร/ยืนยัน Huawei developer account
2. สร้าง Android phone app ใน Huawei developer console ด้วย package `com.nubtam.badmintonscore` หรือเปลี่ยน package ทั้งโปรเจกต์ให้ตรงของจริง
3. ใส่ SHA-256 fingerprint ของ certificate ที่ใช้ sign APK
4. สมัคร Wear Engine service และขอ device/communication capability ที่จำเป็น
5. รออนุมัติก่อนทดสอบ; `Scope unauthorized` แปลว่าการตั้งค่า/approval ยังไม่ครบ

## 2. ตั้งค่า peer โดยไม่ commit secret

เพิ่มใน `%USERPROFILE%\.gradle\gradle.properties` (ไฟล์นอก repo):

```properties
WEAR_PEER_PACKAGE=com.nubtam.badmintonscore.watch
WEAR_PEER_FINGERPRINT=SHA256_FINGERPRINT_FROM_SIGNED_WATCH_APP
```

ฝั่ง Watch แก้ `constants/constants.js` และ `config.json > supportLists` ให้เป็น package/fingerprint ของ Android ที่ signed จริง

## 3. Pair

1. ติดตั้ง Huawei Health จากแหล่ง Huawei ที่รองรับ region
2. ถ้าเป็น non-Huawei Android ให้ติดตั้ง HMS Core และอนุญาต Huawei Health ทำงาน/แสดงหน้าต่างเบื้องหลังตาม guide
3. Pair WATCH FIT 4 ผ่าน Huawei Health และตรวจว่า connected
4. ติดตั้ง signed Android APK และ signed watch HAP
5. เปิด Android app เลือก “Wear Engine”; adapter จะค้นหา bonded device ที่ connected แล้ว register receiver

Android app จะแสดง error แทน crash เมื่อยังไม่มี device/config และกลับไปใช้ปุ่ม “Mock” ได้เสมอ

