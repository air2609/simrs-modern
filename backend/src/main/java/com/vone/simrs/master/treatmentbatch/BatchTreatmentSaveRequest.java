package com.vone.simrs.master.treatmentbatch;

import java.util.List;

/**
 * Request simpan batch untuk screen SCM0056 (UPDATE MASTER TINDAKAN).
 * Mengikuti logika legacy {@code TreatmentManagerImpl.updateTreatmentFee()}
 * yang menerima daftar baris listbox (kode, nama, kelas tarif, jasa RS,
 * jasa dokter, jasa medik, total biaya, no. COA).
 */
public class BatchTreatmentSaveRequest {

    private List<BatchTreatmentItem> items;

    public List<BatchTreatmentItem> getItems() {
        return items;
    }

    public void setItems(List<BatchTreatmentItem> items) {
        this.items = items;
    }

    /**
     * Satu baris batch treatment. Field mengikuti kolom listbox legacy.
     */
    public static class BatchTreatmentItem {
        private String code;
        private String name;
        private String treatmentClassDesc;
        private Double hospitalFee;
        private Double doctorFee;
        private Double medicFee;
        private Double totalFee;
        private String coaNo;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTreatmentClassDesc() {
            return treatmentClassDesc;
        }

        public void setTreatmentClassDesc(String treatmentClassDesc) {
            this.treatmentClassDesc = treatmentClassDesc;
        }

        public Double getHospitalFee() {
            return hospitalFee;
        }

        public void setHospitalFee(Double hospitalFee) {
            this.hospitalFee = hospitalFee;
        }

        public Double getDoctorFee() {
            return doctorFee;
        }

        public void setDoctorFee(Double doctorFee) {
            this.doctorFee = doctorFee;
        }

        public Double getMedicFee() {
            return medicFee;
        }

        public void setMedicFee(Double medicFee) {
            this.medicFee = medicFee;
        }

        public Double getTotalFee() {
            return totalFee;
        }

        public void setTotalFee(Double totalFee) {
            this.totalFee = totalFee;
        }

        public String getCoaNo() {
            return coaNo;
        }

        public void setCoaNo(String coaNo) {
            this.coaNo = coaNo;
        }
    }
}
