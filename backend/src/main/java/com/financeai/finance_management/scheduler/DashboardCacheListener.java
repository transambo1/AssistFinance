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
    int txYear =
        Instant.ofEpochMilli(event.getTransactionDate()).atZone(ZoneId.systemDefault()).getYear();

    evictAndReload(event.getUserId(), txYear, event.getType());
  }

  private void evictAndReload(String userId, int txYear, TransactionType type) {
    Cache forecastCache = cacheManager.getCache("dashboardForecast");
    Cache analyticsCache = cacheManager.getCache("dashboardAnalytics");

    String commonSuffix = userId + "-" + txYear;

    if (forecastCache != null) {
      if (type == TransactionType.EXPENSE) {
        forecastCache.evict("expense-" + commonSuffix);
      } else {
        forecastCache.evict("income-" + commonSuffix);
      }
    }

    if (analyticsCache != null) {
      analyticsCache.evict(commonSuffix);
    }

    log.info("Đã xóa cache {} cho user {} tại năm {}", type, userId, txYear);

    if (type == TransactionType.EXPENSE) {
      dashboardService.getForecastDashboard(userId, txYear);
      //            dashboardService.getDashboardAnalytics(userId, txYear);
    } else {
      dashboardService.getIncomeForecastDashboard(userId, txYear);
    }

    log.info("Đã cập nhật xong toàn bộ Dashboard cache sau khi thay đổi {}", type);
  }
}
