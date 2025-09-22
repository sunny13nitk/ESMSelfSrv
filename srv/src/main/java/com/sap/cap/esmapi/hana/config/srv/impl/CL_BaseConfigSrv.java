package com.sap.cap.esmapi.hana.config.srv.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.exceptions.EX_CONFIG;
import com.sap.cap.esmapi.hana.config.srv.intf.IF_BaseConfigSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.db.esmlogs.Baseconfig;
import cds.gen.db.esmlogs.Baseconfig_;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_BaseConfigSrv implements IF_BaseConfigSrv
{

    private final PersistenceService ps;
    private final MessageSource msgSrc;
    private final IF_UserSessionSrv userSessSrv;

    @Override
    public TY_CatgCus getAllBaseConfigs()
    {

        List<Baseconfig> configs = null;

        CqnSelect qConfigsAll = Select.from(Baseconfig_.class);
        configs = ps.run(qConfigsAll).listOf(Baseconfig.class);
        if (configs == null || configs.isEmpty())
        {
            return null;
        }
        else
        {
            return new TY_CatgCus(configs.stream()
                    .map(cfg -> new TY_CatgCusItem(cfg.getCaseTypeEnum(), cfg.getCasetype(), cfg.getAppNotesTypes(),
                            cfg.getStatusSchema(), cfg.getReplyNoteType(), cfg.getCatgRankEnabled(),
                            cfg.getCatgsranksonlyShow(), cfg.getConfirmStatus(), cfg.getFragmentHead(),
                            cfg.getFragmentTitle(), cfg.getFragmentFooter(), cfg.getCaseFormView(), cfg.getLogouturl()))
                    .toList());
        }
    }

    @Override
    public TY_CatgCusItem getConfigByLoB(String lob) throws EX_CONFIG
    {

        if (StringUtils.hasText(lob) && ps != null)
        {
            CqnSelect qLobId = Select.from(Baseconfig_.class).where(q -> q.caseTypeEnum().eq(lob));
            if (qLobId != null)
            {
                log.info(lob + " config to be fetched. Inside getConfigByLoB routine");
                Optional<Baseconfig> cfgO = ps.run(qLobId).first(Baseconfig.class);
                if (cfgO.isPresent())
                {
                    return new TY_CatgCusItem(cfgO.get().getCaseTypeEnum(), cfgO.get().getCasetype(),
                            cfgO.get().getAppNotesTypes(), cfgO.get().getStatusSchema(), cfgO.get().getReplyNoteType(),
                            cfgO.get().getCatgRankEnabled(), cfgO.get().getCatgsranksonlyShow(),
                            cfgO.get().getConfirmStatus(), cfgO.get().getFragmentHead(), cfgO.get().getFragmentTitle(),
                            cfgO.get().getFragmentFooter(), cfgO.get().getCaseFormView(), cfgO.get().getLogouturl());
                }

            }

        }
        return null;
    }

    @Override
    public TY_CatgCusItem getConfigByCaseType(String caseType) throws EX_CONFIG
    {
        if (StringUtils.hasText(caseType) && ps != null)
        {
            CqnSelect qLobId = Select.from(Baseconfig_.class).where(q -> q.casetype().eq(caseType));
            if (qLobId != null)
            {
                log.info(caseType + " config to be fetched. Inside getConfigByCaseType routine");
                Optional<Baseconfig> cfgO = ps.run(qLobId).first(Baseconfig.class);
                if (cfgO.isPresent())
                {
                    return new TY_CatgCusItem(cfgO.get().getCaseTypeEnum(), cfgO.get().getCasetype(),
                            cfgO.get().getAppNotesTypes(), cfgO.get().getStatusSchema(), cfgO.get().getReplyNoteType(),
                            cfgO.get().getCatgRankEnabled(), cfgO.get().getCatgsranksonlyShow(),
                            cfgO.get().getConfirmStatus(), cfgO.get().getFragmentHead(), cfgO.get().getFragmentTitle(),
                            cfgO.get().getFragmentFooter(), cfgO.get().getCaseFormView(), cfgO.get().getLogouturl());
                }

            }

        }
        return null;
    }

}
