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
    assert.ok(step.boxW >= 0.02);
    assert.ok(step.boxH >= 0.015);
  });

  it("maps spoken text, app guess, box, and confidence", () => {
    const step = toGuideResponse(
      JSON.stringify({
        instructionEn: "Tap Compose",
        instructionSw: "Bonyeza Compose",
        spokenEn: "Tap the round Compose button at the bottom right.",
        spokenSw: "Bonyeza kitufe cha Compose chini kulia.",
        appGuess: "Gmail",
        targetLabel: "Compose",
        pointX: 0.86,
        pointY: 0.9,
        boxX: 0.78,
        boxY: 0.84,
        boxW: 0.16,
        boxH: 0.1,
        confidence: 0.86,
        done: false,
      }),
    );
    assert.equal(step.spokenEn, "Tap the round Compose button at the bottom right.");
    assert.equal(step.appGuess, "Gmail");
    assert.equal(step.targetLabel, "Compose");
    assert.equal(step.boxX, 0.78);
    assert.equal(step.boxY, 0.84);
    assert.equal(step.boxW, 0.16);
    assert.equal(step.boxH, 0.1);
    assert.equal(step.confidence, 0.86);
  });

  it("derives the point from a bounding box when point is missing", () => {
    const step = toGuideResponse(
      '{"instructionEn":"Tap Search","boxX":0.1,"boxY":0.2,"boxW":0.8,"boxH":0.1}',
    );
    assert.ok(Math.abs(step.pointX - 0.5) < 0.001);
    assert.ok(Math.abs(step.pointY - 0.25) < 0.001);
  });

  it("accepts percent-style box values and a box array", () => {
    const step = toGuideResponse(
      '{"instructionEn":"Tap Send","box":[70,80,20,10],"confidence":80}',
    );
    assert.equal(step.boxX, 0.7);
    assert.equal(step.boxY, 0.8);
    assert.equal(step.boxW, 0.2);
    assert.equal(step.boxH, 0.1);
    assert.equal(step.confidence, 0.8);
  });

  it("synthesizes a tight box around the point when none is given", () => {
    const step = toGuideResponse(
      '{"instructionEn":"Tap Next","pointX":0.5,"pointY":0.88}',
    );
    assert.ok(step.boxW >= 0.02 && step.boxH >= 0.015);
    assert.ok(step.pointX > step.boxX && step.pointX < step.boxX + step.boxW);
    assert.ok(step.pointY > step.boxY && step.pointY < step.boxY + step.boxH);
  });
});
