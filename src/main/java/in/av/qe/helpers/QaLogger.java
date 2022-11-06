package in.av.qe.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QaLogger {

    private final static Logger QA_LOGGER = LoggerFactory.getLogger(QaLogger.class.getSimpleName());

    public static Logger getQaLogger() { return QA_LOGGER; }
}
