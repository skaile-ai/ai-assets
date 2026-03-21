# Recipe: Local Development & API Tokens

This recipe covers managing your local Directus instance and obtaining static API tokens for your frontend.

## 1. Initial Login & Token Creation

While you can generate tokens via the UI, you can also do it programmatically. For local development, it's often easiest to use a static token.

### UI Method:
1. Log in to [http://localhost:8055/admin](http://localhost:8055/admin) using your `ADMIN_EMAIL` and `ADMIN_PASSWORD`.
2. Go to **User Directory** -> **Admin User**.
3. Scroll down to the **Token** field.
4. Generate or copy the token.

### CLI/Environment Method:
Add the token to your `.env` file for use in your scripts and frontend:

```env
DIRECTUS_URL="http://localhost:8055"
DIRECTUS_TOKEN="your-static-token"
```

## 2. Automatic Initialization Script

You can use a script to ensure Directus is up and ready before running other tasks (like type generation).

```python
# scripts/wait-for-directus.py
import requests
import time

URL = "http://localhost:8055/server/ping"

def wait():
    while True:
        try:
            response = requests.get(URL)
            if response.status_code == 200:
                print("Directus is up!")
                break
        except:
            pass
        print("Waiting for Directus...")
        time.sleep(2)

if __name__ == "__main__":
    wait()
```

## 3. Best Practices for Tokens

- **Static Tokens**: Use static tokens for service-to-service communication or local development.
- **JWT (Dynamic)**: Use the login flow in your frontend to obtain short-lived JWTs for user sessions.
- **Permissions**: Ensure the user associated with the token has the necessary permissions for the collections you want to access.
