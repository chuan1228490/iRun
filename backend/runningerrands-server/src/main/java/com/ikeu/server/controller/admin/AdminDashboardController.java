package com.ikeu.server.controller.admin;

import com.ikeu.common.result.Result;
import com.ikeu.model.vo.DashboardVO;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-仪表盘")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;

    @RequireRole({1, 2})
    @Operation(summary = "仪表盘统计")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(adminService.getDashboard());
    }
}
