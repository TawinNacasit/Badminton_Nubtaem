import app from '@system.app';
import { Builder, Message, P2pClient } from '../../wearenginesdk/wearengine';
import { PHONE_APP_FINGERPRINT, PHONE_APP_PACKAGE_NAME } from '../../constants/constants.js';

const p2pClient = new P2pClient();
const messageClient = new Message();
const builderClient = new Builder();

export default {
  data: {
    matchId: 'local-match-001', revision: 0,
    teamA: 0, teamB: 0, teamNameA: 'Team A', teamNameB: 'Team B',
    gameNumber: 1, gamesWonA: 0, gamesWonB: 0, gameOver: false, matchOver: false,
    connection: 'syncing', statusText: 'Syncing', pending: [], sequence: 0
  },
  onInit() {
    if (PHONE_APP_FINGERPRINT.indexOf('REPLACE_') === 0) {
      this.connection = 'disconnected'; this.statusText = 'Setup needed'; return;
    }
    p2pClient.setPeerPkgName(PHONE_APP_PACKAGE_NAME);
    p2pClient.setPeerFingerPrint(PHONE_APP_FINGERPRINT);
    this.registerReceiver();
    this.ping();
  },
  onDestroy() { p2pClient.unregisterReceiver({ onSuccess() {} }); },
  registerReceiver() {
    const that = this;
    p2pClient.registerReceiver({
      onSuccess() {},
      onFailure() { that.connection = 'disconnected'; that.statusText = 'Offline'; },
      onReceiveMessage(data) {
        try {
          const raw = data && data.data ? data.data : data;
          const state = typeof raw === 'string' ? JSON.parse(raw) : raw;
          if (state.protocolVersion === 1 && state.type === 'SCORE_STATE') that.applyState(state);
        } catch (error) { console.error(`Invalid score state: ${error}`); }
      }
    });
  },
  ping() {
    const that = this;
    p2pClient.ping({
      onSuccess() {}, onFailure() { that.connection = 'disconnected'; that.statusText = 'Offline'; },
      onPingResult(result) {
        if (result.code === 207) { that.connection = 'connected'; that.statusText = 'Connected'; that.flushQueue(); }
        else { that.connection = 'disconnected'; that.statusText = 'Offline'; }
      }
    });
  },
  command(action, team) {
    const command = {
      protocolVersion: 1, type: 'SCORE_COMMAND', matchId: this.matchId,
      commandId: `watch-${Date.now()}-${this.sequence++}`, senderId: 'watch-01',
      baseRevision: this.revision + this.pending.length, action, timestamp: Date.now()
    };
    if (team) command.team = team;
    if (this.connection !== 'connected') {
      if (this.pending.length < 20) { this.pending.push(command); this.optimistic(action, team); this.statusText = `Offline · ${this.pending.length}`; }
      return;
    }
    this.send(command);
  },
  send(command) {
    const that = this;
    builderClient.setDescription(JSON.stringify(command));
    messageClient.builder = builderClient;
    that.connection = 'syncing'; that.statusText = 'Syncing';
    p2pClient.send(messageClient, {
      onSuccess() {},
      onFailure() { that.connection = 'disconnected'; that.statusText = 'Offline'; if (that.pending.length < 20) that.pending.push(command); },
      onSendResult(result) {
        if (result.code !== 207) { that.connection = 'disconnected'; that.statusText = `Error ${result.code}`; }
      },
      onSendProgress() {}
    });
  },
  flushQueue() {
    if (!this.pending.length) return;
    const command = this.pending.shift();
    command.matchId = this.matchId; command.baseRevision = this.revision;
    this.send(command); // Next item is sent only after authoritative SCORE_STATE arrives.
  },
  applyState(state) {
    this.matchId = state.matchId; this.revision = state.revision;
    this.teamA = state.teamA; this.teamB = state.teamB;
    this.teamNameA = state.teamNameA || 'Team A'; this.teamNameB = state.teamNameB || 'Team B';
    this.gameNumber = state.gameNumber; this.gamesWonA = state.gamesWonA; this.gamesWonB = state.gamesWonB;
    this.gameOver = state.gameOver; this.matchOver = state.matchOver;
    this.connection = 'connected'; this.statusText = 'Connected';
    this.flushQueue();
  },
  optimistic(action, team) {
    if (action === 'INCREMENT') team === 'A' ? this.teamA++ : this.teamB++;
    if (action === 'DECREMENT') team === 'A' ? this.teamA = Math.max(0, this.teamA - 1) : this.teamB = Math.max(0, this.teamB - 1);
  },
  incrementA() { this.command('INCREMENT', 'A'); }, incrementB() { this.command('INCREMENT', 'B'); },
  decrementA() { this.command('DECREMENT', 'A'); }, decrementB() { this.command('DECREMENT', 'B'); },
  undo() { this.command('UNDO'); },
  advance() { this.command(this.matchOver ? 'RESET_MATCH' : 'START_NEXT_GAME'); },
  swipeEvent(event) { if (event.direction === 'right') app.terminate(); }
};

