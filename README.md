# auto-post (Spring MVC + Docker)

A minimal Spring MVC (Spring Boot) application that runs inside Docker.

---

## 🚀 Run with Docker Compose

Start the application using Docker Compose:

```bash
docker compose up
````

Then open:

```
http://localhost:8080
```

---

## 💾 Persistent Data

To prevent data loss when the container stops, mount the `/data` directory to the host.

---

## 🧵 Threads API Config

Add the Threads access token to your application configuration.

Example (`application.properties`):

```properties
# Threads API Config
threads.access.token=YOUR_LONG_LIVED_ACCESS_TOKEN
```

---

## 🔑 How to Get Long-Lived Threads Access Token

### 1. Create a Developer App

Go to:

```
https://developers.facebook.com/apps
```

Create a new app in the Meta developer dashboard.

---

### 2. Add Threads API

Inside the app dashboard:

```
Add Product → Threads API
```

---

### 3. Generate a Short-Lived Token

Open Graph API Explorer:

```
https://developers.facebook.com/tools/explorer
```

Select your app and generate a **User Access Token** with these permissions:

```
threads_basic
threads_content_publish
threads_manage_replies
threads_read_replies
```

---

### 4. Exchange for a Long-Lived Token

Use the following request:

```
https://graph.facebook.com/v19.0/oauth/access_token
?grant_type=fb_exchange_token
&client_id=YOUR_APP_ID
&client_secret=YOUR_APP_SECRET
&fb_exchange_token=SHORT_LIVED_TOKEN
```

The response will contain a **long-lived token** (valid for about 60 days).

---

### 5. Add Token to the Application

Example:

```properties
threads.access.token=EAAGxxxxxxxxxxxxxxxx
```
