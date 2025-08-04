package com.pickyboy.guardian.strategy.impl;

import com.pickyboy.guardian.model.constant.GuardianConstants;
import com.pickyboy.guardian.model.strategy.ActionContext;
import com.pickyboy.guardian.strategy.ActionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志记录动作策略
 *
 * @author pickyboy
 */
@Slf4j
@Component
public class LogActionStrategy implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        String logMessage = String.format(
                "Guardian Alert - Level: %s, Scene: %s, Key: %s, Count: %d, Threshold: %d, Target: %s",
                context.getLevel(),
                context.getScene(),
                context.getKey(),
                context.getCurrentCount(),
                context.getThreshold(),
                context.getTarget()
        );

        switch (context.getLevel()) {
            case GuardianConstants.ALERT_LEVEL_INFO:
                log.info(logMessage);
                break;
            case GuardianConstants.ALERT_LEVEL_WARNING:
                log.warn(logMessage);
                break;
            case GuardianConstants.ALERT_LEVEL_ERROR:
            case GuardianConstants.ALERT_LEVEL_CRITICAL:
                log.error(logMessage);
                break;
            default:
                log.info(logMessage);
        }
    }

    @Override
    public String getType() {
        return GuardianConstants.ACTION_TYPE_LOG;
    }



    @Override
    public String getDescription() {
        return "记录告警日志";
    }
}