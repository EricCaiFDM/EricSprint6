import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { RegisterPage } from "./RegisterPage";
import * as api from "../services/api";
import * as customers from "../services/customers";
import * as session from "../services/session";

const mockNavigate = jest.fn();

jest.mock("react-router-dom", () => {
  const actual = jest.requireActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

jest.mock("../services/api");
jest.mock("../services/customers");

describe("RegisterPage", () => {
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
          <RegisterPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    window.localStorage.clear();
    mockNavigate.mockReset();
    jest.clearAllMocks();
  });

  it("registers and auto signs in customer role", async () => {
    const registerMock = api.register as jest.MockedFunction<typeof api.register>;
    const loginMock = api.login as jest.MockedFunction<typeof api.login>;
    const createCustomerProfileMock = customers.createCustomerProfile as jest.MockedFunction<
      typeof customers.createCustomerProfile
    >;

    registerMock.mockResolvedValue({ status: "CREATED", userId: "customer-1" });
    loginMock.mockResolvedValue({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresIn: 900
    });
    createCustomerProfileMock.mockResolvedValue({
      customerId: "customer-1",
      fullName: "Customer One",
      email: "customer1@example.com",
      mobile: "+61 400 000 001",
      status: "ACTIVE",
      joinedAt: "2026-06-01T00:00:00Z"
    });
    jest.spyOn(session, "getNormalizedTokenRole").mockReturnValue("CUSTOMER");

    renderPage();

    fireEvent.change(screen.getByLabelText(/Email address/i), {
      target: { value: "customer1@example.com" }
    });
    fireEvent.change(screen.getByLabelText(/Password \(min 8 characters\)/i), {
      target: { value: "secret123" }
    });
    fireEvent.change(screen.getByLabelText(/Legal name/i), {
      target: { value: "Customer One" }
    });
    fireEvent.change(screen.getByLabelText(/Mobile number/i), {
      target: { value: "+61 400 000 001" }
    });
    fireEvent.click(screen.getByRole("button", { name: /Create Account/i }));

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalled();
      expect(registerMock.mock.calls[0][0]).toEqual({
        email: "customer1@example.com",
        password: "secret123",
        passwordConfirmation: "secret123",
        role: "CUSTOMER"
      });
    });

    expect(loginMock).toHaveBeenCalled();
    expect(loginMock.mock.calls[0][0]).toEqual({
      identity: "customer1@example.com",
      password: "secret123"
    });
    expect(createCustomerProfileMock).toHaveBeenCalled();
    expect(createCustomerProfileMock.mock.calls[0][0]).toEqual({
      externalCustomerKey: "cust-customer1-example-com",
      legalName: "Customer One",
      primaryEmail: "customer1@example.com",
      phoneNumber: "+61 400 000 001"
    });
    expect(mockNavigate).toHaveBeenCalledWith("/customer/dashboard", { replace: true });
  });

  it("registers and auto signs in admin role", async () => {
    const registerMock = api.register as jest.MockedFunction<typeof api.register>;
    const loginMock = api.login as jest.MockedFunction<typeof api.login>;
    const createCustomerProfileMock = customers.createCustomerProfile as jest.MockedFunction<
      typeof customers.createCustomerProfile
    >;

    registerMock.mockResolvedValue({ status: "CREATED", userId: "admin-1" });
    loginMock.mockResolvedValue({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresIn: 900
    });
    jest.spyOn(session, "getNormalizedTokenRole").mockReturnValue("ADMIN");

    renderPage();

    fireEvent.change(screen.getByLabelText(/Email address/i), {
      target: { value: "admin1@example.com" }
    });
    fireEvent.change(screen.getByLabelText(/Password \(min 8 characters\)/i), {
      target: { value: "secret123" }
    });
    fireEvent.change(screen.getByLabelText(/Account type/i), {
      target: { value: "ADMIN" }
    });
    fireEvent.click(screen.getByRole("button", { name: /Create Account/i }));

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalled();
      expect(registerMock.mock.calls[0][0]).toEqual({
        email: "admin1@example.com",
        password: "secret123",
        passwordConfirmation: "secret123",
        role: "ADMIN"
      });
    });

    expect(loginMock).toHaveBeenCalled();
    expect(loginMock.mock.calls[0][0]).toEqual({
      identity: "admin1@example.com",
      password: "secret123"
    });
    expect(createCustomerProfileMock).not.toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith("/admin/dashboard", { replace: true });
  });

  it("shows role mismatch warning when selected role differs from signed-in role", async () => {
    const registerMock = api.register as jest.MockedFunction<typeof api.register>;
    const loginMock = api.login as jest.MockedFunction<typeof api.login>;
    const createCustomerProfileMock = customers.createCustomerProfile as jest.MockedFunction<
      typeof customers.createCustomerProfile
    >;
    const clearSessionSpy = jest.spyOn(session, "clearAuthSession");

    registerMock.mockResolvedValue({ status: "CREATED", userId: "admin-2" });
    loginMock.mockResolvedValue({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresIn: 900
    });
    jest.spyOn(session, "getNormalizedTokenRole").mockReturnValue("CUSTOMER");

    renderPage();

    fireEvent.change(screen.getByLabelText(/Email address/i), {
      target: { value: "admin2@example.com" }
    });
    fireEvent.change(screen.getByLabelText(/Password \(min 8 characters\)/i), {
      target: { value: "secret123" }
    });
    fireEvent.change(screen.getByLabelText(/Account type/i), {
      target: { value: "ADMIN" }
    });
    fireEvent.click(screen.getByRole("button", { name: /Create Account/i }));

    expect(await screen.findByText(/Access role mismatch/i)).toBeInTheDocument();
    expect(clearSessionSpy).toHaveBeenCalled();
    expect(createCustomerProfileMock).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it("rejects customer sign-up when mobile number length is invalid", async () => {
    const registerMock = api.register as jest.MockedFunction<typeof api.register>;
    const createCustomerProfileMock = customers.createCustomerProfile as jest.MockedFunction<
      typeof customers.createCustomerProfile
    >;

    renderPage();

    fireEvent.change(screen.getByLabelText(/Email address/i), {
      target: { value: "customer2@example.com" }
    });
    fireEvent.change(screen.getByLabelText(/Password \(min 8 characters\)/i), {
      target: { value: "secret123" }
    });
    fireEvent.change(screen.getByLabelText(/Legal name/i), {
      target: { value: "Customer Two" }
    });
    fireEvent.change(screen.getByLabelText(/Mobile number/i), {
      target: { value: "12345" }
    });
    fireEvent.click(screen.getByRole("button", { name: /Create Account/i }));

    expect(await screen.findByText("Phone number must be between 7 and 32 characters.")).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
    expect(createCustomerProfileMock).not.toHaveBeenCalled();
  });
});
