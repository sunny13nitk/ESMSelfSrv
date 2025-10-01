package com.sap.cap.esmapi.hana.config.srv.intf;

import java.util.List;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransI;

public interface IF_StatTransSrv
{

    public List<TY_PortalStatusTransI> getStatus4CaseType(String caseType) throws EX_ESMAPI;
}
