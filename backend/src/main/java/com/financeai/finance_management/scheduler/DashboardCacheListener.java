package com.financeai.finance_management.scheduler;

import com.financeai.finance_management.dto.request.TransactionChangedEvent;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.service.IDashboardService;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardCacheListener {

  private final IDashboardService dashboardService;
  private final CacheManager cacheManager;

  @Async
  @EventListener
  public void handleTransactionChange(TransactionChangedEvent event) {
    if (event.getType() == TransactionType.EXPENSE) {
      int txYear =
          Instant.ofEpochMilli(event.getTransactionDate()).atZone(ZoneId.systemDefault()).getYear();
      String cacheKey = event.getUserId() + "-" + txYear;

      evictAndReload(event.getUserId(), txYear, cacheKey);
    }
  }

  private void evictAndReload(String userId, int txYear, String cacheKey) {
    Cache forecastCache = cacheManager.getCache("dashboardForecast");
    //    Cache analyticsCache = cacheManager.getCache("dashboardAnalytics");

    if (forecastCache != null) forecastCache.evict(cacheKey);
    //    if (analyticsCache != null) analyticsCache.evict(cacheKey);

    log.info("Đã xóa cache cho user {} tại năm {}", userId, txYear);

    // Reload lại để lần sau user vào là có sẵn luôn (Warm up cache)
    dashboardService.getForecastDashboard(userId, txYear);
    //    dashboardService.getDashboardAnalytics(userId, txYear);

    log.info("Đã cập nhật xong toàn bộ Dashboard cache cho năm: {}", txYear);
  }
}
