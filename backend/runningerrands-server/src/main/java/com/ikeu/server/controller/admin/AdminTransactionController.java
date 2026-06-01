package com.ikeu.server.controller.admin;

import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.TransactionVO;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "管理端-资金流水")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final TransactionService transactionService;

    @RequireRole({1, 2})
    @Operation(summary = "资金流水列表")
    @GetMapping("/transactions")
    public Result<PageResult<TransactionVO>> listTransactions(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(transactionService.listAllTransactions(type, userId, start, end, page, size));
    }
}
