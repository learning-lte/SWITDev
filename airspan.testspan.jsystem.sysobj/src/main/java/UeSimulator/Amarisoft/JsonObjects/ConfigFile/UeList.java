
package UeSimulator.Amarisoft.JsonObjects.ConfigFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "as_release",
    "ue_category",
    "sim_algo",
    "half_duplex",
    "forced_cqi",
    "forced_ri",
    "imsi",
    "K",
    "op",
    "tun_setup_script",
    "sim_events"
})
public class UeList {

	@JsonProperty("ue_id")
    private Integer ueId;
    @JsonProperty("as_release")
    private Integer asRelease;
    @JsonProperty("ue_category")
    private Integer ueCategory;
    @JsonProperty("sim_algo")
    private String simAlgo;
    @JsonProperty("half_duplex")
    private Boolean halfDuplex;
    @JsonProperty("forced_cqi")
    private Integer forcedCqi;
    @JsonProperty("forced_ri")
    private Integer forcedRi;
    @JsonProperty("imeisv")
	private String imeisv;
    @JsonProperty("imsi")
    private String imsi;
    @JsonProperty("K")
    private String k;
    @JsonProperty("op")
    private String op;
    @JsonProperty("tun_setup_script")
    private String tunSetupScript;
    @JsonProperty("sim_events")
    private List<SimEvent> simEvents = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    //ue_id

    @JsonProperty("ue_id")
    public Integer getUeId() {
        return ueId;
    }

    @JsonProperty("ue_id")
    public void setUeId(Integer ueId) {
        this.ueId = ueId;
    }
    
    @JsonProperty("as_release")
    public Integer getAsRelease() {
        return asRelease;
    }

    @JsonProperty("as_release")
    public void setAsRelease(Integer asRelease) {
        this.asRelease = asRelease;
    }

    @JsonProperty("ue_category")
    public Integer getUeCategory() {
        return ueCategory;
    }

    @JsonProperty("ue_category")
    public void setUeCategory(Integer ueCategory) {
        this.ueCategory = ueCategory;
    }

    @JsonProperty("sim_algo")
    public String getSimAlgo() {
        return simAlgo;
    }

    @JsonProperty("sim_algo")
    public void setSimAlgo(String simAlgo) {
        this.simAlgo = simAlgo;
    }

    @JsonProperty("half_duplex")
    public Boolean getHalfDuplex() {
        return halfDuplex;
    }

    @JsonProperty("half_duplex")
    public void setHalfDuplex(Boolean halfDuplex) {
        this.halfDuplex = halfDuplex;
    }

    @JsonProperty("forced_cqi")
    public Integer getForcedCqi() {
        return forcedCqi;
    }

    @JsonProperty("forced_cqi")
    public void setForcedCqi(Integer forcedCqi) {
        this.forcedCqi = forcedCqi;
    }

    @JsonProperty("forced_ri")
    public Integer getForcedRi() {
        return forcedRi;
    }

    @JsonProperty("forced_ri")
    public void setForcedRi(Integer forcedRi) {
        this.forcedRi = forcedRi;
    }
    
    @JsonProperty("imeisv")
    public String getImeisv() {
    	return imeisv;
	}
    
    @JsonProperty("imeisv")
    public void setImeisv(String imeisv) {
    	this.imeisv = imeisv;
	}

    @JsonProperty("imsi")
    public String getImsi() {
        return imsi;
    }

    @JsonProperty("imsi")
    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    @JsonProperty("K")
    public String getK() {
        return k;
    }

    @JsonProperty("K")
    public void setK(String k) {
        this.k = k;
    }

    @JsonProperty("op")
    public String getOp() {
        return op;
    }

    @JsonProperty("op")
    public void setOp(String op) {
        this.op = op;
    }

    @JsonProperty("tun_setup_script")
    public String getTunSetupScript() {
        return tunSetupScript;
    }

    @JsonProperty("tun_setup_script")
    public void setTunSetupScript(String tunSetupScript) {
        this.tunSetupScript = tunSetupScript;
    }

    @JsonProperty("sim_events")
    public List<SimEvent> getSimEvents() {
        return simEvents;
    }

    @JsonProperty("sim_events")
    public void setSimEvents(List<SimEvent> simEvents) {
        this.simEvents = simEvents;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

	

}