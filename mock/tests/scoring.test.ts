import { describe, expect, it } from "vitest";
import { createInitialState, isWinningScore, reduceScore } from "../../shared/src/scoring";

function add(state = createInitialState(), team: "A" | "B" = "A", times = 1) {
  for (let i = 0; i < times; i += 1) state = reduceScore(state, { action: "INCREMENT", team });
  return state;
}

describe("scoring reducer", () => {
  it("never decrements below zero", () => {
    const state = reduceScore(createInitialState(), { action: "DECREMENT", team: "A" });
    expect(state.teamA).toBe(0);
  });

  it("undoes at least ten accepted actions in order", () => {
    let state = add(createInitialState(), "A", 12);
    for (let i = 0; i < 10; i += 1) state = reduceScore(state, { action: "UNDO" });
    expect(state.teamA).toBe(2);
    expect(state.history).toHaveLength(2);
  });

  it("requires a two-point lead at 20-20", () => {
    let state = add(createInitialState(), "A", 20);
    state = add(state, "B", 20);
    state = add(state, "A", 1);
    expect(state.gameOver).toBe(false);
    state = add(state, "A", 1);
    expect(state.gameOver).toBe(true);
    expect(state.gamesWonA).toBe(1);
  });

  it("caps a deuce game when one side reaches 30", () => {
    expect(isWinningScore(29, 29, "BADMINTON_21")).toBe(false);
    expect(isWinningScore(30, 29, "BADMINTON_21")).toBe(true);
    const state = reduceScore(createInitialState({ teamA: 29, teamB: 29 }), { action: "INCREMENT", team: "A" });
    expect(state.teamA).toBe(30);
    expect(state.gameOver).toBe(true);
  });

  it("finishes a best-of-three match after two games", () => {
    let state = add(createInitialState(), "A", 21);
    expect(state.gameOver).toBe(true);
    state = reduceScore(state, { action: "START_NEXT_GAME" });
    state = add(state, "A", 21);
    expect(state.matchOver).toBe(true);
    expect(state.gamesWonA).toBe(2);
    expect(reduceScore(state, { action: "INCREMENT", team: "A" })).toEqual(state);
  });

  it("simple mode does not auto-finish", () => {
    const state = add(createInitialState({ mode: "SIMPLE" }), "A", 35);
    expect(state.gameOver).toBe(false);
    expect(state.teamA).toBe(35);
  });
});

