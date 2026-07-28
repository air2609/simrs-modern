package com.vone.simrs.laborat;

/**
 * Response setelah menyimpan hasil lab (SC0043).
 */
public class LaboratResultSaveResultResponse {

    private final Integer resultId;
    private final String resultCode;
    private final String message;

    public LaboratResultSaveResultResponse(Integer resultId, String resultCode, String message) {
        this.resultId = resultId;
        this.resultCode = resultCode;
        this.message = message;
    }

    public Integer getResultId() { return resultId; }
    public String getResultCode() { return resultCode; }
    public String getMessage() { return message; }
}
