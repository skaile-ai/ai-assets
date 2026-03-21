# Use Cases for Context7 API

## 1. Searching for an Unknown Library
If you need to find a library but only know its general name, use the `search` command.
```bash
use-context7-api search react
```

## 2. Fetching Documentation with Examples
When you need specific usage examples or want to learn how to use an API (e.g. creating middleware in Express).
```bash
use-context7-api docs /expressjs/express/v4.18.0 --query "routing and middleware"
```

## 3. Answering General Developer Queries
If asked how to implement a specific feature using a popular package.
1. Run `search` to get the library ID.
2. Run `docs <library-id> --query "<question>"`.
3. Use the returned markdown to formulate your response or write code.
