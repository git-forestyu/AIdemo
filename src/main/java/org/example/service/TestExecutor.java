package org.example.service;

import org.example.model.PreSetupAction;
import org.example.model.TestCase;
import org.example.model.TestResult;
import org.example.model.PostVerification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class TestExecutor {

    private final RestClient restClient = RestClient.create();
    private static final Logger log = LoggerFactory.getLogger(TestExecutor.class);

    public TestResult executeAndPostVerification(TestCase tc) {
        int[] statusCode = new int[1];
        String[] body = new String[1];


        try {
            ///Execute pre-setup if necessary
            PreSetupAction preSetup = tc.getSetUp();
            if (preSetup == null) {
                preSetup = buildPreSetup(tc);
            }

            if (preSetup != null) {
                int[] setupStatus = new int[1];
                String[] setupBody = new String[1];

                sendRequest(preSetup.getTarget(), "", preSetup.getContentType(),
                        preSetup.getRequestBody(), setupStatus, setupBody);

                if (setupStatus[0] != preSetup.getExpectedStatus()) {
                    return new TestResult(tc, 0, false,
                            "Pre-setup failed: " + setupBody[0]);
                }
            }


            sendRequest(tc.getTarget(), tc.getParameter(),
                    tc.getContentType(), tc.getRequestBody(), statusCode, body);

            // Simple check: the status is correct, the response is non-empty and not null, the contains no error keywords has not implemented
            boolean passed =  (statusCode[0]==tc.getExpectedStatus()&&body[0] != null && !body[0].isEmpty()) ;


            //Judge the response body: does it meet the requirement, e.g. does it contain key error content
            //If the test case is a create or a delete, a follow-up query must also run afterwards to confirm it really was added or removed

            PostVerification postVerification = tc.getPostVerification();
            if (postVerification == null && statusCode[0]  == 200) {
                postVerification = buildPostVerficiation(tc);   // Java infers the type automatically
            }

            if (postVerification != null) {
                int[] verifyStatus = new int[1];
                String[] verifyBody = new String[1];

                sendRequest(postVerification.getTarget(), "", "", "",
                        verifyStatus, verifyBody);

                //string comparison: use equals
                boolean verifyPassed = (verifyStatus[0] == postVerification.getExpectedStatus())
                        && verifyBody[0].contains(postVerification.getResponse());

                if (verifyPassed) {
                    body[0] = body[0] + "\n[Post-verification passed] " + postVerification.getTarget()
                            + " returned status " + verifyStatus[0] + " and response: " + verifyBody[0];
                } else {
                    passed = false;
                    body[0] = body[0] + "\n[Post-verification failed] Expected " + postVerification.getExpectedStatus()
                            + ", actual " + verifyStatus[0]
                            + ", response: " + verifyBody[0];
                }
            }

            return new TestResult(tc, statusCode[0], passed, body[0]);

        } catch (Exception e) {
            return new TestResult(tc, statusCode[0],false, "Execute exception: " + e.getMessage());
        }

    }

    //Determine whether this is AI-hallucination data
    public boolean validateAIhallucination(TestCase tc) {
        if (tc.getTarget() == null || tc.getTarget().isBlank()) {
            return false;
        }

        String target = tc.getTarget().toUpperCase();
        boolean hasValidMethod = target.startsWith("GET ")
                || target.startsWith("POST ")
                || target.startsWith("PUT ")
                || target.startsWith("DELETE ")
                || target.startsWith("PATCH ");
        // Check 1: target must not be blank and must contain an HTTP method
        String expected = tc.getExpected().toLowerCase();
        boolean hasVagueWording = expected.matches(".*\\bor\\b.*")
                || expected.matches(".*\\bmaybe\\b.*");

        if (tc.getTarget() == null || tc.getTarget().isBlank() || !tc.getTarget().contains("/")) {
            return false;
        } else if(!hasValidMethod){
            log.warn("AI hallucination: invalid method. target={}", tc.getTarget());
            return false;
        }
        // Check 2: expected must not contain vague wording
        else if (hasVagueWording) {
            log.warn("AI hallucination: invalid expected. expected={}", tc.getExpected());
            return false;
        } else {
            return true;
        }
    }

    private PreSetupAction buildPreSetup(TestCase tc) {
        String target = tc.getTarget();
        if (target == null) return null;

        // The data should exist before delete/put/post
        if (!target.startsWith("DELETE ")
                && !target.startsWith("PUT ")
                && !target.startsWith("GET ")) {
            return null;
        }

        // get /api/users/ from /api/users/1
        String path = target.split(" ")[1];

        //If the path does not end with a number or {id}, it is not an "operate-by-id" request, so no setup is needed.
        if (!path.matches(".*/\\d+$") && !path.matches(".*/\\{id\\}$")) {
            return null;
        }
        String basePath = path.replaceAll("/\\d+$", "").replaceAll("/\\{id\\}$", "");

        PreSetupAction setup = new PreSetupAction();
        setup.setTarget("POST " + basePath);
        setup.setContentType("application/json");
        setup.setRequestBody("{\"name\":\"setup-user\",\"age\":20}");
        setup.setExpectedStatus(200);
        return setup;
    }

    private PostVerification buildPostVerficiation(TestCase tc) {
        String target = tc.getTarget();

        //  only run post-verification for DELETE requests
        if (target == null || !target.startsWith("DELETE ")) {
            return null;
        }

        // Replace DELETE with GET, keeping the path unchanged
        String getTarget = "GET " + target.substring("DELETE ".length());

        PostVerification pv = new PostVerification();
        //after the delete, use one GET request to check the deletion
        pv.setTarget(getTarget);
        pv.setExpectedStatus(404);   // after a successful delete the follow-up query should return 404 with an empty response body
        pv.setResponse("The user doesn't exist");
        return pv;
    }

    private void sendRequest(String target, String parameter, String contentType,
                             String requestBody, int[] statusCode, String[] body) {
        String[] parts = target.split(" ", 2);
        String method = parts[0].trim().toUpperCase();
        String path = parts[1].trim();
        String url = "http://localhost:8080" + path + (parameter != null ? parameter : "");

        var spec = restClient.method(HttpMethod.valueOf(method)).uri(url);

        if (contentType != null && !contentType.isEmpty()) {
            spec = spec.contentType(MediaType.valueOf(contentType));
        }
        if (requestBody != null && !requestBody.isEmpty()) {
            spec = spec.body(requestBody);
        }

        spec.exchange((req, res) -> {
            statusCode[0] = res.getStatusCode().value();
            body[0] = new String(res.getBody().readAllBytes());
            return res;
        });
    }
}


