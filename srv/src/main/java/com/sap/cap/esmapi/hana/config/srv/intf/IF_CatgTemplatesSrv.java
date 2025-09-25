package com.sap.cap.esmapi.hana.config.srv.intf;

import java.util.List;

import cds.gen.db.esmlogs.Catgtemplates;

public interface IF_CatgTemplatesSrv
{
    public List<Catgtemplates> getAllTemplates();
}
