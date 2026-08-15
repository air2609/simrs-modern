package com.vone.simrs.accounting.coa;

import java.util.List;

/**
 * Baris data COA (SCM0046 - CHART OF ACCOUNT).
 * Mengikuti entity legacy {@code MsCoa} (tabel ms_coa).
 * Mendukung struktur tree parent-child via {@code children}.
 */
public class CoaRowResponse {

    private final Integer coaId;
    private final Integer supCoaId;
    private final Integer typeId;
    private final String typeName;
    private final String acctNo;
    private final String acctName;
    private final String desc;
    private final Double balance;
    private final Integer naturalBalance;
    private final Integer status;
    private final String statusLabel;
    private final List<CoaRowResponse> children;

    public CoaRowResponse(Integer coaId, Integer supCoaId, Integer typeId, String typeName,
            String acctNo, String acctName, String desc, Double balance,
            Integer naturalBalance, Integer status, String statusLabel,
            List<CoaRowResponse> children) {
        this.coaId = coaId;
        this.supCoaId = supCoaId;
        this.typeId = typeId;
        this.typeName = typeName;
        this.acctNo = acctNo;
        this.acctName = acctName;
        this.desc = desc;
        this.balance = balance;
        this.naturalBalance = naturalBalance;
        this.status = status;
        this.statusLabel = statusLabel;
        this.children = children;
    }

    public Integer getCoaId() {
        return coaId;
    }

    public Integer getSupCoaId() {
        return supCoaId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getAcctNo() {
        return acctNo;
    }

    public String getAcctName() {
        return acctName;
    }

    public String getDesc() {
        return desc;
    }

    public Double getBalance() {
        return balance;
    }

    public Integer getNaturalBalance() {
        return naturalBalance;
    }

    public Integer getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public List<CoaRowResponse> getChildren() {
        return children;
    }
}
