package com.example.telecom.repository;

import com.example.telecom.model.Device;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByImei(String imei);

    Optional<Device> findFirstByLineIdAndStatus(Long lineId, Device.Status status);

    boolean existsByLineId(Long lineId);

    long countByStatus(Device.Status status);

    @Query("SELECT d.status, COUNT(d) FROM Device d GROUP BY d.status ORDER BY d.status")
    List<Object[]> countGroupedByStatus();
}
