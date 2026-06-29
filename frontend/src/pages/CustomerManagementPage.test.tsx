import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

import { CustomerManagementPage } from "./CustomerManagementPage";
import * as customersService from "../services/customers";
import * as sessionService from "../services/session";

jest.mock("../services/customers");
jest.mock("../services/session");

describe("CustomerManagementPage", () => {
  const baseProfile: customersService.CustomerProfile = {
    customerId: "cust-101",
    fullName: "Taylor Green",
    email: "taylor.green@example.com",
    mobile: "+61 412 000 111",
    status: "ACTIVE",
    joinedAt: "2026-06-01T00:00:00Z"
  };

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
          <CustomerManagementPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    jest.clearAllMocks();

    (sessionService.getNormalizedTokenRole as jest.MockedFunction<typeof sessionService.getNormalizedTokenRole>).mockReturnValue("CUSTOMER");
    (sessionService.setActiveCustomerId as jest.MockedFunction<typeof sessionService.setActiveCustomerId>).mockImplementation(() => undefined);
    (sessionService.clearActiveCustomerId as jest.MockedFunction<typeof sessionService.clearActiveCustomerId>).mockImplementation(() => undefined);
    (sessionService.clearAuthSession as jest.MockedFunction<typeof sessionService.clearAuthSession>).mockImplementation(() => undefined);

    (customersService.fetchCustomerProfile as jest.MockedFunction<typeof customersService.fetchCustomerProfile>).mockResolvedValue(baseProfile);
    (customersService.fetchCustomerDetails as jest.MockedFunction<typeof customersService.fetchCustomerDetails>).mockResolvedValue(baseProfile);
    (customersService.createCustomerProfile as jest.MockedFunction<typeof customersService.createCustomerProfile>).mockResolvedValue(baseProfile);
    (customersService.updateCustomerProfile as jest.MockedFunction<typeof customersService.updateCustomerProfile>).mockResolvedValue(baseProfile);
    (customersService.deleteCustomerProfile as jest.MockedFunction<typeof customersService.deleteCustomerProfile>).mockResolvedValue({
      status: "DELETED",
      message: "Customer deleted"
    });
  });

  it("applies customer-only profile controls", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { name: /Customer management/i })).toBeInTheDocument();

    expect(screen.queryByRole("heading", { name: /Create customer/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Status/i)).not.toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Close your customer account/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Close my customer account/i })).toBeInTheDocument();
  });

  it("submits customer profile updates without status", async () => {
    renderPage();

    fireEvent.change(screen.getByLabelText(/Legal name/i), {
      target: { value: "Taylor Green Updated" }
    });
    fireEvent.change(screen.getByLabelText(/Primary email/i), {
      target: { value: "taylor.updated@example.com" }
    });
    fireEvent.change(screen.getByLabelText(/Phone number/i), {
      target: { value: "+61 499 999 999" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Update customer/i }));

    await waitFor(() => {
      expect(customersService.updateCustomerProfile).toHaveBeenCalledWith(
        {
          legalName: "Taylor Green Updated",
          primaryEmail: "taylor.updated@example.com",
          phoneNumber: "+61 499 999 999",
          status: undefined
        },
        undefined
      );
    });
  });

  it("shows a confirmation modal before closing customer account", async () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Close my customer account/i }));

    expect(screen.getByRole("dialog", { name: /Confirm account closure/i })).toBeInTheDocument();
    expect(customersService.deleteCustomerProfile).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: /Cancel/i }));

    expect(screen.queryByRole("dialog", { name: /Confirm account closure/i })).not.toBeInTheDocument();
    expect(customersService.deleteCustomerProfile).not.toHaveBeenCalled();
  });

  it("allows customers to close their own customer account after confirmation", async () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Close my customer account/i }));
    fireEvent.click(screen.getByRole("button", { name: /Yes, close my account/i }));

    await waitFor(() => {
      expect(customersService.deleteCustomerProfile).toHaveBeenCalledWith(undefined);
      expect(sessionService.clearAuthSession).toHaveBeenCalledTimes(1);
    });
  });

  it("keeps create and status controls for ADMIN", async () => {
    (sessionService.getNormalizedTokenRole as jest.MockedFunction<typeof sessionService.getNormalizedTokenRole>).mockReturnValue("ADMIN");

    renderPage();

    expect(await screen.findByRole("heading", { name: /Create customer/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Status/i)).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /Close your customer account/i })).not.toBeInTheDocument();
  });
});
