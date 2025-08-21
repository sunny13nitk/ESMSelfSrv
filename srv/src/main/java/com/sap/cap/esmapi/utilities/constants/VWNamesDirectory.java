package com.sap.cap.esmapi.utilities.constants;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.ui.pojos.TY_ViewMappings;
import com.sap.cap.esmapi.utilities.enums.EnumVWNames;

public class VWNamesDirectory
{
    public static final String gc_redirect = "redirect:";
    public static final String gc_paramPattern = "{";
    public static final String gc_paramSplit = "\\{";

    public static ArrayList<TY_ViewMappings> getViewsMap()
    {
        ArrayList<TY_ViewMappings> viewsDescMap = new ArrayList<TY_ViewMappings>();

        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.inbox, "/ess/{lob}", "essCasesListView"));
        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.caseForm, "/ess/createCase/}", "essCaseForm"));
        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.caseFormError, "/ess/errForm/", "essCaseForm"));
        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.error, "/err/exception?message={msg}", "error"));
        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.caseReply, "/ess/caseDetails/{caseID}", "essCaseFormReply"));

        return viewsDescMap;
    }

    public static String getViewName(EnumVWNames vwTag, boolean isRedirect, String... params)
    {
        String vwName = null;
        if (StringUtils.hasText(vwTag.toString()))
        {
            Optional<TY_ViewMappings> vwO = getViewsMap().stream()
                    .filter(v -> v.getVwTag().toString().equals(vwTag.toString())).findFirst();
            {
                if (vwO.isPresent())
                {
                    if (isRedirect)
                    {
                        if (vwO.get().getPath().contains(gc_paramPattern))
                        {
                            String[] pathParts = vwO.get().getPath().split(gc_paramSplit);

                            vwName = gc_redirect + pathParts[0];
                            for (int i = 0; i < params.length; i++)
                            {
                                if (params.length > 0)
                                {
                                    vwName = vwName + params[i];
                                }
                            }
                        }
                        else
                        {
                            vwName = gc_redirect + vwO.get().getPath();
                        }

                    }
                    else
                    {
                        vwName = vwO.get().getVwName();
                    }
                }
            }

        }

        return vwName;
    }
}
