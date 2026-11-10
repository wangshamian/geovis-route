package org.heigit.geovisroute.util;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ProgressBarLogger {

    private static final String LOGGER_NAME = "ProgressBarLogger";
    private static final Logger CLASS_LOGGER = Logger.getLogger(ProgressBarLogger.class);


    private static Logger initializeLogger() {
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        if (Logger.getRootLogger().getAppender(LOGGER_NAME) == null) {
            String originalConsolePattern;
            try {
                originalConsolePattern = config.getAppender("Console").getLayout().getContentFormat().get("format");
            } catch (Exception e) {
                originalConsolePattern = "%d{yyyy-MM-dd HH:mm:ss} %highlight{%-7p} %style{%50t}{Cyan} %style{[ %-40.40c{1.} ]}{Bright Cyan}   %m";
            }

            // 移除模式末尾的换行符
            if (originalConsolePattern.endsWith("%n")) {
                originalConsolePattern = originalConsolePattern.substring(0, originalConsolePattern.length() - 2);
            }

            // 移除模式开头的换行符
            if (originalConsolePattern.startsWith("%n")) {
                originalConsolePattern = originalConsolePattern.substring(2);
            }

            final PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern(originalConsolePattern + "\r")
                    .withConfiguration(config)
                    .build();

            Appender consoleAppender = ConsoleAppender.newBuilder()
                    .setConfiguration(config)
                    .setName(LOGGER_NAME)
                    .setLayout(layout)
                    .setFilter(null)
                    .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                    .setName(LOGGER_NAME)
                    .setFollow(false)
                    .build();
            consoleAppender.start();

            // 创建新的 appender 引用
            AppenderRef ref = AppenderRef.createAppenderRef(LOGGER_NAME, null, null);
            AppenderRef[] refs = new AppenderRef[]{ref};
            LoggerConfig loggerConfig = LoggerConfig.newBuilder()
                    .withAdditivity(false)
                    .withLevel(CLASS_LOGGER.getLevel().getVersion2Level())
                    .withLoggerName(LOGGER_NAME)
                    .withIncludeLocation("true")
                    .withRefs(refs)
                    .withProperties(null)
                    .withConfig(config)
                    .withtFilter(null)
                    .build();

            config.addAppender(consoleAppender);
            loggerConfig.addAppender(consoleAppender, null, null);
            config.addLogger(LOGGER_NAME, loggerConfig);
            context.updateLoggers();
        }
        return Logger.getLogger(LOGGER_NAME);
    }

    public static String getLoggerName() {
        return LOGGER_NAME;
    }

    public static Logger getLogger() {
        return initializeLogger();
    }
}
