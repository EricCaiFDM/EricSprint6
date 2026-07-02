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
    (customersService.fetchCustomersForAdmin as jest.MockedFunction<typeof customersService.fetchCustomersForAdmin>).mockResolvedValue([
      {
        customerId: "cust-101",
        externalCustomerKey: "ext-101",
        fullName: "Taylor Green",
        email: "taylor.green@example.com",
        mobile: "+61 412 000 111",
        status: "ACTIVE",
        joinedAt: "2026-06-01T00:00:00Z"
      }
    ]);
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

    fireEvent.change(screen.getByLabelText(/^Legal name$/i), {
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

  it("shows inline update validation errors for short phone input", async () => {
    renderPage();

    fireEvent.change(screen.getByLabelText(/Phone number/i), {
      target: { value: "12345" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Update customer/i }));

    expect(
      await screen.findByText("Phone number must be between 7 and 32 characters.")
    ).toBeInTheDocument();
    expect(customersService.updateCustomerProfile).not.toHaveBeenCalled();
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

  it("resolves admin target scope by customer name for updates", async () => {
    (sessionService.getNormalizedTokenRole as jest.MockedFunction<typeof sessionService.getNormalizedTokenRole>).mockReturnValue("ADMIN");

    renderPage();

    fireEvent.change(await screen.findByLabelText(/Target customer name or ID/i), {
      target: { value: "Taylor Green" }
    });

    fireEvent.change(screen.getAllByPlaceholderText(/No change/i)[0], {
      target: { value: "Taylor Green Updated" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Update customer/i }));

    await waitFor(() => {
      expect(customersService.updateCustomerProfile).toHaveBeenCalledWith(
        expect.objectContaining({ legalName: "Taylor Green Updated" }),
        "cust-101"
      );
    });
  });

  it("submits password when admin creates customer profile", async () => {
    (sessionService.getNormalizedTokenRole as jest.MockedFunction<typeof sessionService.getNormalizedTokenRole>).mockReturnValue("ADMIN");

    renderPage();

    fireEvent.change(await screen.findByLabelText(/External customer key/i), {
      target: { value: "ext-admin-900" }
    });

    fireEvent.change(screen.getAllByLabelText(/^Legal name$/i)[0], {
      target: { value: "Jamie Admin Created" }
    });

    fireEvent.change(screen.getAllByLabelText(/Primary email/i)[0], {
      target: { value: "jamie.created@example.com" }
    });

    fireEvent.change(screen.getAllByLabelText(/Phone number/i)[0], {
      target: { value: "+61 411 222 333" }
    });

    fireEvent.change(screen.getByLabelText(/Temporary password/i), {
      target: { value: "secret123" }
    });

    fireEvent.click(screen.getByRole("button", { name: /Create customer/i }));

    await waitFor(() => {
      expect(customersService.createCustomerProfile).toHaveBeenCalled();
      const firstCall = (customersService.createCustomerProfile as jest.Mock).mock.calls[0]?.[0];
      expect(firstCall).toEqual(expect.objectContaining({
        externalCustomerKey: "ext-admin-900",
        legalName: "Jamie Admin Created",
        primaryEmail: "jamie.created@example.com",
        phoneNumber: "+61 411 222 333",
        password: "secret123"
      }));
    });
  });

  it("allows admins to switch selected customers from dropdown without clearing search", async () => {
    (sessionService.getNormalizedTokenRole as jest.MockedFunction<typeof sessionService.getNormalizedTokenRole>).mockReturnValue("ADMIN");

    (customersService.fetchCustomersForAdmin as jest.MockedFunction<typeof customersService.fetchCustomersForAdmin>).mockResolvedValue([
      {
        customerId: "cust-101",
        externalCustomerKey: "ext-101",
        fullName: "Taylor Green",
        email: "taylor.green@example.com",
        mobile: "+61 412 000 111",
        status: "ACTIVE",
        joinedAt: "2026-06-01T00:00:00Z"
      },
      {
        customerId: "cust-202",
        externalCustomerKey: "ext-202",
        fullName: "Taylor Brown",
        email: "taylor.brown@example.com",
        mobile: "+61 422 000 222",
        status: "ACTIVE",
        joinedAt: "2026-06-05T00:00:00Z"
      }
    ]);

    renderPage();

    fireEvent.change(await screen.findByLabelText(/Target customer name or ID/i), {
      target: { value: "Taylor" }
    });

    const matchingCustomers = screen.getByLabelText(/Matching customers/i);

    fireEvent.change(matchingCustomers, {
      target: { value: "cust-101" }
    });

    await waitFor(() => {
      expect(customersService.fetchCustomerDetails).toHaveBeenCalledWith("cust-101");
    });

    expect(screen.getByLabelText(/Target customer name or ID/i)).toHaveValue("Taylor");

    fireEvent.change(matchingCustomers, {
      target: { value: "cust-202" }
    });

    await waitFor(() => {
      expect(customersService.fetchCustomerDetails).toHaveBeenCalledWith("cust-202");
    });

    expect(screen.getByLabelText(/Target customer name or ID/i)).toHaveValue("Taylor");
  });
});
