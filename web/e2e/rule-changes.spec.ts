import { expect, test } from "@playwright/test";

async function run(page: import("@playwright/test").Page, title: string) {
  await page.goto("/rule-changes#try");
  await page.getByRole("button", { name: title }).click();
  await page.getByRole("button", { name: "Run the test run" }).click();
  return page.locator(".try-result");
}

test("moving Hamburg to DPD passes and lists parcels that switch", async ({ page }) => {
  const result = await run(page, "Move Hamburg to DPD Classic");

  await expect(result.locator(".verdict strong")).toHaveText("Passed");
  await expect(result.getByRole("cell", { name: "DPD Classic" }).first()).toBeVisible();
});

test("a clashing rule fails the test run", async ({ page }) => {
  const result = await run(page, "A rule that clashes");

  await expect(result.locator(".verdict strong")).toHaveText("Failed");
  await expect(result.getByText(/same priority/)).toBeVisible();
});

test("removing a region shows parcels without a courier", async ({ page }) => {
  const result = await run(page, "A region left without a courier");

  await expect(result.locator(".verdict strong")).toHaveText("Passed, with a warning");
  await expect(result.getByRole("cell", { name: "no courier" }).first()).toBeVisible();
});

test("a broken rule is refused before any test run", async ({ page }) => {
  const result = await run(page, "A broken rule");

  await expect(result.locator(".verdict strong")).toHaveText("Refused");
});
