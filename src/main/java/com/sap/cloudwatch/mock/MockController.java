package com.sap.cloudwatch.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@AllArgsConstructor
@Slf4j
public class MockController {
    private final ObjectMapper objectMapper;

    @RequestMapping("/test")
    public JsonNode test() throws java.lang.Exception {
        JsonNode jsonNode = objectMapper.readTree(testStirng());
        try {
            log.error("##### Exception 받았다");
            Exception.exception();
        } catch (java.lang.Exception e){
            log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
            log.error("Exception 떠버렷죠? ", e);
        }
        return jsonNode;
    }

    @RequestMapping("/test2")
    public String exception() throws java.lang.Exception {
        JsonNode jsonNode = objectMapper.readTree(testStirng());
        var response = new WebClientConfig().webClientBuilder().build()
                .post()
                .uri("http://localhost:8081/test")
                .bodyValue(jsonNode)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    }

    private String toPrettyJson(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (java.lang.Exception e) {
            // JSON 문자열이 아님
        }
        return jsonString;
    }

    private String testStirng() {
        return """
                
                {
                    "user": {
                        "id": 12345,
                        "name": "John Doe",
                        "isVerified": true,
                        "contact": {
                            "email": "john.doe@example.com",
                            "phone": "+1-800-555-0123"
                        },
                        "roles": ["admin", "editor"],
                        "preferences": {
                            "notifications": {
                                "email": true,
                                "sms": false
                            },
                            "theme": "dark",
                            "language": "en-US"
                        }
                    },
                    "meta": {
                        "timestamp": "2024-11-01T08:55:40Z",
                        "requestId": "abc123-xyz456",
                        "version": "1.0.0"
                    },
                    "posts": [
                        {
                            "id": 101,
                            "title": "First Post",
                            "content": "This is the first post content.",
                            "tags": ["introduction", "welcome"],
                            "published": true,
                            "comments": [
                                {
                                    "id": 1001,
                                    "author": "Alice",
                                    "content": "Great post!",
                                    "timestamp": "2024-10-30T14:55:00Z"
                                },
                                {
                                    "id": 1002,
                                    "author": "Bob",
                                    "content": "Thanks for the info.",
                                    "timestamp": "2024-10-31T10:17:00Z"
                                }
                            ]
                        },
                        {
                            "id": 102,
                            "title": "Another Post",
                            "content": "More content here.",
                            "tags": ["update", "news"],
                            "published": false,
                            "comments": []
                        }
                    ]
                }
                """;
    }
}
