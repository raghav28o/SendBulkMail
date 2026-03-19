# Bulk Mailer Pro

A robust, Spring Boot-based bulk email scheduling system with built-in throttling, open tracking, Google Sheets integration, and automated bounce handling.

## 🚀 Features

- **Throttled Bulk Sending**: Schedule thousands of emails with custom daily limits and per-email delays to prevent spam flagging.
- **Smart Scheduling**: Set a daily start time; the system automatically picks up where it left off each day until the batch is complete.
- **Google Sheets Integration**: Fetch recipient lists directly from any public Google Sheet by simply providing the URL and sheet number.
- **Real-time Open Tracking**: Track exactly when and how many times a recipient opens your email using a hidden tracking pixel.
- **Automated Bounce Handling**: Background service monitors your IMAP inbox for delivery failure notifications and marks invalid addresses in the database.
- **Modern Dashboard**: A clean Thymeleaf & Bootstrap UI to manage campaigns, view detailed recipient statuses, and monitor real-time statistics.

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.x, Spring Data JPA, Java Mail Sender
- **Database**: MySQL
- **Frontend**: Thymeleaf, Bootstrap 5
- **Integrations**: Google Sheets API v4, Jakarta Mail (IMAP)

## 📋 Prerequisites

- **Java 17** or higher
- **MySQL Server**
- **Google Cloud API Key** (with Sheets API enabled)
- **Gmail Account** with:
    - **App Password** generated (for SMTP and IMAP)
    - **IMAP Enabled** in Gmail settings

## ⚙️ Configuration

Update `src/main/resources/application.properties` with your credentials:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/BulkMailSend
spring.datasource.username=root
spring.datasource.password=your_password

# Gmail SMTP & IMAP
spring.mail.username=your-email@gmail.com
spring.mail.password=your-16-character-app-password

# Google Sheets API
google.api.key=your_google_cloud_api_key

# App Settings
app.base-url=http://localhost:9080
```

*Note: For open tracking to work, `app.base-url` must be a publicly accessible URL if sending to external recipients.*

## 📖 How to Use

1. **Start the Application**: Run `./mvnw spring-boot:run`.
2. **Access the Dashboard**: Open `http://localhost:9080` in your browser.
3. **Schedule a Campaign**:
    - Go to "Schedule New".
    - Provide a subject and HTML body.
    - **Manual Entry**: Paste emails into the recipients box.
    - **Google Sheets**: Paste your Sheet URL, enter the sheet number (e.g., 1), and click "Fetch Emails".
    - Set your Daily Limit (e.g., 500) and Start Time.
4. **Monitor**: View the "Batch Details" to see progress, opens, and bounces in real-time.

## 🏗️ Architecture

- **Persistence**: Every batch and recipient is stored in MySQL. If the server restarts, the system resumes automatically.
- **Concurrency**: Sending is handled in background `@Async` threads to keep the UI responsive.
- **Scheduler**: A cron job runs every minute to trigger active batches at their designated `startTime`.
- **Tracking**: A `1x1` transparent GIF is injected into HTML emails. When loaded by the client, it hits the `/api/track/open/{id}` endpoint.
- **Bounce Logic**: The `BounceHandlerService` logs into your IMAP folder every 5 minutes, searches for "Failure" subjects, and parses the body for failed emails.

## 🤝 Contributing

1. Fork the project.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.
