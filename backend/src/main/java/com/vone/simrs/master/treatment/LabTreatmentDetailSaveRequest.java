package com.vone.simrs.master.treatment;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class LabTreatmentDetailSaveRequest {

    @NotNull(message = "treatmentId wajib diisi")
    private Integer treatmentId;

    @NotBlank(message = "Nama detail / jenis pemeriksaan wajib diisi")
    private String detailName;

    @NotBlank(message = "Satuan / quantify wajib diisi")
    private String quantify;

    @NotBlank(message = "Normal range wajib diisi")
    private String normalRange;

    public Integer getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(Integer treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getDetailName() {
        return detailName;
    }

    public void setDetailName(String detailName) {
        this.detailName = detailName;
    }

    public String getQuantify() {
        return quantify;
    }

    public void setQuantify(String quantify) {
        this.quantify = quantify;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }
}
