package com.classservice.billing.dto;

import java.util.List;

public record GenerateBillsResult(
    int generated,
    int skipped,
    List<BillDto> bills,
    List<String> skipReasons
) {}
