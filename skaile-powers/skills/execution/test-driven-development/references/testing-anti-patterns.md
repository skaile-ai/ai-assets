# Testing Anti-Patterns

## Mocking Anti-Patterns

### Testing the mock instead of real behavior

The most common TDD mistake: asserting that a mock was called rather than asserting on observable output.

```typescript
// BAD: Tests mock not code
test('retry works', async () => {
  const mock = vi.fn()
    .mockRejectedValueOnce(new Error())
    .mockRejectedValueOnce(new Error())
    .mockResolvedValueOnce('success');
  await retryOperation(mock);
  expect(mock).toHaveBeenCalledTimes(3);
});

// GOOD: Tests observable behavior
test('retries failed operations 3 times', async () => {
  let attempts = 0;
  const operation = () => {
    attempts++;
    if (attempts < 3) throw new Error('fail');
    return 'success';
  };

  const result = await retryOperation(operation);

  expect(result).toBe('success');
  expect(attempts).toBe(3);
});
```

The bad test proves the mock was wired up; the good test proves the retry logic works.

### Mocking internal collaborators

Mock only at **system boundaries**: external APIs, databases, time, randomness, and the file system. Never mock your own classes, modules, or anything you control.

```typescript
// BAD: Mocks an internal service
test("checkout calls paymentService.process", async () => {
  const mockPayment = jest.mock(paymentService);
  await checkout(cart, payment);
  expect(mockPayment.process).toHaveBeenCalledWith(cart.total);
});

// GOOD: Tests through the public interface
test("user can checkout with valid cart", async () => {
  const cart = createCart();
  cart.add(product);
  const result = await checkout(cart, paymentMethod);
  expect(result.status).toBe("confirmed");
});
```

When you mock an internal collaborator, refactoring the internals will break tests even if behavior is unchanged.

### Mocking without understanding the dependency

Before mocking an external dependency, understand what it does and what shape it returns. Using a generic fetcher mock forces conditional logic inside the mock itself; a specific SDK-style interface is independently mockable without conditionals.

```typescript
// BAD: One generic function requires conditional mock logic
const api = {
  fetch: (endpoint, options) => fetch(endpoint, options),
};

// GOOD: Each function is independently mockable
const api = {
  getUser: (id) => fetch(`/users/${id}`),
  getOrders: (userId) => fetch(`/users/${userId}/orders`),
  createOrder: (data) => fetch('/orders', { method: 'POST', body: data }),
};
```

## Test Structure Anti-Patterns

### Vague or non-descriptive test names

A test name must describe the behavior being verified, not just state "it works".

```typescript
// BAD: Tells you nothing when it fails
test('retry works', ...);
test('test1', ...);

// GOOD: Failure message is self-explanatory
test('retries failed operations 3 times', ...);
test('rejects empty email', ...);
```

If "and" appears in a test name, the test is testing multiple behaviors. Split it.

### Testing multiple behaviors in one test

One test, one behavior. A test that asserts on five things fails for five possible reasons, making diagnosis harder and refactoring riskier.

```typescript
// BAD: Mixed concerns
test('validates email and domain and whitespace', async () => { ... });

// GOOD: Each behavior isolated
test('rejects email without @', async () => { ... });
test('rejects email with invalid domain', async () => { ... });
test('rejects email with leading whitespace', async () => { ... });
```

### Adding test-only methods to production classes

If you find yourself adding a method to a production class solely to make a test easier to write, the test is reaching into implementation details. Test through the public interface instead.

### Bypassing the interface to verify state

```typescript
// BAD: Bypasses interface to verify
test("createUser saves to database", async () => {
  await createUser({ name: "Alice" });
  const row = await db.query("SELECT * FROM users WHERE name = ?", ["Alice"]);
  expect(row).toBeDefined();
});

// GOOD: Verifies through interface
test("createUser makes user retrievable", async () => {
  const user = await createUser({ name: "Alice" });
  const retrieved = await getUser(user.id);
  expect(retrieved.name).toBe("Alice");
});
```

The bad test couples the test to the database schema. The good test survives any storage refactor that preserves the observable contract.

## Summary

| Anti-Pattern | Signal | Fix |
|---|---|---|
| Testing the mock | Asserting `toHaveBeenCalled` instead of output | Use real collaborators; assert on result |
| Mocking internals | Mocking your own classes/modules | Mock only at system boundaries |
| Generic fetcher mocks | Conditional logic inside mock setup | Use SDK-style interfaces |
| Vague test names | Name says "works" or "test1" | Describe the behavior precisely |
| Multiple behaviors per test | "and" in test name | One behavior per test |
| Test-only production methods | Adding getters/setters solely for tests | Test through the public API |
| Bypassing the interface | Direct DB/state queries in assertions | Verify via the same API callers use |
