package org.podval.calendar;

import org.junit.Test;
import org.junit.Assert;


public class MoladTest {

    @Test
    public void cycleNumbers() {
        cycleNumber(1, 1);
        cycleNumber(2, 1);
        cycleNumber(18, 1);
        cycleNumber(19, 1);
        cycleNumber(20, 2);
        cycleNumber(37, 2);
        cycleNumber(38, 2);
        cycleNumber(39, 3);

        cycleNumber(0, 0);
        cycleNumber(-1, 0);
        cycleNumber(-2, 0);
        cycleNumber(-18, 0);

        cycleNumber(-19, -1);
        cycleNumber(-20, -1);
        cycleNumber(-37, -1);
        cycleNumber(-38, -2);
        cycleNumber(-39, -2);
    }


    private void cycleNumber(final int year, final int cycle) {
        Assert.assertEquals(cycle, JewishCalendar.getInstance().cycleNumber(year));
    }


    @Test
    public void moladNumbers() {
        moladNumber(1, 1, JewishMonth.MarHeshvan);
        moladNumber(0, 1, JewishMonth.Tishri);
        moladNumber(-1, 0, JewishMonth.Elul);
        moladNumber(-2, 0, JewishMonth.Av);
    }


    private void moladNumber(final int number, final int year, final JewishMonth month) {
        Assert.assertEquals(number, JewishCalendar.getInstance().moladNumber(year, month));
    }


    @Test
    public void table5769() {
        // From http://www.chabad.org/library/article_cdo/aid/216238/jewish/Molad-Times.htm

        molad(5769, JewishMonth.Tishri, 3, JewishMonth.Tishri, 1, 2008, GregorianMonth.September, 30, 1, true, 58, 13);
        molad(5769, JewishMonth.MarHeshvan, 4, JewishMonth.Tishri, 30, 2008, GregorianMonth.October, 29, 2, false, 42, 14);
        molad(5769, JewishMonth.Kislev, 6, JewishMonth.Kislev, 1, 2008, GregorianMonth.November, 28, 3, true, 26, 15);
    }


    public void molad(
        final int year,
        final JewishMonth month,

        final int dayOfTheWeek,

        final JewishMonth jMonth,
        final int jDay,

        final int gYear,
        final GregorianMonth gMonth,
        final int gDay,
        final int hour,
        final boolean am,
        final int minute,
        final int parts
        )
    {
        final Date mDate = JewishCalendar.getInstance().moladDate(year, month);
        Assert.assertEquals(dayOfTheWeek, mDate.getDayOfTheWeek());

        Assert.assertEquals(jMonth, mDate.getMonth().month);
        Assert.assertEquals(jDay, mDate.getDay());

        final Date gDate = Date.create(gYear, gMonth, gDay)
            .setTime(hour + (am ? 0 : 12), minute, parts);

        Assert.assertEquals(gDate, mDate.toGregorian());
    }


//    private void moladTable(final int year) {
//        for (int m = 1; m <= Calendar.getJewish().monthsInYear(year); m++) {
//            System.out.println("Molad " + year + " " + m + " = " + Calendar.getJewish().moladDate(year, m));
//        }
//    }
}
