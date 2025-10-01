package com.sap.cap.esmapi.hana.config.srv.impl;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.hana.config.srv.intf.IF_StatTransSrv;
import com.sap.cap.esmapi.status.pojos.TY_PortalStatusTransI;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.db.esmlogs.Statustrans;
import cds.gen.db.esmlogs.Statustrans_;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CL_StatTransSrv implements IF_StatTransSrv
{

    private final PersistenceService ps;
    private final MessageSource msgSrc;

    @Override
    public List<TY_PortalStatusTransI> getStatus4CaseType(String caseType) throws EX_ESMAPI
    {
        List<TY_PortalStatusTransI> retCfgs = null;

        List<Statustrans> configs = null;

        if (StringUtils.hasText(caseType))
        {
            CqnSelect qConfigsAll = Select.from(Statustrans_.class).where(s -> s.casetype().eq(caseType));
            configs = ps.run(qConfigsAll).listOf(Statustrans.class);
            if (CollectionUtils.isNotEmpty(configs))
            {
                // STAT_TRANS_LOADED= Status Transitions loaded for Case Type - {0}. # Configs -
                // {1}!
                msgSrc.getMessage("STAT_TRANS_LOADED", new Object[]
                { caseType, configs.size() }, Locale.getDefault());
                retCfgs = configs
                        .stream().map(e -> new TY_PortalStatusTransI(e.getCasetype(), e.getFromStatus(),
                                e.getToStatus(), e.getCaseEditAllowed(), e.getConfirmAllowed()))
                        .collect(Collectors.toList());
            }
        }
        return retCfgs;
    }

}
