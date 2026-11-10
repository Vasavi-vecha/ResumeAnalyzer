package com.app.ResumeAnalyzer.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonLogger {

    public static void logJson(String rawResponse) {
        try {
            int start = rawResponse.indexOf('{');
            int end = rawResponse.lastIndexOf('}') + 1;

            if (start == -1 || end == -1 || start >= end) {
                log.warn("No valid JSON found in response.");
                return;
            }

            String jsonText = rawResponse.substring(start, end);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonText);

            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            log.info("Extracted JSON:\n{}", prettyJson);


        } catch (Exception e) {
            log.error("Failed to parse JSON", e);
        }
    }
}
