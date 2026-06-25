import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "./App";
import * as api from "./services/api";

jest.mock("./services/api");

describe("App", () => {
  it("renders secure portal heading and health status", async () => {
    const mockedCheckHealth = api.checkHealth as jest.MockedFunction<typeof api.checkHealth>;
    mockedCheckHealth.mockResolvedValue("OK");

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false
        }
      }
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <App />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(screen.getByRole("heading", { name: /Secure Customer Access Portal/i })).toBeInTheDocument();
    expect(await screen.findByText(/Core banking services: OK/i)).toBeInTheDocument();
  });
});
