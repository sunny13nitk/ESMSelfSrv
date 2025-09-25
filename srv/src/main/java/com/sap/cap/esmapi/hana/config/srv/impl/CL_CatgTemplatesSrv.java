package com.sap.cap.esmapi.hana.config.srv.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sap.cap.esmapi.hana.config.srv.intf.IF_CatgTemplatesSrv;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.db.esmlogs.Catgtemplates;
import cds.gen.db.esmlogs.Catgtemplates_;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_CatgTemplatesSrv implements IF_CatgTemplatesSrv
{
    private final PersistenceService ps;

    @Override
    public List<Catgtemplates> getAllTemplates()
    {
        List<Catgtemplates> configs = null;

        CqnSelect qConfigsAll = Select.from(Catgtemplates_.class);
        configs = ps.run(qConfigsAll).listOf(Catgtemplates.class);
        log.info("Templates loaded from HDI ... : # " + configs.size());
        return configs;
    }
}
