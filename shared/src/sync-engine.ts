import type { CommandResult, ScoreCommand, ScoreStateMessage } from "./protocol";
import { reduceScore, toMessage, type MatchState } from "./scoring";

export class AuthoritativeScoreEngine {
  private processed = new Map<string, ScoreStateMessage>();

  constructor(private current: MatchState) {}

  get state(): MatchState {
    return this.current;
  }

  replaceState(state: MatchState): void {
    this.current = state;
    this.processed.clear();
  }

  handle(command: ScoreCommand): CommandResult {
    const duplicate = this.processed.get(command.commandId);
    if (duplicate) return { kind: "DUPLICATE", state: duplicate, detail: "commandId already accepted" };
    if (command.matchId !== this.current.matchId || command.baseRevision !== this.current.revision) {
      return { kind: "CONFLICT", state: toMessage(this.current), detail: "matchId or baseRevision mismatch" };
    }

    const reduced = reduceScore(this.current, command);
    if (reduced === this.current) {
      return { kind: "INVALID", state: toMessage(this.current), detail: "action is not valid for current state" };
    }
    this.current = { ...reduced, revision: this.current.revision + 1, lastCommandId: command.commandId };
    const message = toMessage(this.current);
    this.processed.set(command.commandId, message);
    return { kind: "ACK", state: message };
  }
}

