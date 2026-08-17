package com.vone.simrs.accounting;

import java.util.List;

/**
 * Respons daftar ACCOUNT PAYABLE dengan paging (SC0196). Halaman berukuran
 * 20 baris dengan pencarian berdasarkan nama supplier / journal batch id.
 */
public class AccountPayablePageResponse {

    private final List<AccountPayableRowResponse> rows;
    private final long total;
    private final int page;
    private final int pageSize;
    private final int totalPages;

    public AccountPayablePageResponse(List<AccountPayableRowResponse> rows, long total, int page,
            int pageSize, int totalPages) {
        this.rows = rows;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    public List<AccountPayableRowResponse> getRows() {
        return rows;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
