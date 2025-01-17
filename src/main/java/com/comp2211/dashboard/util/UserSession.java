package com.comp2211.dashboard.util;

import com.comp2211.dashboard.GUIStarter;
import com.comp2211.dashboard.model.data.Filter;
import java.util.ArrayList;
import java.util.List;
import com.comp2211.dashboard.Campaign;

public class UserSession {
  protected static List<Campaign> campaigns = new ArrayList<>();
  protected static boolean fullAccess = false;
  protected static String username = "";
  protected static boolean valid = false;

  /**
   * Stores information about the user session
   * @param dbName the name of the user
   * @param dbCampaigns the list of campaigns the user will be able to access
   * @param dbAccess whether the user has full access
   */
  public static void initializeSession(String dbName, String dbCampaigns, boolean dbAccess) {
    username = dbName;
    fullAccess = dbAccess;
    campaigns.clear();
    for(String s : dbCampaigns.split(";")) {
      int campaignID = Integer.parseInt(s);
      Campaign c = new Campaign(campaignID, GUIStarter.getDatabaseManager().retrieveCampaignName(campaignID), GUIStarter.getDatabaseManager());
      c.cacheData(new Filter(campaignID));
      if (c != null) {
        campaigns.add(c);
      }
    }
    if (!campaigns.isEmpty()) {
      valid = true;
    }
  }

  public static String getUsername() {
    return username;
  }

  public static List<Campaign> getAllowedCampaigns() {
    return campaigns;
  }

  public static boolean hasFullAccess() {
    return fullAccess;
  }

  public static void clearSession() {
    username = "";
    campaigns.clear();
    fullAccess = false;
    valid = false;
  }

  public static boolean isValid() {
    return valid;
  }
}
