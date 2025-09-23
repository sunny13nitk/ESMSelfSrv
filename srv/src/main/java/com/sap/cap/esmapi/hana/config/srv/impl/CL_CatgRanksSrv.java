package com.sap.cap.esmapi.hana.config.srv.impl;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgRanks;
import com.sap.cap.esmapi.catg.pojos.TY_CatgRanksItem;
import com.sap.cap.esmapi.exceptions.EX_CONFIG;
import com.sap.cap.esmapi.hana.config.srv.intf.IF_CatgRanksSrv;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.db.esmlogs.Lobcatgsranks;
import cds.gen.db.esmlogs.Lobcatgsranks_;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_CatgRanksSrv implements IF_CatgRanksSrv
{

    private final PersistenceService ps;
    private final MessageSource msgSrc;

    @Override
    public TY_CatgRanks getAllCatgRanks4Lob(String caseType) throws EX_CONFIG
    {
        List<Lobcatgsranks> configs = null;
        if (StringUtils.hasText(caseType))
        {
            CqnSelect qConfigsByLob = Select.from(Lobcatgsranks_.class).where(b -> b.casetype().eq(caseType));
            configs = ps.run(qConfigsByLob).listOf(Lobcatgsranks.class);

            if (configs == null || configs.isEmpty())
            {
                // ERR_NO_LOB_CFG= No LOB Configuration found for Scenario - {0} in Customizing.
                // Please contact your system administrator.
                throw new EX_CONFIG(msgSrc.getMessage("ERR_NO_LOB_CFG", new String[]
                { caseType }, null));
            }
            else
            {
                return new TY_CatgRanks(configs.stream()
                        .map(cfg -> new TY_CatgRanksItem(cfg.getCasetype(), cfg.getCatg(), (int) cfg.getRank()))
                        .toList());

            }
        }
        return null;

    }

}
