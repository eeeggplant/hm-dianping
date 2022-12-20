package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucherMsg implements Serializable {
    private Long id;
    private Long userId;
    private Long voucherId;
}
