import type { CustomerListItem } from "../services/customers";

export function formatCustomerScopeOption(customer: CustomerListItem): string {
  return `${customer.fullName} (${customer.customerId})`;
}

export function resolveCustomerIdFromScopeInput(input: string, customers: CustomerListItem[]): string {
  const trimmed = input.trim();
  if (!trimmed) {
    return "";
  }

  const optionCustomerId = extractCustomerIdFromOption(trimmed);
  if (optionCustomerId) {
    return optionCustomerId;
  }

  if (customers.length === 0) {
    return trimmed;
  }

  const normalized = trimmed.toLowerCase();

  const byId = customers.find((customer) => customer.customerId.toLowerCase() === normalized);
  if (byId) {
    return byId.customerId;
  }

  const byExactName = customers.find((customer) => customer.fullName.toLowerCase() === normalized);
  if (byExactName) {
    return byExactName.customerId;
  }

  const byPartialName = customers.filter((customer) => customer.fullName.toLowerCase().includes(normalized));
  if (byPartialName.length === 1) {
    return byPartialName[0].customerId;
  }

  if (/\s/.test(trimmed)) {
    return "";
  }

  return trimmed;
}

export function filterCustomersByNameOrId(customers: CustomerListItem[], query: string): CustomerListItem[] {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return customers;
  }

  return customers.filter((customer) => {
    return customer.fullName.toLowerCase().includes(normalized)
      || customer.customerId.toLowerCase().includes(normalized)
      || customer.email.toLowerCase().includes(normalized);
  });
}

function extractCustomerIdFromOption(value: string): string {
  const matched = value.match(/\(([^()]+)\)\s*$/);
  return matched?.[1]?.trim() ?? "";
}
