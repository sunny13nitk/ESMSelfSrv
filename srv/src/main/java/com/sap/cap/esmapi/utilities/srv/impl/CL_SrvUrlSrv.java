package com.sap.cap.esmapi.utilities.srv.impl;

import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SrvUrlSrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.exception.DestinationAccessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("SVY_DEFAULT")
@RequiredArgsConstructor
@Slf4j
public class CL_SrvUrlSrv implements IF_SrvUrlSrv
{

    private final IF_UserSessionSrv userSessSrv;

    private final MessageSource msgSrc;

    @SuppressWarnings("null")
    @Override
    public String getSrvUrl(String lob, String[] params, String... destinationurl) throws EX_ESMAPI
    {
        String svyUrl = null;

        if (StringUtils.hasText(lob) && CollectionUtils.isNotEmpty(Arrays.asList(params)) && userSessSrv != null)
        {

            if (CollectionUtils.isNotEmpty(Arrays.asList(destinationurl)))
            {
                log.info("destinationurl Passed as Parameter from session: " + Arrays.toString(destinationurl));
                svyUrl = destinationurl[0];
            }
            else
            {
                // Get the configuration for LoB
                TY_CatgCusItem cusI = userSessSrv.getCurrentLOBConfig();
                String des = (cusI != null && StringUtils.hasText(cusI.getSvydes())) ? cusI.getSvydes()
                        : GC_Constants.gc_SVY_DESTINATION_DEFAULT;
                log.info("Using Destination for Survey URL : " + des);
                if (StringUtils.hasText(des))
                {
                    try
                    {

                        log.info("Scanning for Destination : " + des);
                        Destination dest = DestinationAccessor.getDestination(des);
                        if (dest != null)
                        {

                            log.info("Qualtrics Destination Bound via Destination Accessor.");

                            for (String prop : dest.getPropertyNames())
                            {

                                if (prop.equals(GC_Constants.gc_prop_URL))
                                {
                                    svyUrl = dest.get(prop).get().toString();
                                    userSessSrv.setSvyUrl(svyUrl); // Load in Session Memory for
                                                                   // later
                                                                   // Use
                                }

                            }

                        }
                    }
                    catch (DestinationAccessException e)
                    {
                        log.error("Error Accessing Destination : " + e.getLocalizedMessage());
                        String msg = msgSrc.getMessage("ERR_DESTINATION_ACCESS", new Object[]
                        { cusI.getSvydes(), e.getLocalizedMessage() }, Locale.ENGLISH);
                        throw new EX_ESMAPI(msg);

                    }
                }

            }

            for (String param : params)
            {
                if (StringUtils.hasText(param) && StringUtils.hasText(svyUrl))
                {
                    svyUrl = svyUrl.replace(GC_Constants.gc_cons_pattn, param);
                }
            }

        }
        return svyUrl;
    }

}
