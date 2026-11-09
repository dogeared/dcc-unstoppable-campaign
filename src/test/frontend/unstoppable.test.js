import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

// Loading the script registers window.Unstoppable, exactly as the browser does.
await import("../../main/frontend/scripts/unstoppable.js");

describe("Unstoppable.copyText", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    document.body.innerHTML = "";
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("uses the async clipboard in a secure context", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("isSecureContext", true);
    vi.stubGlobal("navigator", { clipboard: { writeText } });

    await window.Unstoppable.copyText("hello");

    expect(writeText).toHaveBeenCalledWith("hello");
    vi.unstubAllGlobals();
  });

  it("falls back to the textarea copy when the clipboard rejects", async () => {
    const writeText = vi.fn().mockRejectedValue(new Error("denied"));
    vi.stubGlobal("isSecureContext", true);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    const execCommand = vi.fn().mockReturnValue(true);
    document.execCommand = execCommand;

    await window.Unstoppable.copyText("fallback");

    expect(execCommand).toHaveBeenCalledWith("copy");
    expect(document.body.children).toHaveLength(0);
    vi.unstubAllGlobals();
  });

  it("uses the textarea copy outside a secure context", async () => {
    vi.stubGlobal("isSecureContext", false);
    vi.stubGlobal("navigator", {});
    const execCommand = vi.fn().mockReturnValue(true);
    document.execCommand = execCommand;

    await window.Unstoppable.copyText("plain");

    expect(execCommand).toHaveBeenCalledWith("copy");
    vi.unstubAllGlobals();
  });

  it("returns false from the legacy path when execCommand throws", async () => {
    vi.stubGlobal("isSecureContext", false);
    vi.stubGlobal("navigator", {});
    document.execCommand = vi.fn(() => {
      throw new Error("unsupported");
    });

    await window.Unstoppable.copyText("broken");

    expect(document.body.children).toHaveLength(0);
    vi.unstubAllGlobals();
  });
});
