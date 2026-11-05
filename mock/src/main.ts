import "./styles.css";
import { makeCommand, type ScoreAction, type ScoreCommand, type ScoreStateMessage, type Team } from "../../shared/src/protocol";
import { createInitialState, reduceScore, type MatchState } from "../../shared/src/scoring";
import { AuthoritativeScoreEngine } from "../../shared/src/sync-engine";

type Connection = "CONNECTED" | "DISCONNECTED" | "SYNCING" | "CONFLICT";

const saved = localStorage.getItem("badminton-score-state");
let initial: MatchState;
try {
  initial = saved ? { ...createInitialState(), ...JSON.parse(saved), history: [] } : createInitialState();
} catch {
  initial = createInitialState();
}

const engine = new AuthoritativeScoreEngine(initial);
let watchState: MatchState = structuredClone(initial);
let connection: Connection = "CONNECTED";
let pending: ScoreCommand[] = [];
let lastSync = Date.now();
let eventLog: string[] = ["พร้อมรับคำสั่ง — Android เป็น authoritative state"];
let lastAccepted: ScoreCommand | undefined;

const app = document.querySelector<HTMLElement>("#app")!;

function log(message: string): void {
  eventLog = [`${new Date().toLocaleTimeString("th-TH")}  ${message}`, ...eventLog].slice(0, 7);
}

function persist(): void {
  localStorage.setItem("badminton-score-state", JSON.stringify(engine.state));
}

function acceptState(message: ScoreStateMessage): void {
  watchState = { ...watchState, ...message, history: engine.state.history };
  lastSync = Date.now();
  persist();
}

function send(command: ScoreCommand, optimistic = true): void {
  if (connection === "DISCONNECTED") {
    if (pending.length >= 20) {
      log("คิวเต็ม 20 รายการ — ไม่รับคำสั่งเพิ่ม");
      render();
      return;
    }
    pending.push(command);
    if (optimistic) watchState = reduceScore(watchState, command);
    log(`เก็บ ${command.action} ไว้ในคิว (${pending.length})`);
    render();
    return;
  }

  const result = engine.handle(command);
  if (result.kind === "ACK") {
    lastAccepted = command;
    acceptState(result.state);
    log(`ACK ${command.action} · revision ${result.state.revision}`);
  } else if (result.kind === "DUPLICATE") {
    acceptState(result.state);
    log(`ตัดข้อความซ้ำ ${command.commandId.slice(0, 8)}`);
  } else if (result.kind === "CONFLICT") {
    connection = "CONFLICT";
    acceptState(result.state);
    log("พบ revision conflict — โหลด authoritative state กลับมาแล้ว");
    setTimeout(() => { connection = "CONNECTED"; render(); }, 900);
  } else {
    log(`ปฏิเสธคำสั่ง: ${result.detail}`);
  }
  render();
}

function issue(action: ScoreAction, team?: Team, senderId = "watch-01"): void {
  send(makeCommand({
    matchId: engine.state.matchId,
    senderId,
    baseRevision: engine.state.revision + (connection === "DISCONNECTED" ? pending.length : 0),
    action,
    team,
  }));
}

async function reconnect(): Promise<void> {
  connection = "SYNCING";
  render();
  await new Promise((resolve) => setTimeout(resolve, 350));
  const queued = [...pending];
  pending = [];
  let applied = 0;
  for (const original of queued) {
    const rebased = { ...original, matchId: engine.state.matchId, baseRevision: engine.state.revision };
    const result = engine.handle(rebased);
    if (result.kind === "ACK" || result.kind === "DUPLICATE") {
      applied += result.kind === "ACK" ? 1 : 0;
      acceptState(result.state);
    } else {
      acceptState(result.state);
      log(`หยุด replay เพราะ ${result.kind}`);
      break;
    }
  }
  connection = "CONNECTED";
  log(`เชื่อมต่อแล้ว · replay สำเร็จ ${applied}/${queued.length}`);
  render();
}

function statusText(): string {
  if (connection === "DISCONNECTED") return `ออฟไลน์ · รอส่ง ${pending.length}`;
  if (connection === "SYNCING") return "กำลังซิงก์";
  if (connection === "CONFLICT") return "แก้ conflict แล้ว";
  return "เชื่อมต่อแล้ว";
}

function scorePanel(state: MatchState, device: "watch" | "phone"): string {
  const disabled = state.gameOver ? "disabled" : "";
  return `
    <div class="score-grid ${device}">
      <section class="team team-a">
        <span>${escapeHtml(state.teamNameA)}</span>
        <strong>${state.teamA}</strong>
        <button data-action="INCREMENT" data-team="A" ${disabled} aria-label="เพิ่มคะแนน ${escapeHtml(state.teamNameA)}">+1</button>
      </section>
      <section class="team team-b">
        <span>${escapeHtml(state.teamNameB)}</span>
        <strong>${state.teamB}</strong>
        <button data-action="INCREMENT" data-team="B" ${disabled} aria-label="เพิ่มคะแนน ${escapeHtml(state.teamNameB)}">+1</button>
      </section>
    </div>`;
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>'"]/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[char]!);
}

function render(): void {
  const state = engine.state;
  const winner = state.matchOver ? (state.gamesWonA === 2 ? state.teamNameA : state.teamNameB) : "";
  app.innerHTML = `
    <header class="lab-header">
      <div>
        <p class="kicker">Badminton score transport lab</p>
        <h1>แต้มเดียวกัน<br>ทุกหน้าจอ</h1>
      </div>
      <div class="court-mark" aria-hidden="true"><i></i><i></i><i></i></div>
      <div class="connection ${connection.toLowerCase()}"><span></span>${statusText()}</div>
    </header>

    <section class="simulators">
      <article class="device watch-shell">
        <div class="device-heading"><span>Watch simulation</span><b>เกม ${watchState.gameNumber}</b></div>
        <div class="watch-screen">
          <div class="watch-status"><span class="dot ${connection.toLowerCase()}"></span>${statusText()}<b>${watchState.gamesWonA}–${watchState.gamesWonB}</b></div>
          ${scorePanel(watchState, "watch")}
          <div class="compact-actions">
            <button data-watch-action="DECREMENT" data-team="A">− A</button>
            <button data-watch-action="UNDO">Undo</button>
            <button data-watch-action="DECREMENT" data-team="B">− B</button>
          </div>
          ${watchState.gameOver ? `<button class="next-game" data-watch-action="${watchState.matchOver ? "RESET_MATCH" : "START_NEXT_GAME"}">${watchState.matchOver ? "เริ่มแมตช์ใหม่" : "เริ่มเกมถัดไป"}</button>` : ""}
        </div>
      </article>

      <article class="device phone-shell">
        <div class="device-heading"><span>Android simulation</span><b>rev ${state.revision}</b></div>
        <div class="phone-screen">
          <div class="phone-topline">
            <div><small>Live score</small><h2>${state.matchOver ? `ผู้ชนะ ${escapeHtml(winner)}` : state.gameOver ? "จบเกม" : `เกม ${state.gameNumber}`}</h2></div>
            <div class="sync-time">ซิงก์ล่าสุด<br><b>${new Date(lastSync).toLocaleTimeString("th-TH")}</b></div>
          </div>
          ${scorePanel(state, "phone")}
          <div class="match-meta"><span>เกมที่ชนะ</span><strong>${state.gamesWonA} : ${state.gamesWonB}</strong><span>${state.mode === "BADMINTON_21" ? "21 แต้ม · ชนะ 2 ใน 3" : "นับแต้มอิสระ"}</span></div>
          <div class="phone-actions">
            <button data-phone-action="UNDO">Undo</button>
            <button data-phone-action="SWAP_SIDES">สลับฝั่ง</button>
            <button data-open-settings>ตั้งค่า</button>
            <button class="danger" data-phone-action="RESET_MATCH">เกมใหม่</button>
          </div>
        </div>
      </article>
    </section>

    <section class="control-deck">
      <div class="control-copy">
        <p class="kicker">Network controls</p>
        <h2>ทดลองตอนสัญญาณไม่เป็นใจ</h2>
        <p>ตัดการเชื่อมต่อ กดแต้มหลายครั้ง แล้วต่อกลับ ระบบจะ replay คำสั่งตามลำดับและไม่รับ commandId ซ้ำ</p>
      </div>
      <div class="control-buttons">
        <button class="connection-toggle" data-toggle-connection>${connection === "DISCONNECTED" ? "เชื่อมต่อใหม่" : "ตัดการเชื่อมต่อ"}</button>
        <button data-duplicate ${lastAccepted ? "" : "disabled"}>ส่งข้อความซ้ำ</button>
        <button data-conflict>จำลอง revision conflict</button>
      </div>
      <ol class="event-log">${eventLog.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ol>
    </section>

    <dialog id="settings-dialog">
      <form method="dialog" id="settings-form">
        <div><p class="kicker">Match settings</p><h2>ตั้งค่าแมตช์</h2></div>
        <label>ชื่อฝั่ง A<input name="teamA" maxlength="18" value="${escapeHtml(state.teamNameA)}"></label>
        <label>ชื่อฝั่ง B<input name="teamB" maxlength="18" value="${escapeHtml(state.teamNameB)}"></label>
        <label>รูปแบบ<select name="mode"><option value="BADMINTON_21" ${state.mode === "BADMINTON_21" ? "selected" : ""}>Badminton 21</option><option value="SIMPLE" ${state.mode === "SIMPLE" ? "selected" : ""}>Simple counter</option></select></label>
        <div class="dialog-actions"><button value="cancel">ยกเลิก</button><button value="default" data-save-settings>บันทึก</button></div>
      </form>
    </dialog>`;

  bindEvents();
}

function bindEvents(): void {
  document.querySelectorAll<HTMLButtonElement>("[data-action]").forEach((button) => {
    button.onclick = () => issue(button.dataset.action as ScoreAction, button.dataset.team as Team, button.closest(".watch") ? "watch-01" : "android-01");
  });
  document.querySelectorAll<HTMLButtonElement>("[data-watch-action]").forEach((button) => {
    button.onclick = () => issue(button.dataset.watchAction as ScoreAction, button.dataset.team as Team | undefined);
  });
  document.querySelectorAll<HTMLButtonElement>("[data-phone-action]").forEach((button) => {
    button.onclick = () => {
      const action = button.dataset.phoneAction as ScoreAction;
      if (action === "RESET_MATCH" && !confirm("เริ่มแมตช์ใหม่และล้างคะแนนปัจจุบัน?")) return;
      issue(action, undefined, "android-01");
    };
  });
  document.querySelector<HTMLButtonElement>("[data-toggle-connection]")!.onclick = () => {
    if (connection === "DISCONNECTED") void reconnect();
    else { connection = "DISCONNECTED"; log("ตัดการเชื่อมต่อ — watch เริ่ม queue คำสั่ง"); render(); }
  };
  document.querySelector<HTMLButtonElement>("[data-duplicate]")!.onclick = () => lastAccepted && send(lastAccepted, false);
  document.querySelector<HTMLButtonElement>("[data-conflict]")!.onclick = () => {
    const stale = makeCommand({ matchId: engine.state.matchId, senderId: "watch-01", baseRevision: Math.max(0, engine.state.revision - 1), action: "INCREMENT", team: "A" });
    send(stale, false);
  };
  const dialog = document.querySelector<HTMLDialogElement>("#settings-dialog")!;
  document.querySelector<HTMLButtonElement>("[data-open-settings]")!.onclick = () => dialog.showModal();
  document.querySelector<HTMLButtonElement>("[data-save-settings]")!.onclick = (event) => {
    event.preventDefault();
    const data = new FormData(document.querySelector<HTMLFormElement>("#settings-form")!);
    const next = createInitialState({
      matchId: `match-${Date.now()}`,
      teamNameA: String(data.get("teamA") || "Team A").trim(),
      teamNameB: String(data.get("teamB") || "Team B").trim(),
      mode: data.get("mode") === "SIMPLE" ? "SIMPLE" : "BADMINTON_21",
    });
    engine.replaceState(next);
    watchState = structuredClone(next);
    pending = [];
    log("บันทึกการตั้งค่าและเริ่มแมตช์ใหม่");
    persist();
    dialog.close();
    render();
  };
}

render();
