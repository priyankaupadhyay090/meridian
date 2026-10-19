import { expect, test } from "@playwright/test";

test("compares every courier that takes the parcel and marks the cheapest", async ({ page }) => {
  await page.goto("/compare");

  const options = page.getByRole("radio");
  await expect(options).toHaveCount(4);
  await expect(page.getByRole("radio", { name: /DHL.*Cheapest/ })).toBeChecked();
});

test("a heavy box drops the couriers that cannot take it", async ({ page }) => {
  await page.goto("/compare");
  await page.getByRole("button", { name: "Heavy box" }).click();

  await expect(page.getByRole("radio", { name: /DHL/ })).toBeVisible();
  await expect(page.getByRole("radio", { name: /Päckchen/ })).toHaveCount(0);
});

test("every published price links to its source", async ({ page }) => {
  await page.goto("/compare");

  for (const courier of ["DHL", "Hermes", "DPD", "GLS"]) {
    const group = page.locator(".rate-group", { has: page.getByRole("heading", { name: courier, exact: true }) });
    await expect(group.getByRole("link", { name: /source|price list/ })).toHaveAttribute("href", /^https:\/\//);
  }
});
