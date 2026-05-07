package com.company.crms.payment.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultVO implements Serializable {
    private int total;
    private int success;
    private int failed;
    private List<RowError> errors = new ArrayList<>();

    @Data
    public static class RowError {
        private int row;
        private String message;

        public RowError(int row, String message) {
            this.row = row;
            this.message = message;
        }
    }
}
