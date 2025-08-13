package cn.isliu.core;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Sheet {

    @SerializedName("sheet_id")
    private String sheetId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("index")
    private int index;
    
    @SerializedName("hidden")
    private boolean hidden;
    
    @SerializedName("grid_properties")
    private GridProperties gridProperties;
    
    @SerializedName("resource_type")
    private String resourceType;
    
    @SerializedName("merges")
    private List<Merge> merges;

    public Sheet() {
    }

    public Sheet(String sheetId, String title, int index, boolean hidden, GridProperties gridProperties, String resourceType, List<Merge> merges) {
        this.sheetId = sheetId;
        this.title = title;
        this.index = index;
        this.hidden = hidden;
        this.gridProperties = gridProperties;
        this.resourceType = resourceType;
        this.merges = merges;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getTitle() {
        return title;
    }

    public int getIndex() {
        return index;
    }

    public boolean isHidden() {
        return hidden;
    }

    public GridProperties getGridProperties() {
        return gridProperties;
    }

    public String getResourceType() {
        return resourceType;
    }

    public List<Merge> getMerges() {
        return merges;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setGridProperties(GridProperties gridProperties) {
        this.gridProperties = gridProperties;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setMerges(List<Merge> merges) {
        this.merges = merges;
    }
}