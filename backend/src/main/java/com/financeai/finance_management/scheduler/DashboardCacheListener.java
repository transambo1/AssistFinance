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

      Cache cache = cacheManager.getCache("dashboardForecast");
      if (cache != null) {
        String cacheKey = event.getUserId() + "-" + txYear;
        cache.evict(cacheKey);
        log.info("Đã xóa cache cho user {} tại năm {}", event.getUserId(), txYear);
      }
      dashboardService.getForecastDashboard(event.getUserId(), txYear);
      log.info("Đã cập nhật xong cache cho năm: {}", txYear);
    }
  }
}
