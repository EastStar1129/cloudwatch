package com.sap.cloudwatch.mock;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class MockController {
    @RequestMapping("/test")
    public String test() {
        log.info("##### logging test");
        return "test";
    }
}
