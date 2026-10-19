import { expect, test, type Page } from "@playwright/test";

async function signIn(page: Page, user: string) {
  await page.goto("/sign-in");
  await page.getByLabel("User name").fill(user);
  await page.getByLabel("Password").fill("demo");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/my-contracts/);
}

async function uploadText(page: Page, name: string, text: string) {
  await page.locator('input[type="file"]').setInputFiles({ name, mimeType: "text/plain", buffer: Buffer.from(text) });
  await expect(page.getByRole("heading", { name })).toBeVisible();
}

test("the library links to the couriers' official terms and offers the samples", async ({ page }) => {
  await page.goto("/library");

  await expect(page.getByRole("link", { name: "Open on DHL's site" })).toHaveAttribute("href", "https://www.dhl.de/agb");
  await expect(page.getByRole("link", { name: "Open on GLS's site" })).toHaveAttribute("href", /gls-pakete\.de/);
  await expect(page.getByRole("link", { name: "PDF" }).first()).toHaveAttribute("href", "/samples/sample-contract.pdf");
});

test("a business sees only its own contracts and can check and delete them", async ({ page, browser }) => {
  const nordlichtFile = `nordlicht-${Date.now()}.txt`;
  await signIn(page, "nordlicht");
  await expect(page.getByText("Your business only.")).toBeVisible();
  await uploadText(page, nordlichtFile, "5.1 Liability is limited to 1 SDR per kilogram.");

  const other = await browser.newPage();
  await signIn(other, "weserkontor");
  await expect(other.getByRole("heading", { name: /Weserkontor/ })).toBeVisible();
  await expect(other.getByRole("heading", { name: nordlichtFile })).toHaveCount(0);
  await other.close();

  const item = page.locator(".library-item", { hasText: nordlichtFile });
  await item.getByRole("button", { name: "Check this contract" }).click();
  await expect(page.getByLabel("Contract text")).toHaveValue("5.1 Liability is limited to 1 SDR per kilogram.");

  await page.goto("/my-contracts");
  await page.locator(".library-item", { hasText: nordlichtFile }).getByRole("button", { name: "Delete" }).click();
  await expect(page.getByRole("heading", { name: nordlichtFile })).toHaveCount(0);
});

test("my contracts needs a sign-in, and a wrong password is refused", async ({ page }) => {
  await page.goto("/my-contracts");
  await expect(page).toHaveURL(/sign-in/);

  await page.getByLabel("User name").fill("nordlicht");
  await page.getByLabel("Password").fill("wrong");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("alert")).toContainText("do not match");
});

test("the public contract check says nothing is stored", async ({ page }) => {
  await page.goto("/contract-check#try");
  await expect(page.getByText(/Nothing you upload here is stored/)).toBeVisible();
});
