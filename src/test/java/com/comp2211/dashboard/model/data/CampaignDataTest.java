package com.comp2211.dashboard.model.data;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class CampaignDataTest {

  private static final String TEST_ID = "test-id";

  private CampaignData campaignData;

  @BeforeEach
  void setUp() {
    this.campaignData = new CampaignData(TEST_ID);
  }

  @AfterEach
  void tearDown() {
    this.campaignData = null;
    assertNull(this.campaignData);
  }

  @DisplayName("Test CampaignData.getId()")
  @Test
  void getId() {
    assertEquals(this.campaignData.getId(), TEST_ID);
  }
}