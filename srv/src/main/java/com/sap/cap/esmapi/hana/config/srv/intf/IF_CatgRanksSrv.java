package com.sap.cap.esmapi.hana.config.srv.intf;

import com.sap.cap.esmapi.catg.pojos.TY_CatgRanks;
import com.sap.cap.esmapi.exceptions.EX_CONFIG;

public interface IF_CatgRanksSrv
{

    public TY_CatgRanks getAllCatgRanks4Lob(String lob) throws EX_CONFIG;

}
