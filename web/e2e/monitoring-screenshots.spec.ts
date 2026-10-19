import { test } from "@playwright/test";

const dir = process.env.SCREENSHOTS_DIR;

test.skip(!dir || !process.env.MONITORING, "set SCREENSHOTS_DIR and MONITORING to capture the monitoring screenshots");
test.use({ deviceScaleFactor: 2, viewport: { width: 1600, height: 1000 } });

test("monitoring screenshots", async ({ page }) => {
  await page.goto("http://localhost:3001/d/meridian-kafka/meridian?orgId=1&from=now-12m&to=now&kiosk");
  await page.getByText("Messages in per second, by topic").waitFor();
  await page.waitForTimeout(4000);
  await page.screenshot({ path: `${dir}/meridian-grafana.png` });

  await page.goto("http://localhost:8090/ui/clusters/meridian/all-topics");
  await page.getByText("parcel-events.DLT").first().waitFor();
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${dir}/meridian-kafka-topics.png` });

  await page.goto("http://localhost:8090/ui/clusters/meridian/consumer-groups/meridian-parcels");
  await page.getByText("parcel-events").first().waitFor();
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${dir}/meridian-kafka-consumers.png` });
});
