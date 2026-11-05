import { describe, expect, it } from "vitest";
import { makeCommand } from "../../shared/src/protocol";
import { createInitialState } from "../../shared/src/scoring";
import { AuthoritativeScoreEngine } from "../../shared/src/sync-engine";

describe("authoritative sync engine", () => {
  it("applies a command exactly once even when retried", () => {
    const initial = createInitialState({ matchId: "m-1" });
    const engine = new AuthoritativeScoreEngine(initial);
    const command = makeCommand({
      matchId: "m-1",
      commandId: "same-command",
      senderId: "watch-01",
      baseRevision: 0,
      action: "INCREMENT",
      team: "A",
    });
    expect(engine.handle(command).kind).toBe("ACK");
    expect(engine.handle(command).kind).toBe("DUPLICATE");
    expect(engine.state.teamA).toBe(1);
    expect(engine.state.revision).toBe(1);
  });

  it("rejects stale revisions and returns full authoritative state", () => {
    const engine = new AuthoritativeScoreEngine(createInitialState({ matchId: "m-1", revision: 4, teamA: 8 }));
    const result = engine.handle(makeCommand({
      matchId: "m-1",
      senderId: "watch-01",
      baseRevision: 3,
      action: "INCREMENT",
      team: "A",
    }));
    expect(result.kind).toBe("CONFLICT");
    expect(result.state.teamA).toBe(8);
    expect(result.state.revision).toBe(4);
  });

  it("preserves rapid ordered commands when each uses the latest revision", () => {
    const engine = new AuthoritativeScoreEngine(createInitialState({ matchId: "m-rapid", mode: "SIMPLE" }));
    for (let revision = 0; revision < 50; revision += 1) {
      const result = engine.handle(makeCommand({
        matchId: "m-rapid",
        commandId: `rapid-${revision}`,
        senderId: "watch-01",
        baseRevision: revision,
        action: "INCREMENT",
        team: "B",
      }));
      expect(result.kind).toBe("ACK");
    }
    expect(engine.state.teamB).toBe(50);
    expect(engine.state.revision).toBe(50);
  });
});
