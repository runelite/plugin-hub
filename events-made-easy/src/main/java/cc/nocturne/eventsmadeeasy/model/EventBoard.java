package cc.nocturne.eventsmadeeasy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventBoard
{
    private int teamIndex;     // 1..16
    private String teamName;   // "Nocturne"
    private String spreadsheetId;
    private long gid;
    private String rangeA1;    // "A1:AH32"
}
