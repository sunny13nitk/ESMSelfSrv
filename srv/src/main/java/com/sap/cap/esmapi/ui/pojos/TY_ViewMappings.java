package com.sap.cap.esmapi.ui.pojos;

import com.sap.cap.esmapi.utilities.enums.EnumVWNames;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_ViewMappings
{
    private EnumVWNames vwTag;
    private String path;
    private String vwName;
}
