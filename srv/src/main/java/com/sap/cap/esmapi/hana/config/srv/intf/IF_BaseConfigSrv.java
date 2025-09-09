package com.sap.cap.esmapi.hana.config.srv.intf;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.exceptions.EX_CONFIG;

public interface IF_BaseConfigSrv
{
    public TY_CatgCus getAllBaseConfigs();

    public TY_CatgCusItem getConfigByLoB(String lob) throws EX_CONFIG;

    public TY_CatgCusItem getConfigByCaseType(String caseType) throws EX_CONFIG;

}
