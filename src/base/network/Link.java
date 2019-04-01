package base.network;


import java.io.Serializable;

/**
 * Created by ly on 2017/2/28.
 */
public class Link implements Serializable{
    private int linkId; //链路编号
    private int sourceId;   //链路起始节点ID
    private int sinkId;     //链路目的节点ID
    private int bandwidth;   //链路带宽
    private int unitCost;   //单元成本
    private int delay;  //延时
    private float reliability;  //链路可靠性

    private int bandWidthResourceDemand;    //链路带宽需求

    public Link() {

    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getLinkId() {
        return linkId;
    }

    public void setLinkId(int linkId) {
        this.linkId = linkId;
    }

    public int getSinkId() {
        return sinkId;
    }

    public void setSinkId(int sinkId) {
        this.sinkId = sinkId;
    }

    public int getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(int unitCost) {
        this.unitCost = unitCost;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getBandWidthResourceDemand() {
        return bandWidthResourceDemand;
    }

    public void setBandWidthResourceDemand(int bandWidthResourceDemand) {
        this.bandWidthResourceDemand = bandWidthResourceDemand;
    }

    public float getReliability() {
        return reliability;
    }

    public void setReliability(float reliability) {
        this.reliability = reliability;
    }
}
