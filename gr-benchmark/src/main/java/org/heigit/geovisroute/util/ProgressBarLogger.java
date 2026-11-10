package org.heigit.geovisroute.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;

public class ProgressBarLogger {
    private static final String LOGGER_NAME = "ProgressBarLogger";
    private static Logger logger;

    private ProgressBarLogger() {
        // 私有构造函数，隐藏隐式的公有构造函数
    }

    private static synchronized Logger initializeLogger() {
        if (logger == null) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // 创建控制台 appender
            ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ConsoleAppender<>();
            appender.setContext(context);
            appender.setName(LOGGER_NAME);

            // 创建并设置编码器
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            // 简化模式：仅显示带回车符的消息
            encoder.setPattern("%msg\r");
            encoder.start();

            appender.setEncoder(encoder);
            appender.start();

            // 获取 logger 并设置 appender
            ch.qos.logback.classic.Logger logbackLogger = context.getLogger(LOGGER_NAME);
            logbackLogger.setAdditive(false);
            logbackLogger.addAppender(appender);

            logger = LoggerFactory.getLogger(LOGGER_NAME);
        }
        return logger;
    }

    public static synchronized Logger getLogger() {
        return initializeLogger();
    }
}
