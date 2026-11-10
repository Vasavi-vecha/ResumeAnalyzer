package com.app.ResumeAnalyzer.Controller;

import com.app.ResumeAnalyzer.Entity.Evaluation;
import com.app.ResumeAnalyzer.Service.EvaluationService;
import com.app.ResumeAnalyzer.Service.GeminiService;
import com.app.ResumeAnalyzer.Service.JsonLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pdf")
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
public class PdfUploadController {

    private final GeminiService geminiService;
    private final EvaluationService evaluationService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluateResumeAgainstRfrPdf(
            @RequestParam("rfr") MultipartFile rfrFile,
            @RequestParam("resume") MultipartFile resumeFile) {

        try {
            // Extract RFR ID and Candidate ID from filenames
            String rfrId = rfrFile.getOriginalFilename();
            String candidateId = resumeFile.getOriginalFilename();

            // Check cache first - return cached result if exists
            Evaluation cachedEvaluation = evaluationService.getCachedEvaluation(rfrId, candidateId);
            if (cachedEvaluation != null) {
                log.info("Returning cached evaluation for RFR: {}, Candidate: {}", rfrId, candidateId);
                // Add custom header to indicate this is a cached response
                return ResponseEntity.ok()
                        .header("X-Evaluation-Cached", "true")
                        .header("X-RFR-Name", rfrId)
                        .header("X-Resume-Name", candidateId)
                        .body(convertEvaluationToJson(cachedEvaluation));
            }

            // Not in cache - proceed with evaluation
            log.info("No cached evaluation found. Evaluating RFR: {}, Candidate: {}", rfrId, candidateId);

            // Extract RFR text
            InputStreamResource rfrResource = new InputStreamResource(rfrFile.getInputStream());
            PagePdfDocumentReader rfrReader = new PagePdfDocumentReader(rfrResource);
            StringBuilder rfrText = new StringBuilder();
            for (Document page : rfrReader.read()) {
                rfrText.append(page.getText()).append("\n");
            }

            // Extract Resume text
            InputStreamResource resumeResource = new InputStreamResource(resumeFile.getInputStream());
            PagePdfDocumentReader resumeReader = new PagePdfDocumentReader(resumeResource);
            StringBuilder resumeText = new StringBuilder();
            for (Document page : resumeReader.read()) {
                resumeText.append(page.getText()).append("\n");
            }

            // Validate resume format - TEMPORARILY DISABLED FOR TESTING
            // String formatValidation = validateResumeFormat(resumeText.toString());
            // if (!formatValidation.isEmpty()) {
            //     return ResponseEntity.status(400).body(formatValidation);
            // }

            // Build prompt - AI only evaluates skills, we calculate scores
            String prompt = """
                You are an expert evaluator trained to analyze staffing requests and assess candidate resumes.

                Task:
                1. Analyze the RFR (Request for Resources) document below and extract all required and preferred skills, along with experience ranges and preferences.
                2. Evaluate the resume against those skills.
                3. For each skill found in the RFR, rate the resume on:
                   - match (0.0–5.0): How well the candidate's experience aligns with the skill (0=no match, 5=perfect match with required years of experience)
                   - recency (0.0–5.0): How recently the skill was used (5=currently using, 0=not used in 5+ years)
                   - frequency (0.0–5.0): How often the skill appears in the resume (5=extensively mentioned, 0=not mentioned)

                4. Evaluate project complexity (0.0-100.0) - PRIORITIZE COMPLEXITY OVER EXPERIENCE:
                   - Award higher scores for candidates who have worked on complex projects even with less experience
                   - Key complexity indicators (weighted heavily):
                     * System architecture and design (microservices, distributed systems)
                     * Scalability and performance optimization (handling millions of users, high throughput)
                     * Enterprise-level systems (multi-tenant, global scale)
                     * Complex integrations (third-party APIs, legacy system migrations)
                     * Leadership in technical initiatives (driving architecture decisions, mentoring)
                     * Problem-solving in complex scenarios (debugging production issues, optimization)
                   - Years of experience should be secondary - a 3-year candidate with complex projects should score higher than a 7-year candidate with simple projects
                   - Scoring guide:
                     * 80-100: Highly complex projects (distributed systems, enterprise architecture, performance at scale)
                     * 60-79: Moderately complex projects (multi-component systems, significant integrations)
                     * 40-59: Standard projects (CRUD apps, basic integrations)
                     * 0-39: Simple/basic projects
                   - Return as: "project_complexity_score"

                5. Extract total years of experience:
                   - Count the total years mentioned in the resume
                   - Return as: "years_of_experience"

                Return ONLY a valid JSON object in this exact format — no explanation, no formatting, no commentary:
                {
                  "skills": {
                    "Skill Name": { "match": x.x, "recency": x.x, "frequency": x.x },
                    ...
                  },
                  "project_complexity_score": x.x,
                  "years_of_experience": x.x
                }

                RFR:
                %s

                Resume:
                %s
                """.formatted(rfrText, resumeText);

            Content userContent = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(prompt)))
                    .build();

            String result = geminiService.askGemini(List.of(userContent));
            JsonLogger.logJson(result);

            // Save evaluation to database
            evaluationService.saveEvaluation(result, rfrId, candidateId);

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to process PDFs: " + e.getMessage());
        }
    }

    @PostMapping("/evaluate-folder")
    public ResponseEntity<?> evaluateResumesInFolder(
            @RequestParam("rfrPath") String rfrPath,
            @RequestParam("resumesFolderPath") String resumesFolderPath) {

        log.info("=== MULTIPLE RESUMES EVALUATION STARTED ===");
        log.info("RFR Path: {}", rfrPath);
        log.info("Resumes Folder Path: {}", resumesFolderPath);

        try {
            // Validate and extract RFR text
            File rfrFile = new File(rfrPath);
            log.info("Checking RFR file existence...");

            if (!rfrFile.exists()) {
                String error = "❌ RFR file does not exist at path: " + rfrPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            if (!rfrFile.isFile()) {
                String error = "❌ Path is not a file (might be a directory): " + rfrPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            if (!rfrFile.getName().toLowerCase().endsWith(".pdf")) {
                String error = "❌ RFR file must be a PDF. Found: " + rfrFile.getName();
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ RFR file found: {}", rfrFile.getName());

            InputStreamResource rfrResource = new InputStreamResource(new FileInputStream(rfrFile));
            PagePdfDocumentReader rfrReader = new PagePdfDocumentReader(rfrResource);
            StringBuilder rfrText = new StringBuilder();
            for (Document page : rfrReader.read()) {
                rfrText.append(page.getText()).append("\n");
            }

            log.info("✓ RFR text extracted successfully ({} characters)", rfrText.length());

            // Validate resumes folder
            File resumesFolder = new File(resumesFolderPath);
            log.info("Checking resumes folder existence...");

            if (!resumesFolder.exists()) {
                String error = "❌ Resumes folder does not exist at path: " + resumesFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            if (!resumesFolder.isDirectory()) {
                String error = "❌ Path is not a directory (might be a file): " + resumesFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Resumes folder found");

            // Get all resume files from folder
            File[] resumeFiles = resumesFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".pdf"));

            if (resumeFiles == null || resumeFiles.length == 0) {
                String error = "❌ No PDF files found in resumes folder: " + resumesFolderPath +
                    "\n\nPlease ensure:\n1. The folder contains PDF files\n2. File extensions are .pdf (lowercase)\n3. You have read permissions";
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Found {} resume PDF files", resumeFiles.length);
            for (File resume : resumeFiles) {
                log.info("  - {}", resume.getName());
            }

            String rfrId = rfrFile.getName();
            String rfrTextStr = rfrText.toString();

            // Process resumes in parallel
            log.info("Starting parallel processing of {} resumes...", resumeFiles.length);
            List<CompletableFuture<Map<String, Object>>> futures = Arrays.stream(resumeFiles)
                .map(resumeFile -> CompletableFuture.supplyAsync(() ->
                    processResume(resumeFile, rfrId, rfrTextStr), executorService))
                .collect(Collectors.toList());

            // Wait for all tasks to complete
            List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

            log.info("✓ All {} resumes processed successfully", results.size());

            // Count successes and failures
            long successCount = results.stream().filter(r -> "success".equals(r.get("status")) || "success_cached".equals(r.get("status"))).count();
            long failedCount = results.stream().filter(r -> "failed".equals(r.get("status"))).count();

            log.info("Success: {}, Failed: {}", successCount, failedCount);

            Map<String, Object> response = new HashMap<>();
            response.put("totalResumes", results.size());
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            String error = "❌ Failed to process PDFs: " + e.getMessage() +
                "\n\nPossible causes:\n1. File is corrupted or not a valid PDF\n2. File is locked by another application\n3. Insufficient permissions";
            log.error(error, e);
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            String error = "❌ Unexpected error: " + e.getMessage();
            log.error(error, e);
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/evaluate-multiple-rfrs")
    public ResponseEntity<?> evaluateResumeAgainstMultipleRfrs(
            @RequestParam("rfrsFolderPath") String rfrsFolderPath,
            @RequestParam("resume") MultipartFile resumeFile) {

        log.info("=== MULTIPLE RFRs EVALUATION STARTED ===");
        log.info("RFRs Folder Path: {}", rfrsFolderPath);
        log.info("Resume File: {}", resumeFile.getOriginalFilename());

        try {
            String candidateId = resumeFile.getOriginalFilename();

            // Validate RFRs folder
            File rfrsFolder = new File(rfrsFolderPath);
            log.info("Checking RFRs folder existence...");

            if (!rfrsFolder.exists()) {
                String error = "❌ RFRs folder does not exist at path: " + rfrsFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            if (!rfrsFolder.isDirectory()) {
                String error = "❌ Path is not a directory (might be a file): " + rfrsFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ RFRs folder found");

            // Get all RFR files from folder
            File[] rfrFiles = rfrsFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".pdf"));

            if (rfrFiles == null || rfrFiles.length == 0) {
                String error = "❌ No PDF files found in RFRs folder: " + rfrsFolderPath +
                    "\n\nPlease ensure:\n1. The folder contains PDF files\n2. File extensions are .pdf (lowercase)\n3. You have read permissions";
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Found {} RFR PDF files", rfrFiles.length);
            for (File rfr : rfrFiles) {
                log.info("  - {}", rfr.getName());
            }

            // Validate resume file
            if (resumeFile.isEmpty()) {
                String error = "❌ Resume file is empty";
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Resume file uploaded: {} ({} bytes)", candidateId, resumeFile.getSize());

            // Extract Resume text once
            InputStreamResource resumeResource = new InputStreamResource(resumeFile.getInputStream());
            PagePdfDocumentReader resumeReader = new PagePdfDocumentReader(resumeResource);
            StringBuilder resumeText = new StringBuilder();
            for (Document page : resumeReader.read()) {
                resumeText.append(page.getText()).append("\n");
            }

            log.info("✓ Resume text extracted successfully ({} characters)", resumeText.length());

            String resumeTextStr = resumeText.toString();

            // Process each RFR in parallel
            log.info("Starting parallel processing against {} RFRs...", rfrFiles.length);
            List<CompletableFuture<Map<String, Object>>> futures = Arrays.stream(rfrFiles)
                .map(rfrFile -> CompletableFuture.supplyAsync(() ->
                    processRfr(rfrFile, candidateId, resumeTextStr), executorService))
                .collect(Collectors.toList());

            // Wait for all tasks to complete
            List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

            log.info("✓ All {} RFRs processed successfully", results.size());

            // Count successes and failures
            long successCount = results.stream().filter(r -> "success".equals(r.get("status")) || "success_cached".equals(r.get("status"))).count();
            long failedCount = results.stream().filter(r -> "failed".equals(r.get("status"))).count();

            log.info("Success: {}, Failed: {}", successCount, failedCount);

            Map<String, Object> response = new HashMap<>();
            response.put("candidateId", candidateId);
            response.put("totalRFRs", results.size());
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            String error = "❌ Failed to process PDFs: " + e.getMessage() +
                "\n\nPossible causes:\n1. File is corrupted or not a valid PDF\n2. File is locked by another application\n3. Insufficient permissions";
            log.error(error, e);
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            String error = "❌ Unexpected error: " + e.getMessage();
            log.error(error, e);
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/evaluate-bulk")
    public ResponseEntity<?> evaluateBulk(
            @RequestParam("rfrsFolderPath") String rfrsFolderPath,
            @RequestParam("resumesFolderPath") String resumesFolderPath) {

        log.info("=== BULK EVALUATION STARTED ===");
        log.info("RFRs Folder Path: {}", rfrsFolderPath);
        log.info("Resumes Folder Path: {}", resumesFolderPath);

        try {
            // Validate RFRs folder
            File rfrsFolder = new File(rfrsFolderPath);
            log.info("Checking RFRs folder existence...");

            if (!rfrsFolder.exists()) {
                String error = "❌ RFRs folder does not exist at path: " + rfrsFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            if (!rfrsFolder.isDirectory()) {
                String error = "❌ Path is not a directory (might be a file): " + rfrsFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ RFRs folder found");

            // Get all RFR files
            File[] rfrFiles = rfrsFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".pdf"));

            if (rfrFiles == null || rfrFiles.length == 0) {
                String error = "❌ No PDF files found in RFRs folder: " + rfrsFolderPath +
                    "\n\nPlease ensure:\n1. The folder contains PDF files\n2. File extensions are .pdf (lowercase)\n3. You have read permissions";
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Found {} RFR PDF files", rfrFiles.length);
            for (File rfr : rfrFiles) {
                log.info("  - {}", rfr.getName());
            }

            // Validate resumes folder
            File resumesFolder = new File(resumesFolderPath);
            log.info("Checking resumes folder existence...");

            if (!resumesFolder.exists()) {
                String error = "❌ Resumes folder does not exist at path: " + resumesFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            if (!resumesFolder.isDirectory()) {
                String error = "❌ Path is not a directory (might be a file): " + resumesFolderPath;
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Resumes folder found");

            // Get all resume files
            File[] resumeFiles = resumesFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".pdf"));

            if (resumeFiles == null || resumeFiles.length == 0) {
                String error = "❌ No PDF files found in resumes folder: " + resumesFolderPath +
                    "\n\nPlease ensure:\n1. The folder contains PDF files\n2. File extensions are .pdf (lowercase)\n3. You have read permissions";
                log.error(error);
                return ResponseEntity.badRequest().body(error);
            }

            log.info("✓ Found {} resume PDF files", resumeFiles.length);
            for (File resume : resumeFiles) {
                log.info("  - {}", resume.getName());
            }

            List<Map<String, Object>> allResults = new ArrayList<>();
            int totalCombinations = rfrFiles.length * resumeFiles.length;
            int processedCount = 0;

            log.info("Starting bulk evaluation: {} RFRs × {} Resumes = {} combinations",
                rfrFiles.length, resumeFiles.length, totalCombinations);

            // Process each RFR
            for (File rfrFile : rfrFiles) {
                String rfrId = rfrFile.getName();

                // Extract RFR text
                InputStreamResource rfrResource = new InputStreamResource(new FileInputStream(rfrFile));
                PagePdfDocumentReader rfrReader = new PagePdfDocumentReader(rfrResource);
                StringBuilder rfrText = new StringBuilder();
                for (Document page : rfrReader.read()) {
                    rfrText.append(page.getText()).append("\n");
                }

                String rfrTextStr = rfrText.toString();

                // Process resumes for this RFR in parallel
                List<CompletableFuture<Map<String, Object>>> futures = Arrays.stream(resumeFiles)
                    .map(resumeFile -> CompletableFuture.supplyAsync(() ->
                        processResume(resumeFile, rfrId, rfrTextStr), executorService))
                    .collect(Collectors.toList());

                List<Map<String, Object>> rfrResults = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

                allResults.addAll(rfrResults);
                processedCount += rfrResults.size();

                log.info("Processed {}/{} combinations", processedCount, totalCombinations);
            }

            log.info("✓ Bulk evaluation completed successfully");

            // Count successes and failures
            long successCount = allResults.stream().filter(r -> "success".equals(r.get("status")) || "success_cached".equals(r.get("status"))).count();
            long failedCount = allResults.stream().filter(r -> "failed".equals(r.get("status"))).count();

            log.info("Success: {}, Failed: {}", successCount, failedCount);

            Map<String, Object> response = new HashMap<>();
            response.put("totalRFRs", rfrFiles.length);
            response.put("totalResumes", resumeFiles.length);
            response.put("totalCombinations", totalCombinations);
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("results", allResults);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            String error = "❌ Failed to process PDFs: " + e.getMessage() +
                "\n\nPossible causes:\n1. File is corrupted or not a valid PDF\n2. File is locked by another application\n3. Insufficient permissions";
            log.error(error, e);
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            String error = "❌ Unexpected error: " + e.getMessage();
            log.error(error, e);
            return ResponseEntity.status(500).body(error);
        }
    }

    private Map<String, Object> processResume(File resumeFile, String rfrId, String rfrText) {
        Map<String, Object> result = new HashMap<>();
        String candidateId = resumeFile.getName();

        try {
            // Check cache first - return cached result if exists
            Evaluation cachedEvaluation = evaluationService.getCachedEvaluation(rfrId, candidateId);
            if (cachedEvaluation != null) {
                log.info("Returning cached evaluation for RFR: {}, Candidate: {}", rfrId, candidateId);
                result.put("candidateId", candidateId);
                result.put("status", "success_cached");
                result.put("evaluation", convertEvaluationToJson(cachedEvaluation));
                return result;
            }

            // Not in cache - proceed with evaluation
            log.info("No cached evaluation found. Evaluating RFR: {}, Candidate: {}", rfrId, candidateId);

            // Extract Resume text
            InputStreamResource resumeResource = new InputStreamResource(new FileInputStream(resumeFile));
            PagePdfDocumentReader resumeReader = new PagePdfDocumentReader(resumeResource);
            StringBuilder resumeText = new StringBuilder();
            for (Document page : resumeReader.read()) {
                resumeText.append(page.getText()).append("\n");
            }

            // Validate resume format - TEMPORARILY DISABLED FOR TESTING
            // String formatValidation = validateResumeFormat(resumeText.toString());
            // if (!formatValidation.isEmpty()) {
            //     result.put("candidateId", candidateId);
            //     result.put("status", "failed");
            //     result.put("error", formatValidation);
            //     return result;
            // }

            // Build prompt - AI only evaluates skills, we calculate scores
            String prompt = """
                You are an expert evaluator trained to analyze staffing requests and assess candidate resumes.

                Task:
                1. Analyze the RFR (Request for Resources) document below and extract all required and preferred skills, along with experience ranges and preferences.
                2. Evaluate the resume against those skills.
                3. For each skill found in the RFR, rate the resume on:
                   - match (0.0–5.0): How well the candidate's experience aligns with the skill (0=no match, 5=perfect match with required years of experience)
                   - recency (0.0–5.0): How recently the skill was used (5=currently using, 0=not used in 5+ years)
                   - frequency (0.0–5.0): How often the skill appears in the resume (5=extensively mentioned, 0=not mentioned)

                4. Evaluate project complexity (0.0-100.0) - PRIORITIZE COMPLEXITY OVER EXPERIENCE:
                   - Award higher scores for candidates who have worked on complex projects even with less experience
                   - Key complexity indicators (weighted heavily):
                     * System architecture and design (microservices, distributed systems)
                     * Scalability and performance optimization (handling millions of users, high throughput)
                     * Enterprise-level systems (multi-tenant, global scale)
                     * Complex integrations (third-party APIs, legacy system migrations)
                     * Leadership in technical initiatives (driving architecture decisions, mentoring)
                     * Problem-solving in complex scenarios (debugging production issues, optimization)
                   - Years of experience should be secondary - a 3-year candidate with complex projects should score higher than a 7-year candidate with simple projects
                   - Scoring guide:
                     * 80-100: Highly complex projects (distributed systems, enterprise architecture, performance at scale)
                     * 60-79: Moderately complex projects (multi-component systems, significant integrations)
                     * 40-59: Standard projects (CRUD apps, basic integrations)
                     * 0-39: Simple/basic projects
                   - Return as: "project_complexity_score"

                5. Extract total years of experience:
                   - Count the total years mentioned in the resume
                   - Return as: "years_of_experience"

                Return ONLY a valid JSON object in this exact format — no explanation, no formatting, no commentary:
                {
                  "skills": {
                    "Skill Name": { "match": x.x, "recency": x.x, "frequency": x.x },
                    ...
                  },
                  "project_complexity_score": x.x,
                  "years_of_experience": x.x
                }
                RFR:
                %s

                Resume:
                %s
                """.formatted(rfrText, resumeText);

            Content userContent = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(prompt)))
                    .build();

            String evaluation = geminiService.askGemini(List.of(userContent));
            JsonLogger.logJson(evaluation);

            // Save evaluation to database
            evaluationService.saveEvaluation(evaluation, rfrId, candidateId);

            result.put("candidateId", candidateId);
            result.put("status", "success");
            result.put("evaluation", evaluation);

        } catch (Exception e) {
            log.error("Failed to process resume: " + candidateId, e);
            result.put("candidateId", candidateId);
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> processRfr(File rfrFile, String candidateId, String resumeText) {
        Map<String, Object> result = new HashMap<>();
        String rfrId = rfrFile.getName();

        try {
            // Check cache first - return cached result if exists
            Evaluation cachedEvaluation = evaluationService.getCachedEvaluation(rfrId, candidateId);
            if (cachedEvaluation != null) {
                log.info("Returning cached evaluation for RFR: {}, Candidate: {}", rfrId, candidateId);
                result.put("rfrId", rfrId);
                result.put("status", "success_cached");
                result.put("evaluation", convertEvaluationToJson(cachedEvaluation));
                return result;
            }

            // Not in cache - proceed with evaluation
            log.info("No cached evaluation found. Evaluating RFR: {}, Candidate: {}", rfrId, candidateId);

            // Extract RFR text
            InputStreamResource rfrResource = new InputStreamResource(new FileInputStream(rfrFile));
            PagePdfDocumentReader rfrReader = new PagePdfDocumentReader(rfrResource);
            StringBuilder rfrText = new StringBuilder();
            for (Document page : rfrReader.read()) {
                rfrText.append(page.getText()).append("\n");
            }

            // Build prompt - AI only evaluates skills, we calculate scores
            String prompt = """
                You are an expert evaluator trained to analyze staffing requests and assess candidate resumes.

                Task:
                1. Analyze the RFR (Request for Resources) document below and extract all required and preferred skills, along with experience ranges and preferences.
                2. Evaluate the resume against those skills.
                3. For each skill found in the RFR, rate the resume on:
                   - match (0.0–5.0): How well the candidate's experience aligns with the skill (0=no match, 5=perfect match with required years of experience)
                   - recency (0.0–5.0): How recently the skill was used (5=currently using, 0=not used in 5+ years)
                   - frequency (0.0–5.0): How often the skill appears in the resume (5=extensively mentioned, 0=not mentioned)

                4. Evaluate project complexity (0.0-100.0) - PRIORITIZE COMPLEXITY OVER EXPERIENCE:
                   - Award higher scores for candidates who have worked on complex projects even with less experience
                   - Key complexity indicators (weighted heavily):
                     * System architecture and design (microservices, distributed systems)
                     * Scalability and performance optimization (handling millions of users, high throughput)
                     * Enterprise-level systems (multi-tenant, global scale)
                     * Complex integrations (third-party APIs, legacy system migrations)
                     * Leadership in technical initiatives (driving architecture decisions, mentoring)
                     * Problem-solving in complex scenarios (debugging production issues, optimization)
                   - Years of experience should be secondary - a 3-year candidate with complex projects should score higher than a 7-year candidate with simple projects
                   - Scoring guide:
                     * 80-100: Highly complex projects (distributed systems, enterprise architecture, performance at scale)
                     * 60-79: Moderately complex projects (multi-component systems, significant integrations)
                     * 40-59: Standard projects (CRUD apps, basic integrations)
                     * 0-39: Simple/basic projects
                   - Return as: "project_complexity_score"

                5. Extract total years of experience:
                   - Count the total years mentioned in the resume
                   - Return as: "years_of_experience"

                Return ONLY a valid JSON object in this exact format — no explanation, no formatting, no commentary:
                {
                  "skills": {
                    "Skill Name": { "match": x.x, "recency": x.x, "frequency": x.x },
                    ...
                  },
                  "project_complexity_score": x.x,
                  "years_of_experience": x.x
                }
                RFR:
                %s

                Resume:
                %s
                """.formatted(rfrText, resumeText);

            Content userContent = Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(prompt)))
                    .build();

            String evaluation = geminiService.askGemini(List.of(userContent));
            JsonLogger.logJson(evaluation);

            // Save evaluation to database
            evaluationService.saveEvaluation(evaluation, rfrId, candidateId);

            result.put("rfrId", rfrId);
            result.put("status", "success");
            result.put("evaluation", evaluation);

        } catch (Exception e) {
            log.error("Failed to process RFR: " + rfrId, e);
            result.put("rfrId", rfrId);
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Validates resume format for Cognizant standards
     * Expected sections: Contact Info, Professional Summary, Skills, Experience, Education
     */
    private String validateResumeFormat(String resumeText) {
        String lowerText = resumeText.toLowerCase();
        StringBuilder errors = new StringBuilder();

        // Check for required sections in Cognizant format
        if (!lowerText.contains("professional summary") && !lowerText.contains("summary") && !lowerText.contains("objective")) {
            errors.append("❌ Missing 'Professional Summary' section. ");
        }

        if (!lowerText.contains("skills") && !lowerText.contains("technical skills") && !lowerText.contains("core competencies")) {
            errors.append("❌ Missing 'Skills' section. ");
        }

        if (!lowerText.contains("experience") && !lowerText.contains("work experience") && !lowerText.contains("professional experience")) {
            errors.append("❌ Missing 'Experience' section. ");
        }

        if (!lowerText.contains("education") && !lowerText.contains("academic") && !lowerText.contains("qualification")) {
            errors.append("❌ Missing 'Education' section. ");
        }

        if (errors.length() > 0) {
            return "⚠️  RESUME FORMAT VALIDATION FAILED\n\n" +
                   errors.toString() + "\n\n" +
                   "📋 Please format your resume according to Cognizant standards with the following sections:\n" +
                   "1. Contact Information\n" +
                   "2. Professional Summary\n" +
                   "3. Skills/Technical Skills\n" +
                   "4. Work Experience\n" +
                   "5. Education\n\n" +
                   "Ensure all sections are clearly labeled and properly formatted before resubmitting.";
        }

        return "";
    }

    /**
     * Convert Evaluation entity to JSON string matching AI response format
     * This ensures cached results have the same format as fresh AI evaluations
     */
    private String convertEvaluationToJson(Evaluation evaluation) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();

            // Add skills JSON
            if (evaluation.getSkillsJson() != null) {
                rootNode.set("skills", evaluation.getSkillsJson());
            } else {
                rootNode.set("skills", mapper.createObjectNode());
            }

            // Add complexity score
            if (evaluation.getComplexityScore() != null) {
                rootNode.put("project_complexity_score", evaluation.getComplexityScore());
            } else {
                rootNode.put("project_complexity_score", 0.0);
            }

            // Note: years_of_experience is not stored in DB, so we omit it for cached results
            // This is acceptable as it's not used in score calculation

            return mapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("Failed to convert evaluation to JSON", e);
            return "{}";
        }
    }
}
