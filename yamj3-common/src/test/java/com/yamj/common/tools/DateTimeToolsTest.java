package com.yamj.common.tools;

import com.yamj.common.tools.DateTimeTools;
import java.util.Calendar;
import java.util.Date;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DateTimeToolsTest {

    private static Calendar cal = Calendar.getInstance();
    private static Date dateJava;
    private static DateTime dateTime;
    // Expected results
    private static final String EXP_DD_MM_YYYY = "25-12-2013";
    private static final String EXP_YYYY_MM_DD = "2013-12-25";
    private static final long EXP_1H_1M_1S = 3661000;

    public DateTimeToolsTest() {
        cal.clear();
        cal.set(2013, 11, 25);  // This is 25/12/2013 Month is ZERO based.
        dateJava = cal.getTime();
        dateTime = new DateTime(dateJava);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_Date() {
        System.out.println("convertDateToString");
        String result = DateTimeTools.convertDateToString(dateJava);
        assertEquals(EXP_YYYY_MM_DD, result);
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_Date_String() {
        System.out.println("convertDateToString");
        String dateFormat = "dd-MM-yyyy";
        String result = DateTimeTools.convertDateToString(dateJava, dateFormat);
        assertEquals(EXP_DD_MM_YYYY, result);
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_DateTime() {
        System.out.println("convertDateToString");
        String result = DateTimeTools.convertDateToString(dateTime);
        assertEquals(EXP_YYYY_MM_DD, result);
    }

    /**
     * Test of convertDateToString method, of class DateTimeTools.
     */
    @Test
    public void testConvertDateToString_DateTime_String() {
        System.out.println("convertDateToString");
        String dateFormat = "dd-MM-yyyy";
        String result = DateTimeTools.convertDateToString(dateTime, dateFormat);
        assertEquals(EXP_DD_MM_YYYY, result);
    }

    /**
     * Test of getDuration method, of class DateTimeTools.
     */
    @Test
    public void testGetDuration_Date_Date() {
        System.out.println("getDuration");
        long expResult = EXP_1H_1M_1S * 1000;
        Date start = dateJava;
        Date end = new Date(dateJava.getTime() + expResult);
        long result = DateTimeTools.getDuration(start, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDuration method, of class DateTimeTools.
     */
    @Test
    public void testGetDuration_Long_Long() {
        System.out.println("getDuration");
        long start = dateTime.getMillis();
        long end = dateTime.getMillis() + 3661000;
        long expResult = end - start;
        long result = DateTimeTools.getDuration(start, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDuration method, of class DateTimeTools.
     */
    @Test
    public void testGetDuration_DateTime_DateTime() {
        System.out.println("getDuration");
        DateTime start = dateTime;
        DateTime end = dateTime.plusHours(1).plusMinutes(1).plusSeconds(1);
        long expResult = (3600 + 60 + 1) * 1000;
        long result = DateTimeTools.getDuration(start, end);
        assertEquals(expResult, result);
    }

    /**
     * Test of processRuntime method, of class DateTimeTools.
     */
    @Test
    public void testProcessRuntime() {
        System.out.println("processRuntime");
        String runtime = "1hr30";
        int expResult = 90;
        int result = DateTimeTools.processRuntime(runtime);
        assertEquals(expResult, result);
    }

    /**
     * Test of formatDurationColon method, of class DateTimeTools.
     */
    @Test
    public void testFormatDurationColon() {
        System.out.println("formatDurationColon");
        String expResult = "1:01:01";
        String result = DateTimeTools.formatDurationColon(EXP_1H_1M_1S);
        assertEquals(expResult, result);

        expResult = "1:01:01.001";
        result = DateTimeTools.formatDurationColon(EXP_1H_1M_1S + 1);
        assertEquals(expResult, result);
    }

    /**
     * Test of formatDurationText method, of class DateTimeTools.
     */
    @Test
    public void testFormatDurationText() {
        System.out.println("formatDurationText");
        String expResult = "1h01m01s";
        String result = DateTimeTools.formatDurationText(EXP_1H_1M_1S);
        assertEquals(expResult, result);

        expResult = "1h01m01.001s";
        result = DateTimeTools.formatDurationText(EXP_1H_1M_1S + 1);
        assertEquals(expResult, result);
    }
}
