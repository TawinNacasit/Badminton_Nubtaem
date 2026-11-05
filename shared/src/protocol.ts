export const PROTOCOL_VERSION = 1 as const;

export type Team = "A" | "B";
export type MatchMode = "SIMPLE" | "BADMINTON_21";
export type ScoreAction =
  | "INCREMENT"
  | "DECREMENT"
  | "UNDO"
  | "RESET_MATCH"
  | "START_NEXT_GAME"
  | "SWAP_SIDES";

export interface ScoreCommand {
  protocolVersion: typeof PROTOCOL_VERSION;
  type: "SCORE_COMMAND";
  matchId: string;
  commandId: string;
  senderId: string;
  baseRevision: number;
  action: ScoreAction;
  team?: Team;
  timestamp: number;
}

export interface ScoreStateMessage {
  protocolVersion: typeof PROTOCOL_VERSION;
  type: "SCORE_STATE";
  matchId: string;
  revision: number;
  teamA: number;
  teamB: number;
  teamNameA: string;
  teamNameB: string;
  mode: MatchMode;
  gameNumber: number;
  gamesWonA: number;
  gamesWonB: number;
  gameOver: boolean;
  matchOver: boolean;
  lastCommandId?: string;
}

export interface CommandResult {
  kind: "ACK" | "DUPLICATE" | "CONFLICT" | "INVALID";
  state: ScoreStateMessage;
  detail?: string;
}

export function makeCommand(
  input: Omit<ScoreCommand, "protocolVersion" | "type" | "commandId" | "timestamp"> &
    Partial<Pick<ScoreCommand, "commandId" | "timestamp">>,
): ScoreCommand {
  return {
    protocolVersion: PROTOCOL_VERSION,
    type: "SCORE_COMMAND",
    commandId: input.commandId ?? crypto.randomUUID(),
    timestamp: input.timestamp ?? Date.now(),
    matchId: input.matchId,
    senderId: input.senderId,
    baseRevision: input.baseRevision,
    action: input.action,
    team: input.team,
  };
}

