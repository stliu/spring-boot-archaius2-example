package com.example.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author stliu at apache.org
 * @since 3/20/16
 */
@Entity
@Table(name = "properties")
@Data
public class Props {
    @Id
    private String name;
    private String value;
}
