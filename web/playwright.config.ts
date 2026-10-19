import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "e2e",
  fullyParallel: true,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: "http://localhost:5180",
    channel: "chrome",
    viewport: { width: 1440, height: 900 },
  },
  webServer: [
    {
      command: "cd .. && ./gradlew bootRun",
      url: "http://localhost:8080/api/public/couriers",
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      command: "npm run dev",
      url: "http://localhost:5180",
      reuseExistingServer: true,
    },
  ],
});
