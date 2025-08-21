package com.sap.cap.esmapi.utilities.constants;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.ui.pojos.TY_ViewMappings;
import com.sap.cap.esmapi.utilities.enums.EnumVWNames;

public class VWNamesDirectoryLocal
{
    public static final String gc_redirect = "redirect:";
    public static final String gc_paramPattern = "{";
    public static final String gc_paramSplit = "\\{";

    public static ArrayList<TY_ViewMappings> getViewsMap()
    {
        ArrayList<TY_ViewMappings> viewsDescMap = new ArrayList<TY_ViewMappings>();

        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.inbox, "/poclocal/{lob}", "essCasesListViewLocal"));
        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.caseFormError, "/poclocal/errForm/", "esscaseFormLocal"));
        viewsDescMap.add(new TY_ViewMappings(EnumVWNames.caseForm, "/poclocal/createCase/}", "essCaseFormLocal"));

        viewsDescMap.add(
                new TY_ViewMappings(EnumVWNames.caseReply, "/poclocal/caseDetails/{caseID}", "esscaseFormReplyLocal"));

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
