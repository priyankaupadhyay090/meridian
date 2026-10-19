import { expect, test } from "@playwright/test";

test("the shop's own offer comes first and switching a courier off removes it", async ({ page }) => {
  await page.goto("/checkout");

  const preview = page.locator(".checkout-preview");
  await expect(preview.getByRole("radio", { name: /Hermes.*Shop's choice/ })).toBeChecked();
  await expect(preview.locator(".checkout-total strong")).toHaveText("Free");

  await page.getByRole("switch", { name: "GLS" }).click();
  await expect(preview.getByRole("radio", { name: /GLS/ })).toHaveCount(0);

  await preview.getByRole("radio", { name: /DHL/ }).check();
  await expect(preview.locator(".checkout-total strong")).toHaveText(/4,19/);
});
