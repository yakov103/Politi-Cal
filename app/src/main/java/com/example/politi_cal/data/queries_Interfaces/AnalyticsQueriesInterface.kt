package com.example.politi_cal.data.queries_Interfaces

import com.example.politi_cal.models.Category
import com.example.politi_cal.models.Celeb
import com.example.politi_cal.models.Company
import com.example.politi_cal.models.VoteOption

/**
 * This interface will contain the needed queries for the analytics in the app.
 */

interface AnalyticsQueriesInterface {
    /**
     * This query will get the votes for a given celeb
     */
    fun getCelebDistribution(celeb: Celeb): Map<VoteOption, Int>

    /**
     * This function will get the votes for a given company.
     * Each celeb in the company will have the same weight, which means if the company contain
     * n celebs, each celeb will have a weight of 1 precent of the distribution.
     * moreover, the celeb distribution will decide whether it will be 100% left or right for his
     * precents in the company distribution.
     * For example we have 2 celebs:
     *      A: left votes: 100, right votes: 10
     *      B: left votes 9, right votes: 25
     * The company disribution will be 50% left and 50% right since 50% of the celebs tagged as
     * left, and the other 50% tagged as right.
     */
    fun getCompanyDistribution(company: Company): Map<VoteOption, Int>

    /**
     * This function will get the votes for given category
     * Will work the same as company for each company in the category
     */

    fun getCategoryStatistics(category: Category): Map<VoteOption, Int>

    /**
     * This function will get the votes for all of the categories
     * Will work the same as Category for each category
     */

    fun getTotalDistribution(): Map<VoteOption, Int>

    /**
     * This query will get get the number of users registered to the app during specific time.
     * It will get the first date and will return how many registered since then
     */

    fun getNumberOfUsersByTime(start: Long): Int

    /**
     * This query will get get the number of users registered to the app during specific time.
     * It will get the first date and the last date and will return how many
     * registered since the first date till the last date
     */

    fun getNumberOfUsersByTime(start: Long, end: Long): Int

    /**
     * This function will get the number of users that registered in a specific year
     */

    fun getNumberOfUsersByYear(year: Int): Int

    /**
     * This function will get the number of users that registered in a given years and will
     * return a map that contains how many users registered in each month.
     */

    fun getNumberOfUsersByYear_MonthBasedData(year: Int): Map<Int, Int>
}