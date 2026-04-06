package com.financeai.finance_management.scheduler;

import com.financeai.finance_management.entity.SalaryConfig;
import com.financeai.finance_management.enums.FrequencyType;
import com.financeai.finance_management.repository.SalaryConfigRepository;
import com.financeai.finance_management.service.ISalaryConfigService;
import com.financeai.finance_management.service.impl.SalaryConfigServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AutoImportScheduler {
    private final SalaryConfigRepository configRepository;
    private final ISalaryConfigService salaryConfigService;

//    @Scheduled(cron = "0 0 1 * * *")
    @Scheduled(cron = "0 40 18 * * *")
    public void scanAndExecute() {
        LocalDate now = LocalDate.now();
        int dom = now.getDayOfMonth();
        int dow = now.getDayOfWeek().getValue(); // 1=Mon, 7=Sun

        List<SalaryConfig> activeConfigs = configRepository.findConfigsToExecute(dom, dow);

        if (now.equals(now.with(TemporalAdjusters.lastDayOfMonth()))) {
            List<SalaryConfig> overflowConfigs = configRepository.findOverflowMonthlyConfigs(dom);
            activeConfigs.addAll(overflowConfigs);
        }

        for (SalaryConfig config : activeConfigs) {
            if (isAlreadyProcessed(config, now)) continue;

            salaryConfigService.executeAutoJob(config);
        }
    }

    private boolean isAlreadyProcessed(SalaryConfig config, LocalDate now) {
        if (config.getLastProcessed() == null) return false;
        LocalDate lastDate = Instant.ofEpochMilli(config.getLastProcessed())
                .atZone(ZoneId.systemDefault()).toLocalDate();

        if (config.getFrequency() == FrequencyType.MONTHLY) {
            return lastDate.getMonth() == now.getMonth() && lastDate.getYear() == now.getYear();
        } else {
            return ChronoUnit.DAYS.between(lastDate, now) < 7;
        }
    }
}