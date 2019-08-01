package org.athento.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Date utils.
 */
public class DateUtils {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(DateUtils.class);

    private DateUtils() {
    }

    /**
     * Parse date.
     *
     * @param date
     * @param format
     * @return
     * @throws ParseException
     */
    public static Date parseDate(String date, String format) throws ParseException {
        return new SimpleDateFormat(format).parse(date);
    }

    /**
     * Format date.
     *
     * @param date
     * @param format
     * @return
     */
    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }


    /**
     * Format date from reserved word.
     *
     * @param key
     */
    public static Date formatFromReserved(String key) {
        if (key != null && key.toLowerCase().startsWith("now(")) {
            Pattern p = Pattern.compile("(now|NOW){1}\\(([\\+-]{0,1}[0-9]+[dmy]*)*\\)");
            Matcher m = p.matcher(key);
            if (m.matches()) {
                try {
                    int days;
                    int months;
                    int years;
                    Calendar gc = GregorianCalendar.getInstance();
                    String value = m.group(2);
                    if (value == null) {
                        return gc.getTime();
                    }
                    if (value.endsWith("d") || value.endsWith("D")) {
                        days = Integer.valueOf(value.substring(0, value.length() - 1));
                        gc.add(GregorianCalendar.DAY_OF_MONTH, days);
                    } else if (value.endsWith("m") || value.endsWith("M")) {
                        months = Integer.valueOf(value.substring(0, value.length() - 1));
                        gc.add(GregorianCalendar.MONTH, months);
                    } else if (value.endsWith("y") || value.endsWith("Y")) {
                        years = Integer.valueOf(value.substring(0, value.length() - 1));
                        gc.add(GregorianCalendar.YEAR, years);
                    }
                    return gc.getTime();
                } catch (NumberFormatException e) {
                    LOG.warn("Format from reserved error", e);
                }
            }
        }
        return null;
    }

}
