package base.graph;

/**
 * 虚拟网络的边
 * Created by ly on 2017/2/28.
 */
public class CEdge {
    private int edgeId; //边编号
    private int sourceId;   //边起始点编号
    private int sinkId; //边结束点编号
    private float delay;    //边延迟
    private float totalBandwithResource;    //边总的带宽资源
    private float remainBandWidthResource;  //边的剩余带宽资源

    private float reliability;  //可靠性

    private int associateLinkId;    //对应物理网络的边编号
    public float weight;    //边的权重

    public CEdge() {

    }

    public CEdge(int sourceId, int sinkId, int edgeId, float weight) {
        this.sourceId = sourceId;
        this.sinkId = sinkId;
        this.edgeId = edgeId;
        this.weight = weight;
//        this.associateLinkId = edgeId;在CGraph.initiate()里有设定
    }

    public float getRemainBandWidthResource() {
        return remainBandWidthResource;
    }

    public void setRemainBandWidthResource(float remainBandWidthResource) {
        this.remainBandWidthResource = remainBandWidthResource;
    }

    public int getEdgeId() {
        return edgeId;
    }
    
    public void setEdgeId(int edgeId) {
        this.edgeId = edgeId;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getSinkId() {
        return sinkId;
    }

    public void setSinkId(int sinkId) {
        this.sinkId = sinkId;
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float delay) {
        this.delay = delay;
    }

    public float getTotalBandwithResource() {
        return totalBandwithResource;
    }

    public void setTotalBandwithResource(float totalBandwithResource) {
        this.totalBandwithResource = totalBandwithResource;
    }

    public int getAssociateLinkId() {
        return associateLinkId;
    }

    public void setAssociateLinkId(int associateLinkId) {
        this.associateLinkId = associateLinkId;
    }

    public float getReliability() {
        return reliability;
    }

    public void setReliability(float reliability) {
        this.reliability = reliability;
    }
}
