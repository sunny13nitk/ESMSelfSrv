package com.sap.cap.esmapi.status.pojos;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_StatusCfg
{
    private String caseType;
    private List<TY_StatusCfgItem> userStatusAssignments = new ArrayList<TY_StatusCfgItem>();
}
