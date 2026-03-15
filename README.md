# Threads Scheduled Auto Posting App (Spring MVC + Docker)

A minimal **Spring Boot (Spring MVC)** application that automatically posts to Threads and runs inside **Docker**.

---

# 🚀 Run the Application

Start the application with Docker Compose.

## macOS / Linux

```bash
THREADS_ACCESS_TOKEN=your_access_token docker compose up
````

## Windows (PowerShell)

```powershell
$env:THREADS_ACCESS_TOKEN="your_access_token"; docker compose up
```

After the container starts, open:

```
http://localhost:8080
```

---

# 💾 Persistent Data

Application data is stored inside the `/data` directory.

Docker Compose mounts this folder to the host to **prevent data loss when the container stops**.

Example:

```
./data:/data
```

---

# 🧵 Threads API Configuration

The application reads the token from an environment variable:

```
THREADS_ACCESS_TOKEN
```

In Spring Boot this is mapped as:

```properties
threads.access.token=${THREADS_ACCESS_TOKEN}
```

---

# 🔑 Getting a Threads Access Token

To use the **Threads API**, your application needs an **Access Token**.  
This token acts like a temporary key that allows your app to make API requests.

---

# 1. Create a Meta Developer Account

If you don’t already have one, register as a Meta Developer.

1. Log in with your Facebook account.
2. Follow the registration steps here:

https://developers.facebook.com/docs/development/register

Once completed, you will have access to the **Meta Developer Dashboard**.

---

# 2. Create a Threads App

Go to the Apps dashboard:

https://developers.facebook.com/apps

Steps:

1. Click **Create App**
2. When asked about a **Business Portfolio**, you can skip this step
3. Select the **Threads** use case
4. Enter:
   - **App Name**
   - **Contact Email**
5. Click **Create App**

You will be redirected to your **App Dashboard**.

---

# 3. Enable Threads API

Inside the **App Dashboard**:

1. Click **Access the Threads API**
2. Select the permissions you want.

For basic usage, only enable:

```

threads_basic
threads_manage_insights

```

These permissions allow your app to **read data from Threads**.

Additional permissions exist for posting content or managing replies, but they are not required for basic testing.

---

# 4. Add a Threads Tester

Your app needs permission to access your Threads account.

Steps:

1. Open **Settings**
2. Click **Add or Remove Threads Testers**
3. This will open the **App Roles** page
4. Click **Add People**
5. Choose **Threads Tester**
6. Search for your **Threads username**
7. Add it

---

# 5. Accept the Tester Invitation

Now open your **Threads account** and accept the invitation.

Steps:

1. Go to **Settings**
2. Select **Account**
3. Open **Website Permissions**
4. Click **Invites**
5. Accept the invitation from your app

After accepting, your app is allowed to run API calls for that Threads account.

---

# 6. Generate an Access Token

Go back to your **App Dashboard** and open the testing tools.

Click:

```

Open Graph API Explorer

```

Steps:

1. Make sure the API is set to **Threads**
2. Click **Generate Threads Access Token**
3. Confirm the authorization popup

You will receive a **Short-Lived Access Token**.

Example:

```

EAAGxxxxxxxxxxxxxxxxxxxxxxxx

```

This token usually expires in about **1 hour**.

---

# 7. Use the Token for API Requests

The access token is required for all API calls.

Example request:

```

GET [https://graph.threads.net/v1.0/me?access_token=YOUR_ACCESS_TOKEN](https://graph.threads.net/v1.0/me?access_token=YOUR_ACCESS_TOKEN)

```

If the token is valid, the API will return information about your Threads user.

---

# 8. Get a Long-Lived Token

For production applications, you should convert the short-lived token into a **long-lived token**.

Long-lived tokens usually last about **60–90 days**.

Documentation:

https://developers.facebook.com/docs/threads/get-started/long-lived-tokens

---

# ⚠️ Important Notes

- Treat access tokens like **passwords**
- Do **not commit tokens to Git**
- Short-lived tokens are safer for testing
- Long-lived tokens are recommended for production apps