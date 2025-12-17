package cc.nocturne.eventsmadeeasy;


import cc.nocturne.eventsmadeeasy.model.EventBoard;

import java.util.List;

/**
 * POJO for the JSON coming back from the Apps Script / Registry backend.
 */
public class EventConfigResponse
{
    /** true if the operation succeeded */
    public boolean success;

    /** error message when success == false */
    public String error;

    /** event data when success == true (may be null on some operations) */
    public EventData event;

    // --- Nested classes ----------------------------------------------------

    public static class EventData
    {
        public String eventCode;

        public String sheetWebhookUrl;
        public String discordWebhookUrl;
        public String bannerImageUrl;
        public String soundUrl;

        public List<EventItem> items;

        // ✅ NEW: boards (optional; may be null/empty)
        public List<EventBoard> boards;
    }

    public static class EventItem
    {
        public Integer itemId;   // OSRS item ID
        public String  itemName; // human readable name (optional)
    }
}
