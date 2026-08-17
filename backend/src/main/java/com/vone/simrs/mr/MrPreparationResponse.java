package com.vone.simrs.mr;

import java.util.List;

/**
 * Response untuk screen SC0006 (PERSIAPAN DOKUMEN REKAM MEDIS).
 */
public class MrPreparationResponse {

    private final List<MrPreparationItemResponse> notReadyList;
    private final List<MrPreparationItemResponse> readyList;

    public MrPreparationResponse(List<MrPreparationItemResponse> notReadyList,
            List<MrPreparationItemResponse> readyList) {
        this.notReadyList = notReadyList;
        this.readyList = readyList;
    }

    public List<MrPreparationItemResponse> getNotReadyList() {
        return notReadyList;
    }

    public List<MrPreparationItemResponse> getReadyList() {
        return readyList;
    }
}
