package com.api.v1.inventory.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportedFile {
    private String fileName;
    private String contentType;
    private byte[] data;
}
