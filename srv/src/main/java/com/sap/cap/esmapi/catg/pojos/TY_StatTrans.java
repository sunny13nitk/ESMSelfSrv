package com.sap.cap.esmapi.catg.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TY_StatTrans
{
    private String casetype;
    private String fromStatus;
    private String toStatus;
    private boolean caseEditAllowed;
    private boolean confirmAllowed;

}