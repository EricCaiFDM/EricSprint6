import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { ResetRequestPage } from "./ResetRequestPage";
import * as api from "../services/api";

jest.mock("../services/api");

describe("ResetRequestPage", () => {
  function renderPage() {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false
        }
      }
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ResetRequestPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();
    (api.requestPasswordReset as jest.MockedFunction<typeof api.requestPasswordReset>).mockResolvedValue({
      status: "ACCEPTED",
      message: "If the account exists, reset instructions will be sent."
    });
    (api.confirmPasswordReset as jest.MockedFunction<typeof api.confirmPasswordReset>).mockResolvedValue({
      status: "ACCEPTED",
      message: "If the account exists, account access has been reset."
    });
  });

  it("reveals and submits new password step after reset request", async () => {
    renderPage();

    fireEvent.change(screen.getByLabelText(/Registered email/i), {
      target: { value: "reset.user@example.com" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Request Reset/i }));

    await waitFor(() => {
      expect(api.requestPasswordReset).toHaveBeenCalled();
      const requestCall = (api.requestPasswordReset as jest.Mock).mock.calls[0]?.[0];
      expect(requestCall).toEqual({ identity: "reset.user@example.com" });
    });

    fireEvent.change(screen.getByLabelText(/^New password$/i), {
      target: { value: "newsecret123" }
    });

    fireEvent.change(screen.getByLabelText(/^Confirm new password$/i), {
      target: { value: "newsecret123" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Set New Password/i }));

    await waitFor(() => {
      expect(api.confirmPasswordReset).toHaveBeenCalled();
      const confirmCall = (api.confirmPasswordReset as jest.Mock).mock.calls[0]?.[0];
      expect(confirmCall).toEqual({
        identity: "reset.user@example.com",
        password: "newsecret123",
        passwordConfirmation: "newsecret123"
      });
    });
  });
});
