import { test, type Page } from "@playwright/test";

const dir = process.env.SCREENSHOTS_DIR;

test.skip(!dir, "set SCREENSHOTS_DIR to capture screenshots");
test.use({ deviceScaleFactor: 2 });

async function shot(page: Page, name: string, selector?: string) {
  if (selector) await page.addStyleTag({ content: ".site-header { position: static !important; }" });
  const target = selector ? page.locator(selector).first() : page;
  await target.screenshot({ path: `${dir}/${name}.png`, animations: "disabled" });
}

test("screenshots", async ({ page }) => {
  await page.goto("/");
  await page.locator(".mini-options li").first().waitFor();
  await shot(page, "meridian-home");

  await page.goto("/compare");
  await page.getByRole("radio").first().waitFor();
  await shot(page, "meridian-compare", ".compare-grid");

  await page.goto("/contract-check#try");
  await page.getByRole("button", { name: "Standard terms that cut liability too far" }).click();
  await page.getByRole("button", { name: "Check contract" }).click();
  await page.locator(".try-result .verdict").waitFor();
  await shot(page, "meridian-contract-check", ".try-grid");

  await page.getByRole("button", { name: "Deutsche AGB mit schweren Paketen" }).click();
  await page.getByRole("button", { name: "Check contract" }).click();
  await page.getByText(/reads as an instruction/).waitFor();
  await shot(page, "meridian-instruction-caught", ".try-grid");

  await page.goto("/contract-check");
  await page.locator("#ai .ai-trace").waitFor();
  await shot(page, "meridian-ai", "#ai .container");

  await page.goto("/rule-changes#try");
  await page.getByRole("button", { name: "A region left without a courier" }).click();
  await page.getByRole("button", { name: "Run the test run" }).click();
  await page.locator(".try-result .verdict").waitFor();
  await shot(page, "meridian-rule-change", ".try-grid");

  await page.goto("/library");
  await page.locator(".library-item").first().waitFor();
  await shot(page, "meridian-library", "main");

  await page.goto("/sign-in");
  await page.getByLabel("User name").fill("nordlicht");
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByText("Your business only.").waitFor();
  const sample = await (await fetch("http://localhost:5180/samples/sample-contract.pdf")).arrayBuffer().catch(() => null);
  if (sample) {
    await page.locator('input[type="file"]').setInputFiles({ name: "dpd-terms-2026.pdf", mimeType: "application/pdf", buffer: Buffer.from(sample) });
    await page.getByRole("heading", { name: "dpd-terms-2026.pdf" }).waitFor();
  }
  await shot(page, "meridian-my-contracts", "main");

  await page.goto("/checkout");
  await page.locator(".checkout-preview .delivery-option").first().waitFor();
  await shot(page, "meridian-checkout", ".checkout-grid");
});

test("portfolio card", async ({ page }) => {
  await page.setViewportSize({ width: 960, height: 640 });
  await page.goto("/");
  await page.locator(".mini-options li").first().waitFor();
  await page.addStyleTag({ content: ".site-header { display: none !important; } .hero { padding-top: 40px !important; } .hero h1 { font-size: 2.6rem !important; }" });
  await page.screenshot({ path: `${dir}/meridian-card.png`, clip: { x: 0, y: 0, width: 960, height: 640 }, animations: "disabled" });
});
