export const PHONE_NUMBER_MIN_LENGTH = 7;
export const PHONE_NUMBER_MAX_LENGTH = 32;

const PHONE_NUMBER_ALLOWED_PATTERN = /^[0-9+()\-\s]+$/;

export function validatePhoneNumber(value: string, required = false): string | null {
  const trimmed = value.trim();

  if (trimmed.length === 0) {
    return required ? "Phone number is required." : null;
  }

  if (trimmed.length < PHONE_NUMBER_MIN_LENGTH || trimmed.length > PHONE_NUMBER_MAX_LENGTH) {
    return `Phone number must be between ${PHONE_NUMBER_MIN_LENGTH} and ${PHONE_NUMBER_MAX_LENGTH} characters.`;
  }

  if (!PHONE_NUMBER_ALLOWED_PATTERN.test(trimmed)) {
    return "Phone number may contain only digits, spaces, +, -, and parentheses.";
  }

  return null;
}
