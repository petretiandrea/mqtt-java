package it.petretiandrea.utils;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class CustomLogger {

    public final static Logger LOGGER;
    private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

    static {
        Logger mainLogger = Logger.getLogger("mqtt.logger");
        mainLogger.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format(format,
                        new Date(record.getMillis()),
                        record.getLevel().getLocalizedName(),
                        record.getMessage());
            }
        });
        mainLogger.addHandler(consoleHandler);
        LOGGER = mainLogger;
    }

}
