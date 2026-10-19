import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { CheckoutPage } from "./pages/CheckoutPage";
import { ComparePage } from "./pages/ComparePage";
import { ContractCheckPage } from "./pages/ContractCheckPage";
import { HomePage } from "./pages/HomePage";
import { LibraryPage } from "./pages/LibraryPage";
import { MyContractsPage } from "./pages/MyContractsPage";
import { SignInPage } from "./pages/SignInPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { RuleChangesPage } from "./pages/RuleChangesPage";

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="compare" element={<ComparePage />} />
        <Route path="contract-check" element={<ContractCheckPage />} />
        <Route path="library" element={<LibraryPage />} />
        <Route path="sign-in" element={<SignInPage />} />
        <Route path="my-contracts" element={<MyContractsPage />} />
        <Route path="rule-changes" element={<RuleChangesPage />} />
        <Route path="checkout" element={<CheckoutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
