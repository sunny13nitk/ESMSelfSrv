package com.sap.cap.esmapi.ui.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgDetails;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.events.event.EV_CaseConfirmSubmit;
import com.sap.cap.esmapi.exceptions.EX_CaseAlreadyConfirmed;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.constants.VWNamesDirectory;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumVWNames;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cds.services.request.UserInfo;
import com.sap.cloud.security.token.Token;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/*
 * Main Controller Class
 */

@Slf4j
@Controller
@RequestMapping("/ess")
public class ESSController
{

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private TY_CatgCus catgCusSrv;

    @Autowired
    private IF_CatalogSrv catalogTreeSrv;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private IF_CatalogSrv catalogSrv;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_SessAttachmentsService attSrv;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    private final String caseConfirmError = "alreadyConfirmed";

    @GetMapping("/{lob}")
    @PreAuthorize("hasAnyAuthority('" + GC_Constants.gc_role_employee_esm + "', '" + GC_Constants.gc_role_contractor_esm
            + "')")
    public String showCasesList4User(@AuthenticationPrincipal Token token, @PathVariable(name = "lob") String lob,
            Model model)
    {
        String vw = null;
        log.info(lob);

        try
        {
            if (userInfo != null && userInfo.isAuthenticated())
            {
                log.info("User Info: " + userInfo.getName() + " with lob :" + lob);
                log.info("authenticated user with lob :" + lob);
                if (userSessionSrv.isLobValid(lob))
                {
                    // Get the Customizations for the LOB
                    TY_CatgCusItem catgCusItem = userSessionSrv.getCurrentLOBConfig();
                    if (catgCusItem != null)
                    {
                        TY_UserESS userDetails = new TY_UserESS();
                        if (userSessionSrv.getESSDetails(token, true) != null)
                        {
                            if (userSessionSrv.getUserDetails4mSession() != null)
                            {

                                log.info("User Details Bound from Session!");
                                if (StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getAccountId())
                                        || StringUtils
                                                .hasText(userSessionSrv.getUserDetails4mSession().getEmployeeId()))
                                {
                                    // #View
                                    vw = VWNamesDirectory.getViewName(EnumVWNames.inbox, false, lob);

                                    userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                                    log.info("Fetching Cases for User From Session : "
                                            + userSessionSrv.getUserDetails4mSession().toString());
                                    userDetails.setCases(userSessionSrv.getCases4User4mSession());
                                    model.addAttribute("userInfo", userDetails);
                                    model.addAttribute("caseTypeStr", catgCusItem.getCaseTypeEnum().toString());
                                    // Rate Limit Simulation
                                    model.addAttribute("rateLimitBreached",
                                            userSessionSrv.getCurrentRateLimitBreachedValue());

                                    // Even if No Cases - spl. for Newly Create Acc - to enable REfresh button
                                    model.addAttribute("sessMsgs", userSessionSrv.getSessionMessages());

                                    // Session Active Toast
                                    model.addAttribute("submActive", userSessionSrv.isCurrentSubmissionActive());

                                    // TEmplates
                                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                                    model.addAttribute("dynamicFragmentHeader",
                                            (catgCusItem.getFragmentHead() != null
                                                    && !catgCusItem.getFragmentHead().trim().isBlank())
                                                            ? catgCusItem.getFragmentHead()
                                                            : GC_Constants.gc_HeaderFragmentDefault);
                                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                                    model.addAttribute("dynamicFragmentTitle",
                                            (catgCusItem.getFragmentTitle() != null
                                                    && !catgCusItem.getFragmentTitle().trim().isBlank())
                                                            ? catgCusItem.getFragmentTitle()
                                                            : GC_Constants.gc_TitleFragmentDefault);

                                    model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                                    model.addAttribute("dynamicFragmentFooter",
                                            (catgCusItem.getFragmentFooter() != null
                                                    && !catgCusItem.getFragmentFooter().trim().isBlank())
                                                            ? catgCusItem.getFragmentFooter()
                                                            : GC_Constants.gc_FooterFragmentDefault);

                                    // Survey processing enabled
                                    if (catgCusItem.getEnableSvy())
                                    {
                                        model.addAttribute("svyEnabled", true);
                                        log.info("Survey Processing Enabled for LoB: " + lob);
                                    }
                                    else
                                    {
                                        model.addAttribute("svyEnabled", false);
                                        log.info("Survey Processing Disabled for LoB: " + lob);

                                    }
                                }

                                else
                                {
                                    // ERR_NO_USR=No User customer/employee could be determined/created for user -
                                    // {0} post login!

                                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NO_USR", new Object[]
                                    { userSessionSrv.getUserDetails4mSession().getUserId() }, Locale.ENGLISH));
                                }

                            }

                        }

                    }
                    else
                    {
                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_NO_LOB_CFG", new Object[]
                        { lob }, Locale.ENGLISH));
                    }
                }
                else
                {
                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_NO_LOB_CFG", new Object[]
                    { lob }, Locale.ENGLISH));
                }

            }
            else
            {
                log.info("User Not Authenticated!");
                // UNAUTHENTICATED_ACCESS=User - {0} is not authenticated !
                throw new EX_ESMAPI(msgSrc.getMessage("UNAUTHENTICATED_ACCESS", new Object[]
                { userSessionSrv.getUserDetails4mSession().getUserId() }, Locale.ENGLISH));

            }
        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(e.getLocalizedMessage());
        }

        return vw != null ? vw
                : VWNamesDirectory.getViewName(EnumVWNames.error, false, msgSrc.getMessage("ERR_INBOX", new Object[]
                { userSessionSrv.getUserDetails4mSession().getUserId() }, Locale.ENGLISH));

    }

    @GetMapping("/createCase/")
    public String showCaseAsyncForm(Model model)
    {
        userSessionSrv.clearActiveSubmission();
        String viewCaseForm = VWNamesDirectory.getViewName(EnumVWNames.caseForm, false, (String[]) null);
        log.info("Case Form View : " + viewCaseForm);

        if ((StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()) && userInfo.isAuthenticated())
        {

            TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());
                log.info("Case Type : " + cusItem.getCaseTypeEnum().toString());

                // Before case form Inititation we must check the Rate Limit for the Current
                // User Session --current Form Submission added for Rate Limit Evaulation
                if (userSessionSrv.isWithinRateLimit())
                {
                    // REset Previous Catg
                    userSessionSrv.setPreviousCategory(null);

                    // Populate User Details
                    TY_UserESS userDetails = new TY_UserESS();
                    userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                    model.addAttribute("userInfo", userDetails);

                    // Initialize Attachments Session Service
                    if (attSrv != null)
                    {
                        attSrv.initialize();
                    }
                    // Populate Case Form Details
                    TY_Case_Form caseForm = new TY_Case_Form();
                    if (userSessionSrv.getUserDetails4mSession().isEmployee())
                    {
                        caseForm.setAccId(userSessionSrv.getUserDetails4mSession().getEmployeeId()); // hidden
                    }
                    else
                    {
                        caseForm.setAccId(userSessionSrv.getUserDetails4mSession().getAccountId()); // hidden
                    }

                    caseForm.setCaseTxnType(cusItem.getCaseType()); // hidden
                    caseForm.setLob(cusItem.getCaseTypeEnum().toString()); // hidden
                    model.addAttribute("caseForm", caseForm);

                    model.addAttribute("formErrors", null);

                    // clear Form errors on each refresh or a New Case form request
                    if (CollectionUtils.isNotEmpty(userSessionSrv.getFormErrors()))
                    {
                        userSessionSrv.clearFormErrors();

                    }

                    // also Upload the Catg. Tree as per Case Type
                    model.addAttribute("catgsList",
                            catalogTreeSrv.getCaseCatgTree4LoB(cusItem.getCaseTypeEnum()).getCategories());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                    model.addAttribute("dynamicFragmentHeader",
                            (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                    ? cusItem.getFragmentHead()
                                    : GC_Constants.gc_HeaderFragmentDefault);
                    log.info("Dynamic Header Fragment : " + model.getAttribute("dynamicFragmentHeader"));
                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                    model.addAttribute("dynamicFragmentTitle",
                            (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                    ? cusItem.getFragmentTitle()
                                    : GC_Constants.gc_TitleFragmentDefault);
                    log.info("Dynamic Title Fragment : " + model.getAttribute("dynamicFragmentTitle"));

                    model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                    model.addAttribute("dynamicFragmentFooter",
                            (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                    ? cusItem.getFragmentFooter()
                                    : GC_Constants.gc_FooterFragmentDefault);
                    log.info("Dynamic Footer Fragment : " + model.getAttribute("dynamicFragmentFooter"));

                    // Check if LoB Specific Case Form is configured
                    if (StringUtils.hasText(cusItem.getCaseFormView()))
                    {
                        viewCaseForm = cusItem.getCaseFormView();
                        log.info("Case Form View : " + viewCaseForm);
                    }

                }
                else
                {
                    // Not Within Rate Limit - REdirect to List View
                    viewCaseForm = VWNamesDirectory.getViewName(EnumVWNames.inbox, true,
                            cusItem.getCaseTypeEnum().toString());

                }

            }
            else
            {
                // Handled via central exception handler
                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
            }

        }

        return viewCaseForm;
    }

    @GetMapping("/errForm/")
    public String showErrorCaseForm(Model model)
    {
        String viewCaseForm = VWNamesDirectory.getViewName(EnumVWNames.caseFormError, false, (String[]) null);
        if ((StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations())
                && userSessionSrv.getCurrentForm4Submission() != null)
        {
            userSessionSrv.clearActiveSubmission();

            TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());
                viewCaseForm = (cusItem.getCaseFormView() != null && !cusItem.getCaseFormView().trim().isBlank())
                        ? cusItem.getCaseFormView()
                        : viewCaseForm;
                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Populate Case Form Details
                TY_Case_Form caseForm = new TY_Case_Form();
                if (userSessionSrv.getUserDetails4mSession().isEmployee())
                {
                    caseForm.setAccId(userSessionSrv.getUserDetails4mSession().getEmployeeId()); // hidden
                }
                else
                {
                    caseForm.setAccId(userSessionSrv.getUserDetails4mSession().getAccountId()); // hidden
                }

                caseForm.setCaseTxnType(cusItem.getCaseType()); // hidden
                caseForm.setLob(cusItem.getCaseTypeEnum().toString()); // hidden
                caseForm.setCatgDesc(userSessionSrv.getCurrentForm4Submission().getCaseForm().getCatgDesc()); // Curr
                                                                                                              // Catg
                caseForm.setDescription(userSessionSrv.getCurrentForm4Submission().getCaseForm().getDescription()); // Curr
                                                                                                                    // Notes
                caseForm.setSubject(userSessionSrv.getCurrentForm4Submission().getCaseForm().getSubject()); // Curr
                                                                                                            // Subject
                //// Affected User email
                if (StringUtils.hasText(userSessionSrv.getCurrentForm4Submission().getCaseForm().getAddEmail()))
                {
                    caseForm.setAddEmail(userSessionSrv.getCurrentForm4Submission().getCaseForm().getAddEmail());
                }

                model.addAttribute("formErrors", userSessionSrv.getFormErrors());

                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessionSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessionSrv.getMessageStack().stream()
                            .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                    if (!attErrO.isPresent())
                    {
                        // Attachment able to presist do not remove it from Current Payload
                        caseForm.setAttachment(
                                userSessionSrv.getCurrentForm4Submission().getCaseForm().getAttachment());

                    }
                }

                model.addAttribute("caseForm", caseForm);

                // also Upload the Catg. Tree as per Case Type
                model.addAttribute("catgsList",
                        catalogTreeSrv.getCaseCatgTree4LoB(cusItem.getCaseTypeEnum()).getCategories());

                if (attSrv != null)
                {
                    if (CollectionUtils.isNotEmpty(attSrv.getAttachmentNames()))
                    {
                        model.addAttribute("attachments", attSrv.getAttachmentNames());
                    }
                }

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                // View Name for Dynamic Template Header and Title
                model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                model.addAttribute("dynamicFragmentHeader",
                        (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                ? cusItem.getFragmentHead()
                                : GC_Constants.gc_HeaderFragmentDefault);
                model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                model.addAttribute("dynamicFragmentTitle",
                        (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                ? cusItem.getFragmentTitle()
                                : GC_Constants.gc_TitleFragmentDefault);

                model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                model.addAttribute("dynamicFragmentFooter",
                        (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                ? cusItem.getFragmentFooter()
                                : GC_Constants.gc_FooterFragmentDefault);

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
            }

        }

        return viewCaseForm;
    }

    @GetMapping("/removeAttachment/{fileName}")
    public String removeAttachmentCaseCreate(@PathVariable String fileName, Model model)
    {
        String viewCaseForm = VWNamesDirectory.getViewName(EnumVWNames.caseForm, false, (String[]) null);
        if (StringUtils.hasText(fileName) && attSrv != null && userSessionSrv != null)
        {
            userSessionSrv.clearActiveSubmission();
            if (attSrv.checkIFExists(fileName))
            {
                attSrv.removeAttachmentByName(fileName);
            }

            // Populate the view

            TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());
                viewCaseForm = (cusItem.getCaseFormView() != null && !cusItem.getCaseFormView().trim().isBlank())
                        ? cusItem.getCaseFormView()
                        : viewCaseForm;

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Get Case from Session Service

                TY_Case_Form caseForm = userSessionSrv.getCaseFormB4Submission();

                if (caseForm != null)
                {

                    if (userSessionSrv.getUserDetails4mSession().isEmployee())
                    {
                        caseForm.setEmployee(true);
                    }

                    // Clear form for New Attachment as Current Attachment already in Container
                    caseForm.setAttachment(null);

                    // Scan for Template Load
                    TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                            cusItem.getCaseTypeEnum());
                    if (catgTemplate != null)
                    {

                        // Set Questionnaire for Category
                        caseForm.setTemplate(catgTemplate.getQuestionnaire());

                    }

                    model.addAttribute("caseForm", caseForm);

                    // also Upload the Catg. Tree as per Case Type
                    model.addAttribute("catgsList",
                            catalogTreeSrv.getCaseCatgTree4LoB(cusItem.getCaseTypeEnum()).getCategories());

                    model.addAttribute("attachments", attSrv.getAttachmentNames());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                    // View Name for Dynamic Template Header and Title
                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                    model.addAttribute("dynamicFragmentHeader",
                            (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                    ? cusItem.getFragmentHead()
                                    : GC_Constants.gc_HeaderFragmentDefault);
                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                    model.addAttribute("dynamicFragmentTitle",
                            (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                    ? cusItem.getFragmentTitle()
                                    : GC_Constants.gc_TitleFragmentDefault);

                    model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                    model.addAttribute("dynamicFragmentFooter",
                            (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                    ? cusItem.getFragmentFooter()
                                    : GC_Constants.gc_FooterFragmentDefault);
                }

            }
        }

        return viewCaseForm;
    }

    @GetMapping("/caseReply/removeAttachment/{fileName}")
    public String removeAttachmentCaseReply(@PathVariable String fileName, Model model)
    {
        String caseFormReply = VWNamesDirectory.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        if (StringUtils.hasText(fileName) && attSrv != null && userSessionSrv != null)
        {
            userSessionSrv.clearActiveSubmission();
            if (attSrv.checkIFExists(fileName))
            {
                attSrv.removeAttachmentByName(fileName);
            }

            // Populate User Details
            TY_UserESS userDetails = new TY_UserESS();
            userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
            model.addAttribute("userInfo", userDetails);

            model.addAttribute("formErrors", userSessionSrv.getFormErrors());

            // Get Case Details
            TY_CaseEdit_Form caseEditForm = null;

            // Try to Get Case Edit Form from Upload Form from User Submit
            if (userSessionSrv.getCurrentReplyForm4Submission() != null)
            {
                caseEditForm = userSessionSrv.getCaseDetails4Edit(
                        userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getReply());
                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessionSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessionSrv.getMessageStack().stream()
                            .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                    if (attErrO.isPresent())
                    {
                        // Attachment able to presist do not remove it from Current Payload
                        caseEditForm.setAttachment(null);

                    }
                }
            }
            else
            {
                caseEditForm = userSessionSrv.getCaseDetails4Edit(
                        userSessionSrv.getCaseEditFormB4Submission().getCaseDetails().getCaseGuid());
                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(userSessionSrv.getCaseEditFormB4Submission().getReply());
            }

            if (caseEditForm != null)
            {

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
                if (cusItem != null)
                {

                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                    model.addAttribute("dynamicFragmentHeader",
                            (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                    ? cusItem.getFragmentHead()
                                    : GC_Constants.gc_HeaderFragmentDefault);
                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                    model.addAttribute("dynamicFragmentTitle",
                            (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                    ? cusItem.getFragmentTitle()
                                    : GC_Constants.gc_TitleFragmentDefault);

                    model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                    model.addAttribute("dynamicFragmentFooter",
                            (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                    ? cusItem.getFragmentFooter()
                                    : GC_Constants.gc_FooterFragmentDefault);
                }
            }
        }

        return caseFormReply;

    }

    @GetMapping("/errCaseReply/")
    public String showErrorCaseReplyForm(Model model)
    {
        String viewName = VWNamesDirectory.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        if (userSessionSrv != null)
        {
            userSessionSrv.clearActiveSubmission();

            if (userSessionSrv.getCurrentReplyForm4Submission() != null && StringUtils.hasText(
                    userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid()))
            {

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                model.addAttribute("formErrors", userSessionSrv.getFormErrors());

                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessionSrv.getCaseDetails4Edit(
                        userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid());

                if (caseEditForm != null)
                {
                    // Super Impose Reply from User Form 4m Session
                    caseEditForm.setReply(userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getReply());
                    // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                    // But just to handle theoratically in case there is an Error in Attachment
                    // Persistence only- Remove the attachment otherwise let it persist
                    if (CollectionUtils.isNotEmpty(userSessionSrv.getMessageStack()))
                    {
                        Optional<TY_Message> attErrO = userSessionSrv.getMessageStack().stream()
                                .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                        if (attErrO.isPresent())
                        {
                            // Attachment able to presist do not remove it from Current Payload
                            caseEditForm.setAttachment(null);

                        }
                    }

                    model.addAttribute("caseEditForm", caseEditForm);

                    model.addAttribute("attachments", attSrv.getAttachmentNames());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                    TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
                    if (cusItem != null)
                    {

                        model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                        model.addAttribute("dynamicFragmentHeader",
                                (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                        ? cusItem.getFragmentHead()
                                        : GC_Constants.gc_HeaderFragmentDefault);
                        model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                        model.addAttribute("dynamicFragmentTitle",
                                (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                        ? cusItem.getFragmentTitle()
                                        : GC_Constants.gc_TitleFragmentDefault);

                        model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                        model.addAttribute("dynamicFragmentFooter",
                                (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                        ? cusItem.getFragmentFooter()
                                        : GC_Constants.gc_FooterFragmentDefault);

                    }

                }

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid() },
                        Locale.ENGLISH));
            }
        }

        return viewName;

    }

    @GetMapping("/caseDetails/{caseID}")
    public String getCaseDetails(@PathVariable String caseID, Model model)
    {
        String viewName = VWNamesDirectory.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        userSessionSrv.clearActiveSubmission();
        log.info("Navigating to case with UUID : " + caseID);

        if (StringUtils.hasText(caseID))
        {
            if (userSessionSrv != null)
            {
                TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
                // Before case form Inititation we must check the Rate Limit for the Current
                // User Session --current Form Submission added for Rate Limit Evaulation
                if (userSessionSrv.checkRateLimit())
                {
                    try
                    {

                        // Populate User Details
                        TY_UserESS userDetails = new TY_UserESS();
                        userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                        model.addAttribute("userInfo", userDetails);

                        // Get Case Details
                        TY_CaseEdit_Form caseEditForm = userSessionSrv.getCaseDetails4Edit(caseID);
                        if (caseEditForm != null)
                        {
                            model.addAttribute("caseEditForm", caseEditForm);
                            if (CollectionUtils.isNotEmpty(caseEditForm.getCaseDetails().getNotes()))
                            {
                                log.info("# External Notes Bound for Case ID - "
                                        + caseEditForm.getCaseDetails().getNotes().size());

                            }

                            // Initialize Attachments Session Service
                            if (attSrv != null)
                            {
                                attSrv.initialize();
                            }

                            // Attachment file Size
                            model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                            if (cusItem != null)
                            {
                                model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                                model.addAttribute("dynamicFragmentHeader",
                                        (cusItem.getFragmentHead() != null
                                                && !cusItem.getFragmentHead().trim().isBlank())
                                                        ? cusItem.getFragmentHead()
                                                        : GC_Constants.gc_HeaderFragmentDefault);
                                model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                                model.addAttribute("dynamicFragmentTitle",
                                        (cusItem.getFragmentTitle() != null
                                                && !cusItem.getFragmentTitle().trim().isBlank())
                                                        ? cusItem.getFragmentTitle()
                                                        : GC_Constants.gc_TitleFragmentDefault);

                                model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                                model.addAttribute("dynamicFragmentFooter",
                                        (cusItem.getFragmentFooter() != null
                                                && !cusItem.getFragmentFooter().trim().isBlank())
                                                        ? cusItem.getFragmentFooter()
                                                        : GC_Constants.gc_FooterFragmentDefault);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        return "error";
                    }
                }
                else
                {

                    if (cusItem != null)
                    {
                        // Not Within Rate Limit - REdirect to List View
                        viewName = VWNamesDirectory.getViewName(EnumVWNames.inbox, true,
                                cusItem.getCaseTypeEnum().toString());
                    }
                }

            }
        }

        return viewName;
    }

    @GetMapping("/confirmCase/{caseID}")
    public ModelAndView confirmCase(@PathVariable String caseID, RedirectAttributes attributes)
    {
        String svyUrl = null;

        TY_CaseConfirmPOJO caseDetails;

        if (StringUtils.hasText(caseID) && userSessionSrv != null)
        {
            log.info("Triggered Confirmation for case ID : " + caseID);

            try
            {

                // Check if SSurvey processing is activated for the LOb
                TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
                // Survey processing disabled for LoB
                if (cusItem != null)
                {
                    caseDetails = userSessionSrv.getCaseDetails4Confirmation(caseID);
                    if (cusItem.getEnableSvy())
                    {
                        log.info("Survey Processing Enabled for LoB : " + cusItem.getCaseTypeEnum().toString());
                        svyUrl = userSessionSrv.getSurveyUrl4CaseId(caseID);
                        if (StringUtils.hasText(svyUrl))
                        {

                            if (caseDetails != null && StringUtils.hasText(caseDetails.getETag()))
                            {
                                log.info("Etag Bound. Ready for patch....");
                                EV_CaseConfirmSubmit caseConfirmEvent = new EV_CaseConfirmSubmit(this, caseDetails);
                                applicationEventPublisher.publishEvent(caseConfirmEvent);
                            }
                            return new ModelAndView(new RedirectView(svyUrl)); // Redirect for EXTERNAL user with survey
                                                                               // URL
                        }

                    }
                    else
                    {
                        log.info("Survey Processing Disabled for LoB : " + cusItem.getCaseTypeEnum().toString());
                        if (caseDetails != null && StringUtils.hasText(caseDetails.getETag()))
                        {
                            log.info("Etag Bound. Ready for patch....");
                            EV_CaseConfirmSubmit caseConfirmEvent = new EV_CaseConfirmSubmit(this, caseDetails);
                            applicationEventPublisher.publishEvent(caseConfirmEvent);
                        }
                    }
                }

            }
            catch (Exception e)
            {
                if (e instanceof EX_ESMAPI)
                {
                    throw new EX_ESMAPI("Exception Triggered while Confirming the Case Id : " + caseID + "Details : "
                            + e.getLocalizedMessage());
                }

                if (e instanceof EX_CaseAlreadyConfirmed)
                {
                    userSessionSrv.addSessionMessage(e.getMessage());
                    attributes.addFlashAttribute("message", e.getMessage());
                    return new ModelAndView(new RedirectView("/ess/errorConfirm"));

                }
            }

        }
        // Parallel redirect to Inbox View
        log.info("Redirecting to Inbox View for Case Confirmation");
        return new ModelAndView(new RedirectView(VWNamesDirectory.getViewName(EnumVWNames.inbox, true,
                userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString())));
    }

    @GetMapping("/errorConfirm")
    public String redirectWithUsingRedirectView(Model model, @ModelAttribute("message") String message)
    {
        model.addAttribute("message", message);
        return caseConfirmError;

    }

    @GetMapping("/refreshForm4SelCatg")
    public String refreshFormCxtx4SelCatg(HttpServletRequest request, Model model)
    {
        String viewCaseForm = VWNamesDirectory.getViewName(EnumVWNames.caseForm, false, (String[]) null);
        log.info("Refreshing Case Form for Category Selection... Case Form View: " + viewCaseForm);
        ;
        if (userSessionSrv != null)
        {
            TY_Case_Form caseForm = userSessionSrv.getCaseFormB4Submission();

            if (caseForm != null)
            {
                userSessionSrv.setCaseFormB4Submission(null);

                // Normal Scenario - Catg. chosen Not relevant for Notes Template and/or
                // additional fields

                if ((StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getAccountId())
                        || StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getEmployeeId()))
                        && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                {

                    TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
                    if (cusItem != null)
                    {
                        model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());
                        viewCaseForm = (cusItem.getCaseFormView() != null
                                && !cusItem.getCaseFormView().trim().isBlank()) ? cusItem.getCaseFormView()
                                        : viewCaseForm;
                        log.info("Case View : " + viewCaseForm);
                        // Populate User Details
                        TY_UserESS userDetails = new TY_UserESS();
                        userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                        model.addAttribute("userInfo", userDetails);

                        // clear Form errors on each refresh or a New Case form request
                        if (CollectionUtils.isNotEmpty(userSessionSrv.getFormErrors()))
                        {
                            userSessionSrv.clearFormErrors();
                        }

                        // also Upload the Catg. Tree as per Case Type
                        model.addAttribute("catgsList",
                                catalogTreeSrv.getCaseCatgTree4LoB(cusItem.getCaseTypeEnum()).getCategories());

                        // Scan Current Catg for Templ. Load and or Additional Fields

                        // Scan for Template Load
                        TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                                cusItem.getCaseTypeEnum());
                        if (catgTemplate != null)
                        {

                            // Set Questionnaire for Category
                            caseForm.setTemplate(catgTemplate.getQuestionnaire());

                        }

                        // Also set the Category Description in Upper Case
                        // Get the Category Description for the Category ID from Case Form
                        TY_CatgDetails catgDetails = catalogSrv.getCategoryDetails4Catg(caseForm.getCatgDesc(),
                                cusItem.getCaseTypeEnum(), true);
                        if (catgDetails != null)
                        {
                            caseForm.setCatgText(catgDetails.getCatDesc());
                            log.info("Catg. Text for Category ID : " + caseForm.getCatgDesc() + " is : "
                                    + catgDetails.getCatDesc());
                        }

                        // Case Form Model Set at last
                        model.addAttribute("caseForm", caseForm);

                        if (attSrv != null)
                        {
                            if (CollectionUtils.isNotEmpty(attSrv.getAttachmentNames()))
                            {
                                model.addAttribute("attachments", attSrv.getAttachmentNames());
                            }
                        }

                        // Attachment file Size
                        model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                        // View Name for Dynamic Template Header and Title
                        model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                        model.addAttribute("dynamicFragmentHeader",
                                (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                        ? cusItem.getFragmentHead()
                                        : GC_Constants.gc_HeaderFragmentDefault);
                        model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                        model.addAttribute("dynamicFragmentTitle",
                                (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                        ? cusItem.getFragmentTitle()
                                        : GC_Constants.gc_TitleFragmentDefault);

                        model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                        model.addAttribute("dynamicFragmentFooter",
                                (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                        ? cusItem.getFragmentFooter()
                                        : GC_Constants.gc_FooterFragmentDefault);

                    }
                    else
                    {

                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                        { userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
                    }

                }
            }

        }

        return viewCaseForm;
    }

    @GetMapping("/refreshForm4AttUpload")
    public String refreshCaseFormPostAttachmentUpload(Model model)
    {
        String viewCaseForm = VWNamesDirectory.getViewName(EnumVWNames.caseForm, false, (String[]) null);

        if (attSrv != null && userSessionSrv != null)
        {
            TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {
                viewCaseForm = (cusItem.getCaseFormView() != null && !cusItem.getCaseFormView().trim().isBlank())
                        ? cusItem.getCaseFormView()
                        : viewCaseForm;
                TY_Case_Form caseForm = userSessionSrv.getCaseFormB4Submission();

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                if (userSessionSrv != null)
                {
                    log.info("User Bound in Session..");
                }
                userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                model.addAttribute("caseForm", caseForm);
                // also Place the form in Session
                userSessionSrv.setCaseFormB4Submission(caseForm);

                model.addAttribute("formErrors", attSrv.getSessionMessages());

                // also Upload the Catg. Tree as per Case Type
                model.addAttribute("catgsList",
                        catalogTreeSrv.getCaseCatgTree4LoB(cusItem.getCaseTypeEnum()).getCategories());

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                // View Name for Dynamic Template Header and Title
                model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                model.addAttribute("dynamicFragmentHeader",
                        (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                ? cusItem.getFragmentHead()
                                : GC_Constants.gc_HeaderFragmentDefault);
                model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                model.addAttribute("dynamicFragmentTitle",
                        (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                ? cusItem.getFragmentTitle()
                                : GC_Constants.gc_TitleFragmentDefault);

                model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                model.addAttribute("dynamicFragmentFooter",
                        (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                ? cusItem.getFragmentFooter()
                                : GC_Constants.gc_FooterFragmentDefault);

            }

        }
        return viewCaseForm;

    }

    @GetMapping("/refreshFormReply4AttUpload")
    public String refreshCaseReplyFormPostAttachmentUpload(Model model)
    {
        String viewName = VWNamesDirectory.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        List<String> attMsgs = Collections.emptyList();
        TY_CaseEdit_Form caseEditForm = null;
        if (userSessionSrv != null)
        {
            caseEditForm = userSessionSrv.getCaseEditFormB4Submission();
            if (caseEditForm != null)
            {
                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessionSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Attachment to Local Storage Persistence Error
                attMsgs = attSrv.getSessionMessages();

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("formErrors", attMsgs);

                model.addAttribute("attachments", attSrv.getAttachmentNames());
                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
                TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
                if (cusItem != null)
                {

                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                    model.addAttribute("dynamicFragmentHeader",
                            (cusItem.getFragmentHead() != null && !cusItem.getFragmentHead().trim().isBlank())
                                    ? cusItem.getFragmentHead()
                                    : GC_Constants.gc_HeaderFragmentDefault);
                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                    model.addAttribute("dynamicFragmentTitle",
                            (cusItem.getFragmentTitle() != null && !cusItem.getFragmentTitle().trim().isBlank())
                                    ? cusItem.getFragmentTitle()
                                    : GC_Constants.gc_TitleFragmentDefault);

                    model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                    model.addAttribute("dynamicFragmentFooter",
                            (cusItem.getFragmentFooter() != null && !cusItem.getFragmentFooter().trim().isBlank())
                                    ? cusItem.getFragmentFooter()
                                    : GC_Constants.gc_FooterFragmentDefault);

                }
            }
        }

        return viewName;

    }

}
