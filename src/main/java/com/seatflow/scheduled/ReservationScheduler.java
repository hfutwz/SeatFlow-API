package com.seatflow.scheduled;

import com.seatflow.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final CheckInService checkInService;

    /**
     * 每5分钟检查：签到前15分钟提醒
     */
    @Scheduled(fixedRate = 300000)
    public void remindBeforeCheckIn() {
        try {
            int count = checkInService.remindBeforeCheckIn();
            if (count > 0) log.info("签到提醒发送: {}条", count);
        } catch (Exception e) {
            log.error("签到提醒任务异常", e);
        }
    }

    /**
     * 每5分钟检查：签到逾期警告
     */
    @Scheduled(fixedRate = 300000)
    public void warnLateCheckIn() {
        try {
            int count = checkInService.warnLateCheckIn();
            if (count > 0) log.info("签到逾期警告: {}条", count);
        } catch (Exception e) {
            log.error("签到逾期警告任务异常", e);
        }
    }

    /**
     * 每5分钟检查：超时未签到自动取消
     */
    @Scheduled(fixedRate = 300000)
    public void autoCancelTimeout() {
        try {
            int count = checkInService.autoCancelTimeout();
            if (count > 0) log.info("自动取消超时预约: {}条", count);
        } catch (Exception e) {
            log.error("自动取消超时任务异常", e);
        }
    }
}
