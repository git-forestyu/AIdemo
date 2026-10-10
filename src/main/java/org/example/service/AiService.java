package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final ChatClient chatClient;
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    // Constructor injection of the ChatClient that Spring Boot auto-configures
    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Send a message to DeepSeek and get the reply
     * @param prompt the question or instruction entered by the user
     * @param prompt the question or instruction entered by the user
     */
    public String ask(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * A call that carries a system prompt, which constrains the AI's behaviour
     * A very good fit for an automation platform: let the AI play a "test case generator" or a "code analyser
     */
    public String askWithSystem(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt) // set the AI's role
                .user(userPrompt)      // the user's concrete instruction
                .call()
                .content();
    }


    public String generateTestCasesJsonWithAIhallucination(String apiDescription) {
        String systemPrompt = """
        You are a test case generation expert.
        Input: a description of an API endpoint.
        Output: a JSON array.
        Each element in the array contains seven fields:
        - action: description of the test action
        - target: HTTP method and path
        - parameter: query parameters passed in the URL
        - contentType: content type of the request
        - requestBody: request body
        - expected: expected result, you have to add a "or" in the value, for example, name doesn't exist or id doesn't exist, I need to test or 
        - expectedStatus: expected status code
        """;
        return askWithSystem(systemPrompt, apiDescription);
    }

    public String generateTestCasesJson(String apiDescription) {
        String systemPrompt = """
        You are a test case generation expert.
        Input: a description of an API endpoint.
        Output: a strict JSON array with no explanation text.
        Each element in the array contains seven fields:
        - action: description of the test action, must not be empty
        - target: HTTP method and path, neither can be empty. Use the HTTP method I provide — do not change it. If the path contains a path variable like {id}, replace it with a real value, for example, GET /api/users/1, do not leave {id} in the target.
        - parameter: for GET, the path with query parameters appended, must be a string. If there are query parameters, start with a question mark, e.g. ?name=test. If the GET request has no query parameters, set parameter to an empty string "". For POST, leave it empty.
        - contentType: for POST, the content type of the request, e.g. application/json. Take it directly from the endpoint definition I provide, usually from the requestBody.content field. Do not modify it. Set it to empty only if the endpoint definition has no value. For GET, leave it empty.
        - requestBody: for POST, the request body. It can be empty or non-empty. If non-empty, it must be a string and must strictly match the contentType. For example, if contentType is application/json and the body is a JSON object, serialize it into a string, e.g. "{\\"name\\":\\"test\\"}", do not output a JSON object directly. For GET, leave it empty.
        - expected: expected result, text description, must not be empty. This field does not need to include the expected status code.
        - expectedStatus: expected status code, e.g. 400, 200, must not be empty.
        Each test case's expected must be a single deterministic assertion. Do not use vague expressions like "or", "maybe", or "one of". If unsure, write the most conservative assertion based on the endpoint definition.
        """;
        return askWithSystem(systemPrompt, apiDescription);
    }


    public String generateUiScenariosJson(String pageDescription) {
        String systemPrompt = """
    You are a UI test case generation expert.
    Input: a description of a web page.
    Output: a strict JSON object with no explanation text.

    The object contains a "scenarios" array. Each scenario has:
    - name: scenario name, e.g. "Valid login" or "Invalid password"
    - steps: an ordered array of steps. Each step contains:
      - action: "open", "input", "click", or "assert"
      - target: the element id or URL
      - value: the value to input or the expected value

    Generate at least two scenarios:
    1. A normal scenario (valid credentials, expects redirect to /home.html)
    2. An exception scenario (invalid credentials, expects to stay on /login.html)

    Steps must be in the order a user would perform them:
    open the page, input fields, click the button, then assert the result.
    """;
        return askWithSystem(systemPrompt, pageDescription);
    }

    public List<TestCase> parseToTestCases(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(json);
        List<TestCase> cases = new ArrayList<>();

        for (JsonNode node : root) {
            TestCase tc = new TestCase();
            tc.setAction(node.path("action").asText());
            tc.setTarget(node.path("target").asText());
            tc.setParameter(node.path("parameter").asText(""));
            tc.setContentType(node.path("contentType").asText(""));
            tc.setExpected(node.path("expected").asText());
            tc.setExpectedStatus(node.path("expectedStatus").asInt());

            // requestBody Compatible with objects and strings
            JsonNode rb = node.path("requestBody");
            if (rb.isNull() || rb.isMissingNode()) {
                tc.setRequestBody("");
            } else if (rb.isTextual()) {
                tc.setRequestBody(rb.asText());
            } else {
                tc.setRequestBody(mapper.writeValueAsString(rb));
            }

            cases.add(tc);
        }
        return cases;

    }

    public List<ApiEndpoint> parseSwaggerToEndpoint(String swaggerJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(swaggerJson);
        JsonNode paths = root.get("paths");

        List<ApiEndpoint> endpoints = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = paths.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String path = entry.getKey();
            JsonNode pathItem = entry.getValue();

            //debug code: for now only test the matching method
          // if (!path.equals("/ai/run-test"))
            if (path.contains("/ai/"))
                continue;
            if (path.startsWith("/v3/") || path.startsWith("/swagger") || path.startsWith("/actuator"))
                continue;
            if (path.contains("/api/users/reset"))
                continue;
            // one path may carry several methods (GET/POST)
            Iterator<Map.Entry<String, JsonNode>> methods = pathItem.fields();
            while (methods.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methods.next();
                String method = methodEntry.getKey().toUpperCase();  // GET / POST
                JsonNode methodNode = methodEntry.getValue();

                JsonNode schemaNode = methodNode.at("/requestBody/content/application~1json/schema");
                String schemaJson = "";

                //schemaNode.has only supports application/json; with application/x-www-form-urlencoded or multipart/form-data the schema cannot be retrieved
                if (schemaNode.has("$ref")) {
                    String ref = schemaNode.get("$ref").asText();   // "#/components/schemas/User"
                    String schemaName = ref.substring(ref.lastIndexOf("/") + 1);  // "User"
                    JsonNode realSchema = root.at("/components/schemas/" + schemaName);
                    schemaJson = realSchema != null ? realSchema.toString() : "";
                } else {
                    schemaJson = schemaNode.toString();
                }


                ApiEndpoint ep = new ApiEndpoint();
                ep.setPath(path);
                ep.setMethod(method);
                ep.setRawJson(methodNode.toString());  // keep this endpoint's raw JSON around to feed to the AI
                ep.setSchemaJson(schemaJson);
                endpoints.add(ep);
            }
                    }
        return endpoints;
    }

    public List<UiTestCase> parseToUiTestCases(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        List<UiTestCase> cases = new ArrayList<>();

        for (JsonNode node : root) {
            UiTestCase tc = new UiTestCase();
            tc.setAction(node.path("action").asText());
            tc.setTarget(node.path("target").asText());
            tc.setValue(node.path("value").asText(""));
            tc.setExpected(node.path("expected").asText(""));
            tc.setExpectedStatus(node.path("expectedStatus").asInt(200));
            cases.add(tc);
        }
        return cases;
    }
    public List<UiScenario> parseToUiScenarios(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode scenariosNode = root.path("scenarios");

        List<UiScenario> scenarios = new ArrayList<>();
        for (JsonNode scenarioNode : scenariosNode) {
            UiScenario scenario = new UiScenario();
            scenario.setName(scenarioNode.path("name").asText());

            List<UiStep> steps = new ArrayList<>();
            for (JsonNode stepNode : scenarioNode.path("steps")) {
                UiStep step = new UiStep();
                step.setAction(stepNode.path("action").asText());
                step.setTarget(stepNode.path("target").asText());
                step.setValue(stepNode.path("value").asText(""));
                steps.add(step);
            }
            scenario.setSteps(steps);
            scenarios.add(scenario);
        }
        return scenarios;
    }

}
