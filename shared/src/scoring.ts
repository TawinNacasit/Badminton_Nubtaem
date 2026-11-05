import type { MatchMode, ScoreAction, ScoreStateMessage, Team } from "./protocol";
import { PROTOCOL_VERSION } from "./protocol";

export interface Snapshot {
  teamA: number;
  teamB: number;
  teamNameA: string;
  teamNameB: string;
  gamesWonA: number;
  gamesWonB: number;
  gameNumber: number;
  gameOver: boolean;
  matchOver: boolean;
}

export interface MatchState extends ScoreStateMessage {
  history: Snapshot[];
}

export interface ReducerAction {
  action: ScoreAction;
  team?: Team;
}

export function createInitialState(overrides: Partial<MatchState> = {}): MatchState {
  return {
    protocolVersion: PROTOCOL_VERSION,
    type: "SCORE_STATE",
    matchId: `match-${Date.now()}`,
    revision: 0,
    teamA: 0,
    teamB: 0,
    teamNameA: "Team A",
    teamNameB: "Team B",
    mode: "BADMINTON_21",
    gameNumber: 1,
    gamesWonA: 0,
    gamesWonB: 0,
    gameOver: false,
    matchOver: false,
    history: [],
    ...overrides,
  };
}

function snapshot(state: MatchState): Snapshot {
  const { teamA, teamB, teamNameA, teamNameB, gamesWonA, gamesWonB, gameNumber, gameOver, matchOver } = state;
  return { teamA, teamB, teamNameA, teamNameB, gamesWonA, gamesWonB, gameNumber, gameOver, matchOver };
}

export function isWinningScore(own: number, other: number, mode: MatchMode): boolean {
  if (mode === "SIMPLE") return false;
  if (own === 30) return true;
  return own >= 21 && own - other >= 2;
}

export function reduceScore(state: MatchState, command: ReducerAction): MatchState {
  if (command.action === "UNDO") {
    const previous = state.history.at(-1);
    if (!previous) return state;
    return { ...state, ...previous, history: state.history.slice(0, -1) };
  }

  if (command.action === "RESET_MATCH") {
    return createInitialState({
      matchId: `match-${Date.now()}`,
      teamNameA: state.teamNameA,
      teamNameB: state.teamNameB,
      mode: state.mode,
    });
  }

  if (command.action === "START_NEXT_GAME") {
    if (!state.gameOver || state.matchOver) return state;
    return {
      ...state,
      teamA: 0,
      teamB: 0,
      gameNumber: state.gameNumber + 1,
      gameOver: false,
      history: [...state.history, snapshot(state)].slice(-100),
    };
  }

  if (command.action === "SWAP_SIDES") {
    return {
      ...state,
      teamA: state.teamB,
      teamB: state.teamA,
      teamNameA: state.teamNameB,
      teamNameB: state.teamNameA,
      gamesWonA: state.gamesWonB,
      gamesWonB: state.gamesWonA,
      history: [...state.history, snapshot(state)].slice(-100),
    };
  }

  if (!command.team || state.gameOver) return state;
  const key = command.team === "A" ? "teamA" : "teamB";
  const current = state[key];
  const nextValue = command.action === "INCREMENT" ? current + 1 : Math.max(0, current - 1);
  if (nextValue === current || !["INCREMENT", "DECREMENT"].includes(command.action)) return state;

  const next: MatchState = {
    ...state,
    [key]: nextValue,
    history: [...state.history, snapshot(state)].slice(-100),
  };
  const own = next[key];
  const other = command.team === "A" ? next.teamB : next.teamA;
  if (isWinningScore(own, other, next.mode)) {
    next.gameOver = true;
    if (command.team === "A") next.gamesWonA += 1;
    else next.gamesWonB += 1;
    next.matchOver = next.gamesWonA === 2 || next.gamesWonB === 2;
  }
  return next;
}

export function toMessage(state: MatchState): ScoreStateMessage {
  const { history: _history, ...message } = state;
  return message;
}

