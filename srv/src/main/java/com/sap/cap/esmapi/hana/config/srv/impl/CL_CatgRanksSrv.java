package com.sap.cap.esmapi.hana.config.srv.impl;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.sap.cap.esmapi.catg.pojos.TY_CatgRanks;
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
    public TY_CatgRanks getAllCatgRanks()
    {
        List<Lobcatgsranks> configs = null;

        CqnSelect qConfigsAll = Select.from(Lobcatgsranks_.class);
        configs = ps.run(qConfigsAll).listOf(Lobcatgsranks.class);

        if (configs == null || configs.isEmpty())
        {
            return null;
        }
        else
        {
            // return new TY_CatgRanks(configs.stream()
            // .map(cfg -> new TY_CatgRanksItem(cfg.getCasetype(), cfg.getCatg(), (int)
            // cfg.getRank())).toList());

            return null;
        }
    }

}
