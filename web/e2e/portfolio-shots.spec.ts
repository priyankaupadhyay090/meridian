import { test, type Locator, type Page } from "@playwright/test";

const dir = process.env.SCREENSHOTS_DIR;

test.skip(!dir || !process.env.PORTFOLIO, "set SCREENSHOTS_DIR and PORTFOLIO to capture the portfolio images");
test.use({ deviceScaleFactor: 2, viewport: { width: 760, height: 1000 } });

async function shot(page: Page, target: Locator, name: string, maxHeight = 640) {
  await page.addStyleTag({ content: ".site-header { position: static !important; }" });
  await target.scrollIntoViewIfNeeded();
  const box = await target.boundingBox();
  if (!box) throw new Error(`${name} not visible`);
  await page.screenshot({
    path: `${dir}/${name}.png`,
    clip: { x: box.x, y: box.y, width: box.width, height: Math.min(box.height, maxHeight) },
    animations: "disabled",
  });
}

test("portfolio images", async ({ page }) => {
  await page.goto("/contract-check#try");
  await page.getByRole("button", { name: "Standard terms that cut liability too far" }).click();
  await page.getByRole("button", { name: "Check contract" }).click();
  await page.locator(".try-result .verdict").waitFor();
  await shot(page, page.locator(".try-result"), "meridian-feat-contract", 560);

  await shot(page, page.locator("#ai .ai-trace"), "meridian-feat-ai", 600);

  await page.goto("/compare");
  await page.getByRole("radio").first().waitFor();
  await shot(page, page.locator(".compare-grid > div").last(), "meridian-feat-compare", 560);

  await page.goto("/sign-in");
  await page.getByLabel("User name").fill("nordlicht");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByText("Your business only.").waitFor();
  await shot(page, page.locator(".privacy"), "meridian-feat-privacy", 560);
});
