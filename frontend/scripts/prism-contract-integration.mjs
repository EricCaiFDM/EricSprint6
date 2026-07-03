import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "..", "..");

const UUID_A = "11111111-1111-4111-8111-111111111111";
const UUID_B = "22222222-2222-4222-8222-222222222222";
const NOW_ISO = "2026-07-01T00:00:00Z";

const suites = [
  {
    id: "authentication",
    name: "Authentication",
    contract: "specs/002-authentication/contracts/openapi.yaml",
    cases: [
      {
        name: "register returns 201",
        method: "POST",
        path: "/auth/register",
        preferStatus: 201,
        expectStatus: 201,
        expectJson: true,
        expectFields: ["status", "userId"],
        body: {
          email: "prism-user@example.com",
          password: "secret123",
          passwordConfirmation: "secret123"
        }
      },
      {
        name: "login unauthorized returns 401",
        method: "POST",
        path: "/auth/login",
        preferStatus: 401,
        expectStatus: 401,
        expectJson: true,
        expectFields: ["code", "message"],
        body: {
          identity: "missing@example.com",
          password: "wrong-pass"
        }
      }
    ]
  },
  {
    id: "customer-management",
    name: "Customer Management",
    contract: "specs/003-customer-management/contracts/openapi.yaml",
    cases: [
      {
        name: "create customer returns 201",
        method: "POST",
        path: "/customers",
        preferStatus: 201,
        expectStatus: 201,
        expectJson: true,
        expectFields: ["customerId", "status"],
        body: {
          externalCustomerKey: "ext-prism-001",
          legalName: "Prism Customer",
          primaryEmail: "prism.customer@example.com",
          phoneNumber: "+61123456789",
          password: "secret123"
        }
      },
      {
        name: "delete blocked returns 409",
        method: "DELETE",
        path: `/customers/${UUID_A}`,
        preferStatus: 409,
        expectStatus: 409,
        expectJson: true,
        expectFields: ["code", "message"]
      }
    ]
  },
  {
    id: "account-management",
    name: "Account Management",
    contract: "specs/004-account-management/contracts/openapi.yaml",
    cases: [
      {
        name: "create account returns 201",
        method: "POST",
        path: "/accounts",
        preferStatus: 201,
        expectStatus: 201,
        expectJson: true,
        expectFields: ["accountId", "status"],
        body: {
          customerId: UUID_A,
          accountType: "CHECKING",
          currencyCode: "AUD",
          nickname: "Prism Checking"
        }
      },
      {
        name: "delete blocked returns 409",
        method: "DELETE",
        path: `/accounts/${UUID_A}`,
        preferStatus: 409,
        expectStatus: 409,
        expectJson: true,
        expectFields: ["code", "message"]
      }
    ]
  },
  {
    id: "transaction-operations",
    name: "Transaction Operations",
    contract: "specs/005-transaction-operations/contracts/openapi.yaml",
    cases: [
      {
        name: "deposit returns 201",
        method: "POST",
        path: "/transactions/deposit",
        preferStatus: 201,
        expectStatus: 201,
        expectJson: true,
        expectFields: ["transactionId", "transactionType"],
        headers: {
          "Idempotency-Key": "idem-prism-deposit-001"
        },
        body: {
          accountId: UUID_A,
          amount: "25.00"
        }
      },
      {
        name: "withdrawal insufficient funds returns 422",
        method: "POST",
        path: "/transactions/withdrawal",
        preferStatus: 422,
        expectStatus: 422,
        expectJson: false,
        headers: {
          "Idempotency-Key": "idem-prism-withdrawal-001"
        },
        body: {
          accountId: UUID_A,
          amount: "9999.99"
        }
      },
      {
        name: "history returns 200",
        method: "GET",
        path: `/transactions/history?scopeType=ACCOUNT&scopeId=${UUID_A}&page=1&pageSize=20`,
        preferStatus: 200,
        expectStatus: 200,
        expectJson: true,
        expectFields: ["items", "page", "pageSize"]
      }
    ]
  },
  {
    id: "standing-orders",
    name: "Standing Orders",
    contract: "specs/006-standing-orders/contracts/openapi.yaml",
    cases: [
      {
        name: "create standing order returns 201",
        method: "POST",
        path: "/standing-orders",
        preferStatus: 201,
        expectStatus: 201,
        expectJson: true,
        expectFields: ["standingOrderId", "lifecycleState"],
        body: {
          sourceAccountId: UUID_A,
          destinationAccountId: UUID_B,
          amount: "10.00",
          cadence: "MONTHLY",
          effectiveFromUtc: NOW_ISO
        }
      },
      {
        name: "pause missing standing order returns 404",
        method: "POST",
        path: `/standing-orders/${UUID_A}/pause`,
        preferStatus: 404,
        expectStatus: 404,
        expectJson: false
      }
    ]
  },
  {
    id: "notifications",
    name: "Notifications",
    contract: "specs/007-notifications/contracts/openapi.yaml",
    cases: [
      {
        name: "trigger notification returns 202",
        method: "POST",
        path: "/notifications/events",
        preferStatus: 202,
        expectStatus: 202,
        expectJson: true,
        expectFields: ["notificationEventId", "status"],
        body: {
          eventType: "TRANSACTION_POSTED",
          recipientScopeType: "CUSTOMER",
          recipientScopeId: UUID_A,
          templateCode: "TXN_ALERT",
          templateContext: {
            amount: "10.00",
            currencyCode: "AUD"
          }
        }
      },
      {
        name: "event not found returns 404",
        method: "GET",
        path: `/notifications/events/${UUID_A}`,
        preferStatus: 404,
        expectStatus: 404,
        expectJson: false
      },
      {
        name: "notification feed returns 200",
        method: "GET",
        path: "/notifications/events?size=6",
        preferStatus: 200,
        expectStatus: 200,
        expectJson: true,
        expectArray: true
      }
    ]
  },
  {
    id: "monthly-statements",
    name: "Monthly Statements",
    contract: "specs/008-monthly-statements/contracts/openapi.yaml",
    cases: [
      {
        name: "generate statement returns 202",
        method: "POST",
        path: "/statements/generate",
        preferStatus: 202,
        expectStatus: 202,
        expectJson: true,
        expectFields: ["statementId", "generationStatus"],
        body: {
          accountId: UUID_A,
          periodYearMonth: "2026-06",
          generationMode: "STANDARD"
        }
      },
      {
        name: "statement missing returns 404",
        method: "GET",
        path: `/statements/${UUID_A}`,
        preferStatus: 404,
        expectStatus: 404,
        expectJson: true,
        expectFields: ["code", "message"]
      },
      {
        name: "statement list returns 200",
        method: "GET",
        path: `/statements?accountId=${UUID_A}&periodYearMonth=2026-06&page=1&pageSize=20`,
        preferStatus: 200,
        expectStatus: 200,
        expectJson: true,
        expectFields: ["items", "page", "pageSize"]
      }
    ]
  },
  {
    id: "spending-insights",
    name: "Spending Insights",
    contract: "specs/009-spending-insights/contracts/openapi.yaml",
    cases: [
      {
        name: "insights generated returns 200",
        method: "GET",
        path: `/insights/spending?scopeType=CUSTOMER&scopeId=${UUID_A}`,
        preferStatus: 200,
        expectStatus: 200,
        expectJson: true,
        expectFields: ["periodLabel", "status", "categories"]
      },
      {
        name: "invalid category filter returns 400",
        method: "GET",
        path: `/insights/spending?scopeType=CUSTOMER&scopeId=${UUID_A}&categoryFilters=TRAVEL`,
        preferStatus: 400,
        expectStatus: 400,
        expectJson: true,
        expectFields: ["code", "message"]
      }
    ]
  }
];

async function main() {
  let failures = 0;

  for (let i = 0; i < suites.length; i += 1) {
    const suite = suites[i];
    const port = 4100 + i;

    try {
      await runSuite(suite, port);
      console.log(`PASS ${suite.name}`);
    } catch (error) {
      failures += 1;
      console.error(`FAIL ${suite.name}`);
      console.error(error instanceof Error ? error.message : String(error));
    }
  }

  if (failures > 0) {
    process.exitCode = 1;
    throw new Error(`${failures} Prism suite(s) failed.`);
  }

  console.log(`PASS All ${suites.length} Prism contract suites passed.`);
}

async function runSuite(suite, port) {
  const contractPath = path.resolve(repoRoot, suite.contract);
  const originalContract = await readFile(contractPath, "utf8");
  const normalizedContract = normalizeContractYaml(originalContract);

  const tempDir = await mkdtemp(path.join(os.tmpdir(), `prism-${suite.id}-`));
  const tempContractPath = path.join(tempDir, `${suite.id}.openapi.yaml`);
  await writeFile(tempContractPath, normalizedContract, "utf8");

  const prism = startPrism(tempContractPath, port);
  const baseUrl = `http://127.0.0.1:${port}`;

  try {
    await waitForPrism(baseUrl, prism);

    for (const testCase of suite.cases) {
      await executeCase(baseUrl, testCase);
      console.log(`  PASS ${testCase.name}`);
    }
  } finally {
    await stopPrism(prism.child);
    await rm(tempDir, { recursive: true, force: true });
  }
}

function normalizeContractYaml(content) {
  return content
    .split(/\r?\n/)
    .map((line) => line.replace(/^[ \t]+/, (indent) => indent.replace(/\t/g, "  ")))
    .join("\n");
}

function startPrism(contractPath, port) {
  const escapedContractPath = contractPath.replace(/"/g, '\\"');
  const command = `npx prism mock "${escapedContractPath}" --host 127.0.0.1 --port ${port} --errors`;
  const child = spawn(command, {
    cwd: path.resolve(repoRoot, "frontend"),
    stdio: ["ignore", "pipe", "pipe"],
    shell: true
  });

  let output = "";
  child.stdout.on("data", (chunk) => {
    output += chunk.toString();
  });
  child.stderr.on("data", (chunk) => {
    output += chunk.toString();
  });

  return {
    child,
    getOutput: () => output
  };
}

async function waitForPrism(baseUrl, prism, timeoutMs = 15000) {
  const start = Date.now();

  while (Date.now() - start < timeoutMs) {
    if (prism.child.exitCode !== null) {
      throw new Error(
        `Prism exited early with code ${prism.child.exitCode}.\n${prism.getOutput()}`
      );
    }

    try {
      await fetch(`${baseUrl}/__prism_ready_probe`, { method: "GET" });
      return;
    } catch {
      await delay(250);
    }
  }

  throw new Error(`Timed out waiting for Prism server at ${baseUrl}.\n${prism.getOutput()}`);
}

async function executeCase(baseUrl, testCase) {
  const headers = {
    Accept: "application/json",
    Authorization: "Bearer prism-test-token",
    ...(testCase.headers ?? {})
  };

  if (testCase.preferStatus) {
    headers.Prefer = `code=${testCase.preferStatus}`;
  }

  let body;
  if (testCase.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(testCase.body);
  }

  const response = await fetch(`${baseUrl}${testCase.path}`, {
    method: testCase.method,
    headers,
    body
  });

  const rawBody = await response.text();

  if (response.status !== testCase.expectStatus) {
    throw new Error(
      [
        `${testCase.method} ${testCase.path} expected ${testCase.expectStatus} but got ${response.status}.`,
        rawBody.length > 0 ? `Response body: ${rawBody.slice(0, 500)}` : "Response body: <empty>"
      ].join("\n")
    );
  }

  if (!testCase.expectJson) {
    return;
  }

  if (rawBody.length === 0) {
    throw new Error(`${testCase.method} ${testCase.path} expected JSON body but response was empty.`);
  }

  let parsed;
  try {
    parsed = JSON.parse(rawBody);
  } catch (error) {
    throw new Error(
      `${testCase.method} ${testCase.path} expected JSON body but parsing failed: ${
        error instanceof Error ? error.message : String(error)
      }`
    );
  }

  if (testCase.expectArray) {
    if (!Array.isArray(parsed)) {
      throw new Error(`${testCase.method} ${testCase.path} expected an array response.`);
    }
    return;
  }

  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw new Error(`${testCase.method} ${testCase.path} expected an object response.`);
  }

  if (Array.isArray(testCase.expectFields) && testCase.expectFields.length > 0) {
    for (const field of testCase.expectFields) {
      if (!(field in parsed)) {
        throw new Error(`${testCase.method} ${testCase.path} response missing required field '${field}'.`);
      }
    }
  }
}

async function stopPrism(child) {
  if (child.exitCode !== null) {
    return;
  }

  child.kill("SIGTERM");

  await Promise.race([
    onceProcessExit(child),
    delay(2000)
  ]);

  if (child.exitCode === null) {
    child.kill("SIGKILL");
    await Promise.race([
      onceProcessExit(child),
      delay(1000)
    ]);
  }
}

function onceProcessExit(child) {
  return new Promise((resolve) => {
    child.once("exit", () => resolve());
  });
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
