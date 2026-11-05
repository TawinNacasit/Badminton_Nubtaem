# Score Protocol v1

JSON UTF-8 ขนาดเล็กสำหรับ Wear Engine P2P โดย Android เป็น authoritative state เมื่อเชื่อมต่อ

## SCORE_COMMAND

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

`action`: `INCREMENT`, `DECREMENT`, `UNDO`, `RESET_MATCH`, `START_NEXT_GAME`, `SWAP_SIDES` โดย `team` จำเป็นเฉพาะเพิ่ม/ลด

## SCORE_STATE

```json
{
  "protocolVersion": 1,
  "type": "SCORE_STATE",
  "matchId": "local-match-001",
  "revision": 13,
  "teamA": 18,
  "teamB": 16,
  "teamNameA": "Team A",
  "teamNameB": "Team B",
  "mode": "BADMINTON_21",
  "gameNumber": 1,
  "gamesWonA": 0,
  "gamesWonB": 0,
  "gameOver": false,
  "matchOver": false,
  "lastCommandId": "uuid-unique-per-action"
}
```

## กติกาซิงก์

1. ผู้ส่งสร้าง `commandId` ครั้งเดียวและใช้ค่าเดิมเมื่อ retry
2. Android รับคำสั่งเมื่อ `matchId` และ `baseRevision` ตรง current state เท่านั้น
3. เมื่อรับสำเร็จ Android เพิ่ม revision หนึ่งครั้ง เก็บ commandId ที่เคยรับ แล้วตอบ full state
4. commandId ซ้ำตอบ state เดิมโดยไม่ apply ซ้ำ
5. revision/match ไม่ตรงให้ตอบ authoritative full state และบันทึก conflict; client ห้ามเขียนทับเงียบ ๆ
6. Watch offline เก็บคิวสูงสุด 20 คำสั่งพร้อมแสดง pending; reconnect แล้ว replay ทีละรายการ หลังได้รับ state ของรายการก่อนหน้า
7. `UNDO` ใช้ history ของ authoritative reducer ไม่ใช่เดาคะแนนด้วยการลบหนึ่ง

ไฟล์ executable contract อยู่ที่ `shared/src/` และตัวอย่าง payload อยู่ที่ `shared/test-vectors.json`

