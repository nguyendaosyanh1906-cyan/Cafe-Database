package com.cafe.cafeshopmanagement.dao;

import java.util.Map;

public interface ReportDAO {
    /** Today and previous six calendar days; only days with COMPLETED orders. */
    Map<String, Integer> getRevenueLast7Days();
    /** All-time COMPLETED quantities; keys are name + " [#productId]" to preserve identity. */
    Map<String, Integer> getTopSellingProducts();
    int getTodayRevenue();
}