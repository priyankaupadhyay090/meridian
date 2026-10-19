import { expect, test } from "@playwright/test";

const pages = ["/", "/compare", "/contract-check", "/rule-changes", "/checkout"];

for (const path of pages) {
  test(`${path} fits a phone screen without sideways scrolling`, async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 800 });
    await page.goto(path);
    await expect(page.locator("h1")).toBeVisible();

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
  });
}

test("the home page shows a live comparison", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator(".mini-options li")).toHaveCount(4);
});
