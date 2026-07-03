export function formatCurrency(amount: number, currency: string): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount);
}

export function formatDate(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("en-AU", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(parsed);
}

export function formatDateTime(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("en-AU", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(parsed);
}

export function formatStatementPeriod(periodYearMonth: string): string {
  const value = periodYearMonth?.trim() ?? "";
  const match = value.match(/^(\d{4})-(0[1-9]|1[0-2])$/);
  if (!match) {
    return value || periodYearMonth;
  }

  const year = Number(match[1]);
  const monthIndex = Number(match[2]) - 1;
  const periodStart = new Date(year, monthIndex, 1, 0, 0, 0, 0);
  const periodEnd = new Date(year, monthIndex + 1, 0, 0, 0, 0, 0);

  return `${match[1]}-${match[2]} (${formatLocalDate(periodStart)} to ${formatLocalDate(periodEnd)})`;
}

function formatLocalDate(value: Date): string {
  return new Intl.DateTimeFormat("en-AU", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(value);
}
