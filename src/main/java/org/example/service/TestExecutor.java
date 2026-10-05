package org.example.service;

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

    public TestResult executeAndVerifyAfter(TestCase tc) {
        int[] statusCode = new int[1];
        String[] body = new String[1];


        try {
            sendRequest(tc.getTarget(), tc.getParameter(),
                    tc.getContentType(), tc.getRequestBody(), statusCode, body);

            // 简单判断：响应非空且不包含错误关键词
            boolean passed =  (statusCode[0]==tc.getExpectedStatus()) ;


            //response响应内容的判断，是否符合需求，如包含关键报错内容
            //如果test case是新增或者删除，则执行后还要加一个查询验证，是否真正增加了

            VerifyAfter verifyAfter = tc.getVerifyAfter();
            if (verifyAfter == null && statusCode[0]  == 200) {
                verifyAfter = buildVerifyAfter(tc);   // Java 自动推导
            }

            if (verifyAfter != null) {
                int[] verifyStatus = new int[1];
                String[] verifyBody = new String[1];

                sendRequest(verifyAfter.getTarget(), "", "", "",
                        verifyStatus, verifyBody);

                //字符串比较，用equal
                boolean verifyPassed = (verifyStatus[0] == verifyAfter.getExpectedStatus())
                        && verifyBody[0].contains(verifyAfter.getResponse());

                if (verifyPassed) {
                    body[0] = body[0] + "\n[后置验证通过] " + verifyAfter.getTarget()
                            + " 返回status " + verifyStatus[0] + " 和response:" + verifyBody[0]; //因删除后再查询为空，此处先写死返回空
                } else {
                    passed = false;
                    body[0] = body[0] + "\n[后置验证失败] 期望 " + verifyAfter.getExpectedStatus()
                            + "，实际 " + verifyStatus[0]
                            + "，响应: " + verifyBody[0];
                }
            }

            return new TestResult(tc, statusCode[0], passed, body[0]);

        } catch (Exception e) {
            return new TestResult(tc, statusCode[0],false, "执行异常: " + e.getMessage());
        }

    }

    //判断是否生成AI hallucination数据
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
        // 校验 1：target 不能为空，且必须包含 HTTP 方法
        if (tc.getTarget() == null || tc.getTarget().isBlank() || !tc.getTarget().contains("/")) {
            return false;
        } else if(!hasValidMethod){
            return false;
        }
        // 校验 2：expected 不能包含模糊词
        else if (tc.getExpected().contains("或") || tc.getExpected().contains("可能")) {
            return false;
        } else {
            return true;
        }
    }



    private VerifyAfter buildVerifyAfter(TestCase tc) {
        String target = tc.getTarget();

        // 只对 DELETE 请求做后置验证
        if (target == null || !target.startsWith("DELETE ")) {
            return null;
        }

        // 把 DELETE 替换成 GET，路径不变
        String getTarget = "GET " + target.substring("DELETE ".length());

        VerifyAfter va = new VerifyAfter();
        //删除后用一个get请求检查删除情况
        va.setTarget(getTarget);
        va.setExpectedStatus(404);   // 删除成功后，再查应返回404且查询响应值为空
        va.setResponse("用户不存在");
        return va;
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


