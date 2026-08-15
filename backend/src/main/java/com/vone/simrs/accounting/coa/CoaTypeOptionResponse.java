package com.vone.simrs.accounting.coa;

/**
 * Opsi tipe account COA (SCM0046). Mengikuti entity legacy
 * {@code MsCoaType} (tabel ms_coa_type).
 */
public class CoaTypeOptionResponse {

    private final Integer typeId;
    private final String typeName;
    private final Integer naturalBalance;

    public CoaTypeOptionResponse(Integer typeId, String typeName, Integer naturalBalance) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.naturalBalance = naturalBalance;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public Integer getNaturalBalance() {
        return naturalBalance;
    }
}
