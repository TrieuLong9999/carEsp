package com.control.espcar.repository;

import com.control.espcar.entity.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceInfoRepository extends JpaRepository<DeviceInfo, Long> {
    Optional<DeviceInfo> findBySerialNumber(String serialNumber);
}
