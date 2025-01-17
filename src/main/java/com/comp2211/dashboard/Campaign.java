package com.comp2211.dashboard;

import com.comp2211.dashboard.model.data.Bounce;
import com.comp2211.dashboard.model.data.Bounce.BounceType;
import com.comp2211.dashboard.model.data.Demographics.Demographic;
import com.comp2211.dashboard.model.data.Filter;
import com.comp2211.dashboard.util.Logger;
import com.comp2211.dashboard.io.DatabaseManager;
import com.comp2211.dashboard.io.DatabaseManager.Cost;

import de.saxsys.mvvmfx.MvvmFX;
import com.comp2211.dashboard.viewmodel.DatabaseViewModel;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * Campaign object used to represent each individual campaign. Contains impression, click and server
 * info.
 */
public class Campaign {

  public static final String RESET_GRAN = "RESET_GRANULARITY";

  // Used as a singleton across app
  private static List<Campaign> allCampaigns = new ArrayList<>();

  private String campaignName;
  private int campaignID;
  private DatabaseManager dbManager;

  private Filter appliedFilter;
  private Bounce bounceDefinition;
  private byte totalsGranularity, avgsGranularity, costTotalsGranularity, ratesGranularity;

  private BigDecimal totalClickCost, totalImpressionCost, averageAcquisitionCost;
  private long clickDataCount, impressionDataCount, serverDataCount, uniquesCount, bouncesCount, conversionsCount;

  private HashMap<String, BigDecimal> cachedDatedAcquisitionCostAverages, cachedDatedImpressionCostAverages, cachedDatedClickCostAverages;
  private HashMap<String, Long> cachedDatedImpressionTotals, cachedDatedClickTotals, cachedDatedUniqueTotals, cachedDatedBounceTotals, cachedDatedAcquisitionTotals;
  private HashMap<String, BigDecimal> cachedAgePercentage, cachedGenderPercentage, cachedIncomePercentage, cachedContextPercentage;
  private HashMap<String, BigDecimal> cachedDatedCostTotals;
  private HashMap<String, BigDecimal> cachedDatedBounceRates, cachedDatedCTRs;

  public static List<Campaign> getCampaigns(){
    return allCampaigns;
  }
  public static void removeAllCampaigns() { allCampaigns = new ArrayList<>(); }

  public static Campaign getCampaignByID(int id){
    for(Campaign c : allCampaigns) {
      if(c.getCampaignID() == id) {
        return c;
      }
    }
    return null;
  }

  /**
   * Campaign constructor, initialises global variables / cached data maps
   * @param campaignID ID for campaign
   * @param campaignName name for campaign
   * @param dbManager database manager for the campaign
   */
  public Campaign(int campaignID, String campaignName, DatabaseManager dbManager) {
    this.campaignID = campaignID;
    this.campaignName = campaignName;
    this.dbManager = Objects.requireNonNull(dbManager, "dbManager must not be null");

    totalsGranularity = 24;
    avgsGranularity = 24;
    costTotalsGranularity = 24;
    ratesGranularity = 24;

    cachedDatedAcquisitionCostAverages = new LinkedHashMap<>();
    cachedDatedImpressionCostAverages = new LinkedHashMap<>();
    cachedDatedClickCostAverages = new LinkedHashMap<>();

    cachedDatedImpressionTotals = new LinkedHashMap<>();
    cachedDatedClickTotals = new LinkedHashMap<>();
    cachedDatedUniqueTotals = new LinkedHashMap<>();
    cachedDatedBounceTotals = new LinkedHashMap<>();
    cachedDatedAcquisitionTotals = new LinkedHashMap<>();

    cachedAgePercentage = new LinkedHashMap<>();
    cachedGenderPercentage = new LinkedHashMap<>();
    cachedIncomePercentage = new LinkedHashMap<>();
    cachedContextPercentage = new LinkedHashMap<>();

    cachedDatedCostTotals = new LinkedHashMap<>();

    cachedDatedBounceRates = new LinkedHashMap<>();
    cachedDatedCTRs = new LinkedHashMap<>();

    updateBouncesByPages((byte) 1, new Filter(campaignID));
    bounceDefinition = new Bounce((byte) 1);

    //allCampaigns.add(this); todo:check if needed
    DatabaseViewModel.addNewCampaign(this);
  }

  @Override
  public String toString(){
    return getCampaignName();
  }

  public int getCampaignID() {
    return campaignID;
  }

  /**
   * Get the name of the campaign
   * @return the name
   */
  public String getCampaignName() {
    return campaignName;
  }

  public boolean hasAppliedFilter() {
    return appliedFilter != null;
  }
  public Filter getAppliedFilter() {
    return appliedFilter;
  }

  public BounceType getBounceType() {
    return bounceDefinition.getType();
  }
  public Bounce getBounceDefinition() {
    return bounceDefinition;
  }
  /*public void setBounceDefinition(byte maxPages) {
    bounceDefinition = new Bounce(maxPages);
  }
  public void setBounceDefinition(long maxSeconds, boolean allowInf) {
    bounceDefinition = new Bounce(maxSeconds, allowInf);
  }*/

  /**
   * If filter is different to current appliedFilter, retrieves data from database manager and stores it in the cache variables
   * @param filter new filter to apply
   */
  public void cacheData(Filter filter) {
    //TODO Edit stdout/logs messages
    if (appliedFilter != null && filter.isEqualTo(appliedFilter)) {
      Logger.log("[INFO] [Campaign " + this.campaignName + "] Data not cached - filter provided was identical to current applied filter.");
      return;
    }

    clickDataCount = dbManager.retrieveDataCount(DatabaseManager.Table.click_table, filter);
    impressionDataCount = dbManager.retrieveDataCount(DatabaseManager.Table.impression_table, filter);
    serverDataCount = dbManager.retrieveDataCount(DatabaseManager.Table.server_table, filter);
    uniquesCount = dbManager.retrieveDataCount(DatabaseManager.Table.click_table, true, filter);
    conversionsCount = dbManager.retrieveAcquisitionCount(filter);

    totalClickCost = dbManager.retrieveTotalCost(Cost.Click_Cost, filter);
    totalImpressionCost = dbManager.retrieveTotalCost(Cost.Impression_Cost, filter);
    //averageAcquisitionCost = dbManager.retrieveAverageAcquisitionCost(filter);

    clearCache();

    cachedDatedClickCostAverages.putAll(dbManager.retrieveDatedAverageCost(Cost.Click_Cost, avgsGranularity, filter));
    cachedDatedImpressionCostAverages.putAll(dbManager.retrieveDatedAverageCost(Cost.Impression_Cost, avgsGranularity, filter));
    cachedDatedAcquisitionCostAverages.putAll(dbManager.retrieveDatedAverageAcquisitionCost(avgsGranularity, filter));

    cachedDatedImpressionTotals.putAll(dbManager.retrieveDatedImpressionTotals(totalsGranularity, filter));
    cachedDatedClickTotals.putAll(dbManager.retrieveDatedClickTotals(totalsGranularity, filter));
    cachedDatedUniqueTotals.putAll(dbManager.retrieveDatedUniqueTotals(totalsGranularity, filter));
    cachedDatedAcquisitionTotals.putAll(dbManager.retrieveDatedAcquisitionTotals(totalsGranularity, filter));

    cachedAgePercentage.putAll(percentageMap(Demographic.Age, dbManager.retrieveDemographics(Demographic.Age, filter)));
    cachedGenderPercentage.putAll(percentageMap(Demographic.Gender, dbManager.retrieveDemographics(Demographic.Gender, filter)));
    cachedIncomePercentage.putAll(percentageMap(Demographic.Income, dbManager.retrieveDemographics(Demographic.Income, filter)));
    cachedContextPercentage.putAll(percentageMap(Demographic.Context, dbManager.retrieveDemographics(Demographic.Context, filter)));

    cachedDatedCostTotals.putAll(calcDatedSums(dbManager.retrieveDatedCostTotals(Cost.Impression_Cost, costTotalsGranularity, filter), dbManager.retrieveDatedCostTotals(Cost.Click_Cost, costTotalsGranularity, filter)));

    //cachedDatedBounceRates.putAll(calcDatedRates(cachedDatedBounceTotals, dbManager.retrieveDatedServerTotals(ratesGranularity, filter)));
    cachedDatedCTRs.putAll(calcDatedRates(dbManager.retrieveDatedClickTotals(ratesGranularity, filter), dbManager.retrieveDatedImpressionTotals(ratesGranularity, filter)));

    appliedFilter = filter;

    allCampaigns.add(this);
    DatabaseViewModel.changeProgressToCompleted(this);
    Logger.log("[INFO] [Campaign " + this.campaignName + "] Data cached successfully.");
  }

  /**
   * Clears all cached data maps
   */
  public void clearCache() {
    cachedDatedClickCostAverages.clear();
    cachedDatedImpressionCostAverages.clear();
    cachedDatedAcquisitionCostAverages.clear();

    cachedDatedImpressionTotals.clear();
    cachedDatedClickTotals.clear();
    cachedDatedUniqueTotals.clear();
    cachedDatedBounceTotals.clear();
    cachedDatedAcquisitionTotals.clear();

    cachedAgePercentage.clear();
    cachedGenderPercentage.clear();
    cachedIncomePercentage.clear();
    cachedContextPercentage.clear();

    cachedDatedCostTotals.clear();

    cachedDatedBounceRates.clear();
    cachedDatedCTRs.clear();
  }

  public long getClickDataCount() {
    return clickDataCount;
  }

  public long getImpressionDataCount() {
    return impressionDataCount;
  }

  public long getServerDataCount() {
    return serverDataCount;
  }

  public long getUniquesCount() {
    return uniquesCount;
  }

  public long getBouncesCount() {
    return bouncesCount;
  }

  public long getConversionsCount() {
    return conversionsCount;
  }

  public BigDecimal getTotalClickCost() {
    return totalClickCost;
  }

  public BigDecimal getTotalImpressionCost() {
    return totalImpressionCost;
  }

  /**
   * Calculates the average cost per click using the total click cost divided by the number of clicks
   * @return Average cost per click given in pence
   */
  public BigDecimal getAvgCostPerClick() {
    if (clickDataCount == 0) {
      return BigDecimal.ZERO;
    }
    return getTotalCost().divide(BigDecimal.valueOf(clickDataCount), 6, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the click through rate in percentage using click and impression lists
   * @return click through rate as a percentage
   */
  public BigDecimal getClickThroughRate() {
    if (impressionDataCount == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(clickDataCount).divide(BigDecimal.valueOf(impressionDataCount), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  /**
   * Updates all graphs' data to be granularity of 24hrs
   */
  public void resetGranularity() {
    updateTotalsGranularity((byte) 24);
    updateAvgsGranularity((byte) 24);
    updateCostTotalsGranularity((byte) 24);
    updateRatesGranularity((byte) 24);
    /* Received by PrimaryView and PrimaryViewModel to update information there */
    MvvmFX.getNotificationCenter().publish(Campaign.RESET_GRAN);
  }

  /**
   * Updates values in data maps to a new granularity for all data related to the totals graph
   * @param hoursGranularity number of hours to change the granularity to
   */
  public void updateTotalsGranularity(byte hoursGranularity) {
    totalsGranularity = hoursGranularity;
    cachedDatedImpressionTotals.clear();
    cachedDatedImpressionTotals.putAll(dbManager.retrieveDatedImpressionTotals(totalsGranularity, appliedFilter));
    cachedDatedClickTotals.clear();
    cachedDatedClickTotals.putAll(dbManager.retrieveDatedClickTotals(totalsGranularity, appliedFilter));
    cachedDatedUniqueTotals.clear();
    cachedDatedUniqueTotals.putAll(dbManager.retrieveDatedUniqueTotals(totalsGranularity, appliedFilter));

    cachedDatedBounceTotals.clear();
    if (getBounceType().equals(BounceType.Pages))
      cachedDatedBounceTotals.putAll(dbManager.retrieveDatedBounceTotalsByPages(totalsGranularity, bounceDefinition.getMaxPages(), appliedFilter));
    else
      cachedDatedBounceTotals.putAll(dbManager.retrieveDatedBounceTotalsByTime(totalsGranularity, bounceDefinition.getMaxSeconds(), bounceDefinition.allowInf(), appliedFilter));

    cachedDatedAcquisitionTotals.clear();
    cachedDatedAcquisitionTotals.putAll(dbManager.retrieveDatedAcquisitionTotals(totalsGranularity, appliedFilter));
  }

  /**
   * Updates values in data maps to a new granularity for all data related to the cost averages graph
   * @param hoursGranularity number of hours to change the granularity to
   */
  public void updateAvgsGranularity(byte hoursGranularity) {
    avgsGranularity = hoursGranularity;
    cachedDatedImpressionCostAverages.clear();
    cachedDatedImpressionCostAverages.putAll(dbManager.retrieveDatedAverageCost(Cost.Impression_Cost, avgsGranularity, appliedFilter));
    cachedDatedClickCostAverages.clear();
    cachedDatedClickCostAverages.putAll(dbManager.retrieveDatedAverageCost(Cost.Click_Cost, avgsGranularity, appliedFilter));
    cachedDatedAcquisitionCostAverages.clear();
    cachedDatedAcquisitionCostAverages.putAll(dbManager.retrieveDatedAverageAcquisitionCost(avgsGranularity, appliedFilter));
  }

  /**
   * Updates values in data maps to a new granularity for all data related to the cost totals graph
   * @param hoursGranularity number of hours to change the granularity to
   */
  public void updateCostTotalsGranularity(byte hoursGranularity) {
    costTotalsGranularity = hoursGranularity;
    cachedDatedCostTotals.clear();
    cachedDatedCostTotals.putAll(calcDatedSums(dbManager.retrieveDatedCostTotals(Cost.Impression_Cost, costTotalsGranularity, appliedFilter), dbManager.retrieveDatedCostTotals(Cost.Click_Cost, costTotalsGranularity, appliedFilter)));
  }

  /**
   * Updates values in data maps to a new granularity for all data related to the rates graph
   * @param hoursGranularity number of hours to change the granularity to
   */
  public void updateRatesGranularity(byte hoursGranularity) {
    ratesGranularity = hoursGranularity;

    HashMap<String, Long> serverTotals = dbManager.retrieveDatedServerTotals(ratesGranularity, appliedFilter);
    cachedDatedBounceRates.clear();
    if (getBounceType().equals(BounceType.Pages))
      cachedDatedBounceRates.putAll(calcDatedRates(dbManager.retrieveDatedBounceTotalsByPages(ratesGranularity, bounceDefinition.getMaxPages(), appliedFilter), serverTotals));
    else
      cachedDatedBounceRates.putAll(calcDatedRates(dbManager.retrieveDatedBounceTotalsByTime(ratesGranularity, bounceDefinition.getMaxSeconds(), bounceDefinition.allowInf(), appliedFilter), serverTotals));

    cachedDatedCTRs.clear();
    cachedDatedCTRs.putAll(calcDatedRates(dbManager.retrieveDatedClickTotals(ratesGranularity, appliedFilter), dbManager.retrieveDatedImpressionTotals(ratesGranularity, appliedFilter)));
  }

  /**
   * Updates values in bounce-related data maps to values calculated by time
   * @param maxSeconds maximum seconds between entry and exit for which a bounce is counted
   * @param allowInf whether to count entries with no exit datetime as a bounce
   * @param filter filter to apply
   */
  public void updateBouncesByTime(long maxSeconds, boolean allowInf, Filter filter) {
    if (maxSeconds < 0) {
      Logger.log("[WARNING] Attempted bounce calculation with negative 'maxSeconds' value.");
      return;
    }
    bounceDefinition = new Bounce(maxSeconds, allowInf);
    bouncesCount = dbManager.retrieveBouncesCountByTime(maxSeconds, allowInf, filter);
    cachedDatedBounceTotals.clear();
    cachedDatedBounceTotals.putAll(dbManager.retrieveDatedBounceTotalsByTime(totalsGranularity, maxSeconds, allowInf, filter));
    cachedDatedBounceRates.clear();
    cachedDatedBounceRates.putAll(calcDatedRates(dbManager.retrieveDatedBounceTotalsByTime(ratesGranularity, maxSeconds, allowInf, filter), dbManager.retrieveDatedServerTotals(ratesGranularity, filter)));
  }

  /**
   * Updates values in bounce-related data maps to values calculated by pages visited
   * @param maxPages maximum pages visited for which a bounce is counted
   * @param filter filter to apply
   */
  public void updateBouncesByPages(byte maxPages, Filter filter) {
    if (maxPages < 0) {
      Logger.log("[WARNING] Attempted bounce calculation with negative 'maxPages' value.");
      return;
    }
    bounceDefinition = new Bounce(maxPages);
    bouncesCount = dbManager.retrieveBouncesCountByPages(maxPages, filter);
    cachedDatedBounceTotals.clear();
    cachedDatedBounceTotals.putAll(dbManager.retrieveDatedBounceTotalsByPages(totalsGranularity, maxPages, filter));
    cachedDatedBounceRates.clear();
    cachedDatedBounceRates.putAll(calcDatedRates(dbManager.retrieveDatedBounceTotalsByPages(ratesGranularity, maxPages, filter), dbManager.retrieveDatedServerTotals(ratesGranularity, filter)));
  }

  /**
   * Calculates the bounce rate as the percentage of server entries that result in a bounce
   * @return bounce rate as a percentage
   */
  public BigDecimal getBounceRate() {
    if (serverDataCount == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(bouncesCount).divide(BigDecimal.valueOf(serverDataCount), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  /**
   * Calculates the ratio of number of bounces to number of conversions
   * @return bounces per conversion as a decimal value
   */
  public BigDecimal getBouncesPerConversion() {
    if (conversionsCount == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(bouncesCount).divide(BigDecimal.valueOf(conversionsCount), 6, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the ratio of number of conversions to number of unique click users
   * @return conversions per unique as a decimal value
   */
  public BigDecimal getConversionsPerUnique() {
    if (uniquesCount == 0) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(conversionsCount).divide(BigDecimal.valueOf(uniquesCount), 6, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the total cost of the campaign using the sum of the costs of impression and clicks
   * @return total cost of a campaign in pence
   */
  public BigDecimal getTotalCost() {
    return getTotalClickCost().add(getTotalImpressionCost());
  }

  /*/**
   * Calculates the average cost per acquisition/conversion by summing all converted clicks and
   * dividing by the count.
   *
   * @return The average cost per acquisition given in pence.
   */
  /*public BigDecimal getAvgCostPerAcquisition() {
    return averageAcquisitionCost;
  }*/

  /*/**
   * Calculates/estimates the cost per thousand impression by calculating the average cost of a
   * single impression multiplied by 1000
   *
   * @return The average cost per acquisition given in pence.
   */
  /*public BigDecimal getCostPerThousandImpressions() {
    if (impressionDataCount == 0) {
      return BigDecimal.ZERO;
    }
    return getTotalImpressionCost().divide(BigDecimal.valueOf(impressionDataCount), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1000));
  }*/

  public HashMap<String, BigDecimal> getDatedAcquisitionCostAverages() {
    return cachedDatedAcquisitionCostAverages;
  }

  public HashMap<String, BigDecimal> getDatedImpressionCostAverages() {
    return cachedDatedImpressionCostAverages;
  }

  public HashMap<String, BigDecimal> getDatedClickCostAverages() {
    return cachedDatedClickCostAverages;
  }

  public HashMap<String, Long> getDatedImpressionTotals() {
    return cachedDatedImpressionTotals;
  }

  public HashMap<String, Long> getDatedClickTotals() {
    return cachedDatedClickTotals;
  }

  public HashMap<String, Long> getDatedUniqueTotals() {
    return cachedDatedUniqueTotals;
  }

  public HashMap<String, Long> getDatedBounceTotals() {
    return cachedDatedBounceTotals;
  }

  public HashMap<String, Long> getDatedAcquisitionTotals() {
    return cachedDatedAcquisitionTotals;
  }

  /**
   * Calculates the percentage of each group for a given Demographic type
   * @param type the Demographic type to use
   * @return HashMap containing the groups as keys, and the percentage of each group in the campaign
   */
  public HashMap<String, BigDecimal> getPercentageMap(Demographic type) {
    switch(type) {
      case Age:
        return getAgePercentage();
      case Context:
        return getContextPercentage();
      case Gender:
        return getGenderPercentage();
      case Income:
        return getIncomePercentage();
      default:
        return null;
    }
  }

  private HashMap<String, BigDecimal> getAgePercentage() {
    return cachedAgePercentage;
  }

  private HashMap<String, BigDecimal> getGenderPercentage() {
    return cachedGenderPercentage;
  }

  private HashMap<String, BigDecimal> getIncomePercentage() {
    return cachedIncomePercentage;
  }

  private HashMap<String, BigDecimal> getContextPercentage() {
    return cachedContextPercentage;
  }

  /**
   * Retrieves the start date of the campaign from the database manager
   * @return start date as a string
   */
  public String getCampaignStartDate(){
    return dbManager.retrieveCampaignStartDate(new Filter(getCampaignID()));
  }

  /**
   * Retrieves the end date of the campaign from the database manager
   * @return end date as a string
   */
  public String getCampaignEndDate(){
    return dbManager.retrieveCampaignEndDate(new Filter(getCampaignID()));
  }

  /**
   * Calculates the percentages of total impressions each value of a demographic contains, as a map of demographic subgroup to percentage
   * @param demographic type of demographic to return the percentages of
   * @param dataMap map containing total numbers of a demographic's subgroup
   * @return map of demographic subgroup to percentage
   */
  private HashMap<String, BigDecimal> percentageMap (Demographic demographic, HashMap<String, Long> dataMap) {
    HashMap<String, BigDecimal> resultMap = new LinkedHashMap<>();
    for (Entry<String, Long> entry : dataMap.entrySet()) {
      Long count = entry.getValue();
      if (count == null || count == 0 || impressionDataCount == 0) {
        resultMap.put(entry.getKey(), BigDecimal.ZERO);
      } else {
        resultMap.put(entry.getKey(), BigDecimal.valueOf(count).divide(BigDecimal.valueOf(impressionDataCount), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
      }
    }
    return resultMap;
  }

  public HashMap<String, BigDecimal> getDatedCostTotals() {
    return cachedDatedCostTotals;
  }

  /**
   * Calculates, given two maps of dates to sum for that date, the sum of matching datetimes, returning the combined map
   * @param datedSums1 a map containing datetimes to any particular value for that date
   * @param datedSums2 same as datedSums1
   * @return map containing all datetimes in both given maps, with the sum of values in each
   */
  private static HashMap<String, BigDecimal> calcDatedSums(HashMap<String, BigDecimal> datedSums1, HashMap<String, BigDecimal> datedSums2) {
    HashMap<String, BigDecimal> returnMap = new LinkedHashMap<>();
    for (Entry<String, BigDecimal> sums1Entry : datedSums1.entrySet()) {
      String date = sums1Entry.getKey();
      BigDecimal sum1 = sums1Entry.getValue();
      BigDecimal sum2 = BigDecimal.ZERO;
      if (datedSums2.containsKey(date)) {
        sum2 = datedSums2.get(date);
      }
      /* Adds the value in map 1 to the value in map 2 */
      BigDecimal sum = sum1.add(sum2);
      returnMap.put(date, sum);
    }
    return returnMap;
  }

  public HashMap<String, BigDecimal> getDatedBounceRates() {
    return cachedDatedBounceRates;
  }

  public HashMap<String, BigDecimal> getDatedCTRs() {
    return cachedDatedCTRs;
  }

  /**
   * Calculates, given two maps of dates to values for that date, the ratio of matching datetimes, returning the combined map
   * @param datedDividendTotals a map containing datetimes to any particular value for that date, with values to be used as the dividends
   * @param datedDivisorTotals same as datedSums1, but the values are used as divisors
   * @return map containing all datetimes in both given maps, with the results of value-divisions in each
   */
  private static HashMap<String, BigDecimal> calcDatedRates(HashMap<String, Long> datedDividendTotals, HashMap<String, Long> datedDivisorTotals) {
    HashMap<String, BigDecimal> returnMap = new LinkedHashMap<>();
    for (Entry<String, Long> divisorEntry : datedDivisorTotals.entrySet()) {
      String date = divisorEntry.getKey();
      Long divisor = divisorEntry.getValue();
      if (divisor == 0L) {
        returnMap.put(date, BigDecimal.ZERO);
      } else {
        Long dividend = 0L;
        if (datedDividendTotals.containsKey(date)) {
          dividend = datedDividendTotals.get(date);
        }
        /* Divides the value in map 1 by the value in map 2 */
        BigDecimal bounceRate = BigDecimal.valueOf(dividend).divide(BigDecimal.valueOf(divisor), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        returnMap.put(date, bounceRate);
      }
    }
    return returnMap;
  }
}
