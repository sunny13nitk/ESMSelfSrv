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
import com.sap.cap.esmapi.ui.srv.intf.IF_ESS_UISrv;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.constants.VWNamesDirectory;
import com.sap.cap.esmapi.utilities.enums.EnumCaseTypes;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumVWNames;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.pojos.Ty_UserAccountEmployee;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserAPISrv;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpLOBUIModelSrv;
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
    private IF_UserAPISrv userSrv;

    @Autowired
    private UserInfo userInfo;

    @Autowired
    private IF_ESS_UISrv uiSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private TY_CatgCus catgCusSrv;

    @Autowired
    private IF_CatalogSrv catalogTreeSrv;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private IF_VHelpLOBUIModelSrv vhlpUISrv;

    @Autowired
    private IF_SrvCloudAPI srvCloudApiSrv;

    @Autowired
    private IF_CatalogSrv catalogSrv;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_SessAttachmentsService attSrv;

    @Autowired
    private IF_UserSessionSrv userSessionSrv;

    private final String caseListVWRedirect = "redirect:/lso/";
    private final String caseFormViewLXSS = "caseFormLSOLXSS";
    private final String caseFormReplyLXSS = "caseFormReplyLSOLXSS";
    private final String lsoCaseListViewLXSS = "lsoCasesListViewLXSS";
    private final String caseConfirmError = "alreadyConfirmed";
    private final String invalidToken = "invalid_token";

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

                                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                                    model.addAttribute("dynamicFragmentHeader", catgCusItem.getFragmentHead());
                                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                                    model.addAttribute("dynamicFragmentTitle", catgCusItem.getFragmentTitle());

                                }

                                else
                                {

                                    throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                                    { catgCusItem.getCaseTypeEnum().toString() }, Locale.ENGLISH));
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
                vw = invalidToken;
            }
        }
        catch (Exception e)
        {
            throw new EX_ESMAPI(e.getLocalizedMessage());
        }

        return vw != null ? vw : invalidToken;

    }

    @GetMapping("/createCase/")
    public String showCaseAsyncForm(Model model)
    {
        userSessionSrv.clearActiveSubmission();
        String viewCaseForm = caseFormViewLXSS;

        if ((StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessionSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()) && userInfo.isAuthenticated())
        {

            TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                // Before case form Inititation we must check the Rate Limit for the Current
                // User Session --current Form Submission added for Rate Limit Evaulation
                if (userSessionSrv.isWithinRateLimit())
                {
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

                }
                else
                {
                    // Not Within Rate Limit - REdirect to List View
                    viewCaseForm = caseListVWRedirect;

                }

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
            }

        }

        return viewCaseForm;
    }

    @GetMapping("/errForm/")
    public String showErrorCaseForm(Model model)
    {

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
                caseForm.setCatgDesc(userSessionSrv.getCurrentForm4Submission().getCaseForm().getCatgDesc()); // Curr
                                                                                                              // Catg
                caseForm.setDescription(userSessionSrv.getCurrentForm4Submission().getCaseForm().getDescription()); // Curr
                                                                                                                    // Notes
                caseForm.setSubject(userSessionSrv.getCurrentForm4Submission().getCaseForm().getSubject()); // Curr
                                                                                                            // Subject

                if (StringUtils.hasText(userSessionSrv.getCurrentForm4Submission().getCaseForm().getAddEmail())) // Affected
                                                                                                                 // User
                                                                                                                 // email
                {
                    caseForm.setAddEmail(userSessionSrv.getCurrentForm4Submission().getCaseForm().getAddEmail());
                }

                if (StringUtils.hasText(userSessionSrv.getCurrentForm4Submission().getCaseForm().getCountry()))
                {
                    caseForm.setCountry(userSessionSrv.getCurrentForm4Submission().getCaseForm().getCountry());
                }

                if (StringUtils.hasText(userSessionSrv.getCurrentForm4Submission().getCaseForm().getLanguage()))
                {
                    caseForm.setLanguage(userSessionSrv.getCurrentForm4Submission().getCaseForm().getLanguage());
                }

                if (StringUtils.hasText(userSessionSrv.getCurrentForm4Submission().getCaseForm().getLanguage()))
                {
                    caseForm.setLanguage(userSessionSrv.getCurrentForm4Submission().getCaseForm().getLanguage());
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

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
            }

        }

        return caseFormViewLXSS;
    }

    @GetMapping("/removeAttachment/{fileName}")
    public String removeAttachmentCaseCreate(@PathVariable String fileName, Model model)
    {
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
                }

            }
        }

        return caseFormViewLXSS;
    }

    @GetMapping("/caseReply/removeAttachment/{fileName}")
    public String removeAttachmentCaseReply(@PathVariable String fileName, Model model)
    {
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
            }
        }

        return caseFormReplyLXSS;

    }

    @GetMapping("/errCaseReply/")
    public String showErrorCaseReplyForm(Model model)
    {
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
                }

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_DET_FETCH", new Object[]
                { userSessionSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid() },
                        Locale.ENGLISH));
            }
        }

        return caseFormReplyLXSS;

    }

    @GetMapping("/caseDetails/{caseID}")
    public String getCaseDetails(@PathVariable String caseID, Model model)
    {
        String viewName = caseFormReplyLXSS;
        userSessionSrv.clearActiveSubmission();
        log.info("Navigating to case with UUID : " + caseID);
        if (StringUtils.hasText(caseID))
        {
            if (userSessionSrv != null)
            {
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
                        }
                    }
                    catch (Exception e)
                    {
                        return "error";
                    }
                }
                else
                {
                    // Not Within Rate Limit - REdirect to List View
                    viewName = caseListVWRedirect;
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

                Ty_UserAccountEmployee usAccConEmpl = userSessionSrv.getUserDetails4mSession();

                if (!usAccConEmpl.isExternal())
                { // Internal User
                    log.info("Internal employee detected. Skipping survey redirect for case ID: " + caseID);

                    caseDetails = userSessionSrv.getCaseDetails4Confirmation(caseID);
                    if (caseDetails != null && StringUtils.hasText(caseDetails.getETag()))
                    {
                        EV_CaseConfirmSubmit caseConfirmEvent = new EV_CaseConfirmSubmit(this, caseDetails);
                        applicationEventPublisher.publishEvent(caseConfirmEvent);
                    }
                    return new ModelAndView(new RedirectView(GC_Constants.gc_basePath)); // Redirect for INTERNAL user -
                                                                                         // Base Path
                }
                else
                { // External User
                    svyUrl = userSessionSrv.getSurveyUrl4CaseId(caseID);

                    if (StringUtils.hasText(svyUrl))
                    {
                        caseDetails = userSessionSrv.getCaseDetails4Confirmation(caseID);
                        if (caseDetails != null && StringUtils.hasText(caseDetails.getETag()))
                        {
                            EV_CaseConfirmSubmit caseConfirmEvent = new EV_CaseConfirmSubmit(this, caseDetails);
                            applicationEventPublisher.publishEvent(caseConfirmEvent);
                        }
                        return new ModelAndView(new RedirectView(svyUrl)); // Redirect for EXTERNAL user with survey URL
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
                    return new ModelAndView(new RedirectView("/lso/errorConfirm"));

                }
            }

        }
        return new ModelAndView(new RedirectView(GC_Constants.gc_basePath)); // Redirect for INTERNAL user -
                                                                             // Base Path
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
                    }
                    else
                    {

                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                        { userSessionSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
                    }

                }
            }

        }

        return caseFormViewLXSS;
    }

    @GetMapping("/refreshForm4AttUpload")
    public String refreshCaseFormPostAttachmentUpload(Model model)
    {
        String viewName = caseFormViewLXSS;

        if (attSrv != null && userSessionSrv != null)
        {
            TY_CatgCusItem cusItem = userSessionSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

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

            }

        }
        return viewName;

    }

    @GetMapping("/refreshFormReply4AttUpload")
    public String refreshCaseReplyFormPostAttachmentUpload(Model model)
    {
        String viewName = caseFormReplyLXSS;
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
            }
        }

        return viewName;

    }

}
