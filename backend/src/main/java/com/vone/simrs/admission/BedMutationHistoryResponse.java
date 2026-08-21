package com.vone.simrs.admission;

/**
 * Baris riwayat mutasi bed (daftar history mutasi pasien). Migrasi dari legacy
 * {@code MutasiKamarManagerImpl.getHistoryOfBedMove()} + {@code TbBedOccupancyDAO.getHistoryMove()}.
 */
public class BedMutationHistoryResponse {

    private final String createdDate;   // d_whn_create (kunci untuk mode ubah)
    private final Integer bedId;
    private final String bedDesc;       // bed asal
    private final Integer hallId;
    private final String hallName;      // ruangan
    private final Integer classId;      // kelas tarif
    private final String checkInTime;   // tanggal masuk
    private final String checkOutTime;  // tanggal keluar (null = bed aktif)
    private final String duration;      // durasi ("- " bila masih aktif)

    public BedMutationHistoryResponse(String createdDate, Integer bedId, String bedDesc,
            Integer hallId, String hallName, Integer classId, String checkInTime,
            String checkOutTime, String duration) {
        this.createdDate = createdDate;
        this.bedId = bedId;
        this.bedDesc = bedDesc;
        this.hallId = hallId;
        this.hallName = hallName;
        this.classId = classId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.duration = duration;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public Integer getBedId() {
        return bedId;
    }

    public String getBedDesc() {
        return bedDesc;
    }

    public Integer getHallId() {
        return hallId;
    }

    public String getHallName() {
        return hallName;
    }

    public Integer getClassId() {
        return classId;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public String getDuration() {
        return duration;
    }
}
