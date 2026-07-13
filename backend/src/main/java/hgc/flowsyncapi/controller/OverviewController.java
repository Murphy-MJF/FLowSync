package hgc.flowsyncapi.controller;

import hgc.flowsyncapi.common.ApiResponse;
import hgc.flowsyncapi.service.OverviewService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/overview")
public class OverviewController {
    @Resource
    private OverviewService overviewService;

    @GetMapping
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(overviewService.getOverview());
    }
}
