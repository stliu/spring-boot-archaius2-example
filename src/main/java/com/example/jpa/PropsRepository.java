package com.example.jpa;

import com.example.entity.Props;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author stliu at apache.org
 * @since 3/20/16
 */
@Repository
public interface PropsRepository extends JpaRepository<Props, String> {
}
