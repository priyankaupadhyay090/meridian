import { expect, test } from "@playwright/test";

const samples: { title: string; verdict: string; finding?: string }[] = [
  { title: "Standard terms that cut liability too far", verdict: "Blocked", finding: "§ 449 Abs. 2 HGB" },
  { title: "Individually negotiated framework agreement", verdict: "Can become a draft" },
  { title: "Deutsche AGB mit schweren Paketen", verdict: "Can become a draft", finding: "§ 73 Abs. 1 PostG" },
  { title: "Returns booked by consumers", verdict: "Blocked", finding: "§ 449 Abs. 3 HGB" },
  { title: "Terms that exclude late delivery", verdict: "Blocked", finding: "§ 431 Abs. 3 i.V.m. § 449 Abs. 1 HGB" },
  { title: "Terms that follow the law", verdict: "Can become a draft" },
  { title: "Clauses a rule cannot express", verdict: "Nothing to draft" },
];

for (const sample of samples) {
  test(`sample contract: ${sample.title}`, async ({ page }) => {
    await page.goto("/contract-check#try");
    await page.getByRole("button", { name: sample.title }).click();
    await page.getByRole("button", { name: "Check contract" }).click();

    const result = page.locator(".try-result");
    await expect(result.locator(".verdict strong")).toHaveText(sample.verdict);
    if (sample.finding) {
      await expect(result.getByRole("link", { name: sample.finding })).toHaveAttribute(
        "href",
        /gesetze-im-internet\.de/,
      );
    }
  });
}

test("a hidden instruction in a contract is handed to a person", async ({ page }) => {
  await page.goto("/contract-check#try");
  await page.getByRole("button", { name: "Deutsche AGB mit schweren Paketen" }).click();
  await page.getByRole("button", { name: "Check contract" }).click();

  await expect(page.getByText(/reads as an instruction/)).toBeVisible();
  await expect(page.locator(".try-result tbody tr")).toHaveCount(1);
});

test("a changed contract is checked as typed", async ({ page }) => {
  await page.goto("/contract-check#try");
  await expect(page.getByLabel("Contract text")).not.toBeEmpty();
  await page.getByLabel("Contract text").fill("1. Liability is limited to 5 SDR per kilogram.");
  await page.getByRole("button", { name: "Check contract" }).click();

  await expect(page.locator(".try-result .verdict strong")).toHaveText("Nothing to draft");
  await expect(page.getByText("No clause could be turned into a routing rule.", { exact: true })).toBeVisible();
  await expect(page.locator(".try-result .badge-check")).toHaveText("Needs a check");
});

for (const file of ["sample-contract.pdf", "sample-contract.docx"]) {
  test(`an uploaded ${file.split(".").pop()} contract is read and checked`, async ({ page }) => {
    await page.goto("/contract-check#try");
    await page.locator('input[type="file"]').setInputFiles(`public/samples/${file}`);

    await expect(page.getByRole("status")).toContainText(file);
    await expect(page.getByLabel("Contract text")).toHaveValue(/5\.1 Liability for loss or damage/);

    await page.getByRole("button", { name: "Check contract" }).click();
    const result = page.locator(".try-result");
    await expect(result.locator(".verdict strong")).toHaveText("Blocked");
    await expect(result.getByRole("cell", { name: "DPD Express", exact: true })).toBeVisible();
    await expect(result.getByText(/depends on a condition/)).toBeVisible();
  });
}

test("a German PDF keeps its § clauses and the hidden instruction is caught", async ({ page }) => {
  await page.goto("/contract-check#try");
  await page.locator('input[type="file"]').setInputFiles("public/samples/muster-vertrag.pdf");
  await page.getByRole("button", { name: "Check contract" }).click();

  await expect(page.locator(".try-result .verdict strong")).toHaveText("Can become a draft");
  await expect(page.getByText(/reads as an instruction/)).toBeVisible();
});

test("an old Word file is refused with a reason", async ({ page }) => {
  await page.goto("/contract-check#try");
  await page.locator('input[type="file"]').setInputFiles({
    name: "old.doc",
    mimeType: "application/msword",
    buffer: Buffer.from([0xd0, 0xcf, 0x11, 0xe0, 0, 1]),
  });

  await expect(page.getByRole("alert")).toContainText(".docx or PDF");
});

test("German liability paragraphs numbered (1), (2) are read one by one", async ({ page }) => {
  await page.goto("/contract-check#try");
  await expect(page.getByLabel("Contract text")).not.toBeEmpty();
  await page.getByLabel("Contract text").fill(
    [
      "(1) Der Paketdienst haftet im Übrigen nur bis zu den gesetzlichen Haftungsgrenzen. Der Ersatz weitergehender Schäden ist ausgeschlossen.",
      "(2) Bei Überschreitung der Lieferfrist ist die Haftung auf den dreifachen Betrag der Fracht begrenzt.",
    ].join("\n"),
  );
  await page.getByRole("button", { name: "Check contract" }).click();

  const result = page.locator(".try-result");
  await expect(result.locator(".verdict strong")).toHaveText("Nothing to draft");
  await expect(result.getByText("8.33 SDR per kg")).toBeVisible();
  await expect(result.getByText("liability excluded")).toHaveCount(0);
});
