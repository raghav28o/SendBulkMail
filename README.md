# Bulk Mailer Pro

A robust, Spring Boot-based bulk email scheduling system with built-in throttling, open tracking, Google Sheets integration, and automated bounce handling. Now powered by **PostgreSQL** for reliable data persistence.

## 🚀 Features

- **Throttled Bulk Sending**: Schedule thousands of emails with custom daily limits and per-email delays to prevent spam flagging.
- **Smart Scheduling**: Set a daily start time; the system automatically picks up where it left off each day until the batch is complete.
- **Google Sheets Integration**: Fetch recipient lists directly from any public Google Sheet by simply providing the URL and sheet number.
- **Real-time Open Tracking**: Track exactly when and how many times a recipient opens your email using a hidden tracking pixel.
- **Automated Bounce Handling**: Background service monitors your IMAP inbox for delivery failure notifications and marks invalid addresses in the database.
- **Modern Dashboard**: A clean Thymeleaf & Bootstrap UI to manage campaigns, view detailed recipient statuses, and monitor real-time statistics.
- **Docker Ready**: Includes `Dockerfile` and `docker-compose.yml` for easy deployment.

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.x, Spring Data JPA, Java Mail Sender
- **Database**: PostgreSQL 15+
- **Frontend**: Thymeleaf, Bootstrap 5
- **Integrations**: Google Sheets API v4, Jakarta Mail (IMAP)
- **Containerization**: Docker, Docker Compose

## 📋 Prerequisites

- **Java 17** or higher (if running locally)
- **PostgreSQL Server** (or Docker)
- **Google Cloud API Key** (with Sheets API enabled)
- **Gmail Account** with:
    - **App Password** generated (for SMTP and IMAP)
    - **IMAP Enabled** in Gmail settings

## ⚙️ Configuration

The application uses environment variables for configuration. You can set these in your system or use a `.env` file (see `docker-compose.yml`).

### Environment Variables

| Variable | Description | Example |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/bulkmailsend` |
| `SPRING_DATASOURCE_USERNAME` | DB Username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB Password | `your_password` |
| `SMTP_USERNAME` | Gmail Email Address | `your-email@gmail.com` |
| `SMTP_PASSWORD` | Gmail App Password | `your-16-char-password` |
| `GOOGLE_API_KEY` | Google Cloud API Key | `AIza...` |
| `APP_BASE_URL` | Public URL for tracking | `http://localhost:9080` |
| `APP_PERSONAL_NAME` | Sender Name | `Raghav Agarwal` |

*Note: For open tracking to work, `APP_BASE_URL` must be a publicly accessible URL if sending to external recipients.*

## 🚀 Getting Started

### Option 1: Using Docker (Recommended)

1. Create a `.env` file in the root directory with the variables listed above.
2. Run the following command:
   ```bash
   docker-compose up --build
   ```
3. Access the dashboard at `http://localhost:9080`.

### Option 2: Running Locally

1. **Setup PostgreSQL**: Create a database named `bulkmailsend`.
2. **Set Environment Variables**: Export the required variables in your terminal.
3. **Run the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Access the Dashboard**: Open `http://localhost:9080` in your browser.

## 📖 How to Use

1. **Start the Application**: Follow the steps above.
2. **Access the Dashboard**: Open `http://localhost:9080`.
3. **Schedule a Campaign**:
    - Go to "Schedule New".
    - Provide a subject and HTML body.
    - **Manual Entry**: Paste emails into the recipients box.
    - **Google Sheets**: Paste your Sheet URL, enter the sheet number (e.g., 1), and click "Fetch Emails".
    - Set your Daily Limit (e.g., 500) and Start Time.
4. **Monitor**: View the "Batch Details" to see progress, opens, and bounces in real-time.

## 🏗️ Architecture

- **Persistence**: Every batch and recipient is stored in **PostgreSQL**. If the server restarts, the system resumes automatically.
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
