# ResumeAnalyzer – AI-Powered Resume Evaluation System

ResumeAnalyzer is a Java-based application designed to simplify the resume screening process. It uses Google Gemini AI to analyze candidate resumes against job requirements, evaluate skill compatibility, and generate structured evaluation results.

The application supports individual and bulk resume evaluation, making it easier to assess multiple candidates efficiently.

## Features

- **AI-Powered Resume Analysis:** Uses Google Gemini to analyze resumes and identify relevant skills.
- **Skill Matching:** Evaluates candidates based on skill match, recency, and frequency.
- **Project Complexity Evaluation:** Assesses the technical complexity of projects mentioned in resumes.
- **Automated Scoring:** Calculates evaluation scores using a weighted scoring mechanism implemented in Java.
- **Bulk Processing:** Supports evaluating multiple resumes and job requirements.
- **Concurrent Processing:** Uses Java CompletableFuture and ExecutorService for parallel task execution.
- **Result Caching:** Stores previous evaluations to avoid unnecessary repeated AI API calls.
- **Database Integration:** Uses PostgreSQL to persist evaluation results.

## Tech Stack

| Category | Technologies |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot |
| Database | PostgreSQL |
| ORM | Spring Data JPA, Hibernate |
| AI Integration | Google Gemini API |
| Document Processing | Spring AI PDF Reader |
| Frontend | HTML, CSS, JavaScript |
| Build Tool | Maven |

## How It Works

1. Upload a Request for Resources (RFR) document and candidate resumes in PDF format.
2. Extract text from the uploaded documents.
3. Send the extracted content to Google Gemini for AI-powered analysis.
4. Evaluate skill compatibility, recency, frequency, and project complexity.
5. Calculate weighted evaluation scores using Java business logic.
6. Store the evaluation results in PostgreSQL.
7. Display the evaluation results through the application interface.

## Getting Started

### Prerequisites

- Java JDK 21
- Maven
- PostgreSQL
- Google Gemini API key

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_USERNAME/ResumeAnalyzer.git
cd ResumeAnalyzer
```

**2. Configure PostgreSQL**

Create a PostgreSQL database and configure the database connection in `application.properties`.

**3. Configure environment variables**

Set the following environment variables:

```text
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
GEMINI_API_KEY=your_gemini_api_key
```

Update the corresponding properties in `application.properties` to reference these variables.

**4. Run the application**

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Alternatively:

```bash
mvn spring-boot:run
```

**5. Access the application**

Open your browser and navigate to:

```text
http://localhost:8080
```

## Project Structure

```text
src/
 ├── main/
 │   ├── java/
 │   │   └── com/app/ResumeAnalyzer/
 │   │       ├── Controller/
 │   │       ├── Service/
 │   │       ├── Entity/
 │   │       ├── Repository/
 │   │       └── Config/
 │   └── resources/
 │       ├── static/
 │       ├── templates/
 │       └── application.properties/
 └── test/
```

## Future Enhancements

- Authentication and role-based access control.
- Improved evaluation accuracy and bias testing.
- Comprehensive unit and integration testing.
- Docker-based deployment.
- Enhanced reporting and analytics.

## Note

This project is intended as a resume evaluation and decision-support tool. Its automated scores should not replace human recruitment decisions.

## Author

Developed as a Java and Spring Boot project exploring AI integration, backend development, database management, and concurrent processing.