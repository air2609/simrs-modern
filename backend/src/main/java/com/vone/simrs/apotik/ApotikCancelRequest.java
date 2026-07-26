package com.vone.simrs.apotik;

import javax.validation.constraints.NotBlank;

public class ApotikCancelRequest {

    @NotBlank
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
