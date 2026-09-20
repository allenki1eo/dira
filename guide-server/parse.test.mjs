import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { clamp01, extractJsonObject, toGuideResponse } from "./parse.mjs";

describe("clamp01", () => {
  it("keeps fractions in range", () => {
    assert.equal(clamp01(0.25), 0.25);
    assert.equal(clamp01(-1), 0);
    assert.equal(clamp01(2), 1);
  });
  it("treats 0–100 as percents", () => {
    assert.equal(clamp01(80), 0.8);
  });
});

describe("extractJsonObject", () => {
  it("parses fenced JSON", () => {
    const obj = extractJsonObject('Sure:\n```json\n{"pointX":0.2,"done":false}\n```');
    assert.equal(obj.pointX, 0.2);
  });
  it("parses raw JSON with prose around it", () => {
    const obj = extractJsonObject('Here you go {"instructionEn":"Tap Back","pointY":0.9}');
    assert.equal(obj.instructionEn, "Tap Back");
  });
});

describe("toGuideResponse", () => {
  it("maps the Dira wire format", () => {
    const step = toGuideResponse(
      '{"instructionEn":"Tap Settings","instructionSw":"Bonyeza Mipangilio","pointX":0.9,"pointY":0.08,"done":false}',
    );
    assert.equal(step.instructionEn, "Tap Settings");
    assert.equal(step.instructionSw, "Bonyeza Mipangilio");
    assert.equal(step.pointX, 0.9);
    assert.equal(step.pointY, 0.08);
    assert.equal(step.done, false);
  });
});
