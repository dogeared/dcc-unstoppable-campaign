import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    include: ["src/test/frontend/**/*.test.js"],
    coverage: {
      provider: "v8",
      include: ["src/main/frontend/scripts/**/*.js"],
      reporter: ["text", "json-summary", "lcov"],
      reportsDirectory: "target/frontend-coverage",
      thresholds: {
        lines: 90,
        functions: 90,
        branches: 90,
        statements: 90
      }
    }
  }
});
