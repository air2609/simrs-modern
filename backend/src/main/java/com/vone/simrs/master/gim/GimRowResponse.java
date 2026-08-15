package com.vone.simrs.master.gim;

/**
 * Baris data general information (SCM0047). Mengikuti entity legacy
 * {@code MsGim} (tabel ms_gim).
 */
public class GimRowResponse {

    private final Integer id;
    private final String key;
    private final String value;

    public GimRowResponse(Integer id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
