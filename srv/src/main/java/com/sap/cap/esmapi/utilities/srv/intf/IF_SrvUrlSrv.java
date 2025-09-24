package com.sap.cap.esmapi.utilities.srv.intf;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

public interface IF_SrvUrlSrv
{
    public String getSrvUrl(String lob, String[] params, String... destinationurl) throws EX_ESMAPI;
}
