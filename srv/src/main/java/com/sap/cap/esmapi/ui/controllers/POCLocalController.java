package com.sap.cap.esmapi.ui.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.sap.cap.esmapi.catg.pojos.TY_CatgCus;
import com.sap.cap.esmapi.catg.pojos.TY_CatgCusItem;
import com.sap.cap.esmapi.catg.pojos.TY_CatgTemplates;
import com.sap.cap.esmapi.catg.srv.intf.IF_CatalogSrv;
import com.sap.cap.esmapi.events.event.EV_CaseConfirmSubmit;
import com.sap.cap.esmapi.events.event.EV_CaseFormSubmit;
import com.sap.cap.esmapi.events.event.EV_CaseReplySubmit;
import com.sap.cap.esmapi.exceptions.EX_CaseAlreadyConfirmed;
import com.sap.cap.esmapi.exceptions.EX_ESMAPI;
import com.sap.cap.esmapi.ui.pojos.TY_CaseConfirmPOJO;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEditFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_CaseEdit_Form;
import com.sap.cap.esmapi.ui.pojos.TY_CaseFormAsync;
import com.sap.cap.esmapi.ui.pojos.TY_Case_Form;
import com.sap.cap.esmapi.utilities.constants.GC_Constants;
import com.sap.cap.esmapi.utilities.constants.VWNamesDirectoryLocal;
import com.sap.cap.esmapi.utilities.enums.EnumMessageType;
import com.sap.cap.esmapi.utilities.enums.EnumVWNames;
import com.sap.cap.esmapi.utilities.pojos.TY_Message;
import com.sap.cap.esmapi.utilities.pojos.TY_RLConfig;
import com.sap.cap.esmapi.utilities.pojos.TY_UserESS;
import com.sap.cap.esmapi.utilities.srv.intf.IF_SessAttachmentsService;
import com.sap.cap.esmapi.utilities.srv.intf.IF_UserSessionSrv;
import com.sap.cap.esmapi.utilities.srvCloudApi.srv.intf.IF_SrvCloudAPI;
import com.sap.cap.esmapi.utilities.uimodel.intf.IF_CountryLanguageVHelpAdj;
import com.sap.cap.esmapi.vhelps.srv.intf.IF_VHelpLOBUIModelSrv;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/poclocal")
@Slf4j
public class POCLocalController
{
    @Autowired
    private IF_UserSessionSrv userSessSrv;

    @Autowired
    private TY_CatgCus catgCusSrv;

    @Autowired
    private IF_CatalogSrv catalogTreeSrv;

    @Autowired
    private MessageSource msgSrc;

    @Autowired
    private IF_VHelpLOBUIModelSrv vhlpUISrv;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IF_SessAttachmentsService attSrv;

    @Autowired
    private TY_RLConfig rlConfig;

    @Autowired
    private IF_CountryLanguageVHelpAdj coLaDDLBSrv;

    // #TEST - Begin
    @Autowired
    private IF_SrvCloudAPI apiSrv;

    private final String caseConfirmError = "alreadyConfirmed";

    // #TEST - End

    @GetMapping("/{lob}")
    public String showCasesList(@PathVariable(name = "lob") String lob, Model model)
    {
        log.info("User Logged in with lob :" + lob);
        TY_UserESS userDetails = new TY_UserESS();

        // Mocking the authentication

        if (userSessSrv != null)
        {
            userSessSrv.loadUser4Test(lob);

            // check User and Account Bound
            if (userSessSrv.getUserDetails4mSession() != null)
            {
                if (StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                        || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                {
                    if (!CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                    {

                        TY_CatgCusItem catgCusItem = userSessSrv.getCurrentLOBConfig();
                        if (catgCusItem != null)

                        {
                            userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                            userDetails.setCases(userSessSrv.getCases4User4mSession());
                            model.addAttribute("userInfo", userDetails);
                            model.addAttribute("caseTypeStr", catgCusItem.getCaseTypeEnum().toString());
                            // Rate Limit Simulation
                            model.addAttribute("rateLimitBreached", userSessSrv.getCurrentRateLimitBreachedValue());

                            // Even if No Cases - spl. for Newly Create Acc - to enable REfresh button
                            model.addAttribute("sessMsgs", userSessSrv.getSessionMessages());

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

                        }

                        else
                        {

                            throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                            { lob }, Locale.ENGLISH));
                        }
                    }

                }
            }

        }

        return "essListViewPOCLocalLXSS";

    }

    @GetMapping("/createCase/")
    public String showCaseAsyncForm(Model model)
    {
        String viewCaseForm = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseForm, false, (String[]) null);

        if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
        {

            TY_CatgCusItem catgCusItem = userSessSrv.getCurrentLOBConfig();
            if (catgCusItem != null)

            {

                model.addAttribute("caseTypeStr", catgCusItem.getCaseTypeEnum().toString());

                // Before case form Inititation we must check the Rate Limit for the Current
                // User Session --current Form Submission added for Rate Limit Evaulation
                if (userSessSrv.isWithinRateLimit())
                {

                    // REset Previous Catg
                    userSessSrv.setPreviousCategory(null);
                    // Populate User Details
                    TY_UserESS userDetails = new TY_UserESS();
                    userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                    model.addAttribute("userInfo", userDetails);

                    // Initialize Attachments Session Service
                    if (attSrv != null)
                    {
                        attSrv.initialize();
                    }

                    // Populate Case Form Details
                    TY_Case_Form caseForm = new TY_Case_Form();
                    if (userSessSrv.getUserDetails4mSession().isEmployee())
                    {
                        caseForm.setAccId(userSessSrv.getUserDetails4mSession().getEmployeeId()); // hidden
                    }
                    else
                    {
                        caseForm.setAccId(userSessSrv.getUserDetails4mSession().getAccountId()); // hidden
                    }

                    caseForm.setCaseTxnType(catgCusItem.getCaseType()); // hidden
                    caseForm.setLob(catgCusItem.getCaseTypeEnum().toString()); // hidden
                    model.addAttribute("caseForm", caseForm);

                    model.addAttribute("formErrors", null);

                    // clear Form errors on each refresh or a New Case form request
                    if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
                    {
                        userSessSrv.clearFormErrors();

                    }

                    // also Upload the Catg. Tree as per Case Type
                    model.addAttribute("catgsList",
                            catalogTreeSrv.getCaseCatgTree4LoB(catgCusItem.getCaseTypeEnum()).getCategories());

                    // Attachment file Size
                    model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                    model.addAttribute("dynamicTemplateHeader", GC_Constants.gc_HeaderFragments);
                    model.addAttribute("dynamicFragmentHeader",
                            (catgCusItem.getFragmentHead() != null && !catgCusItem.getFragmentHead().trim().isBlank())
                                    ? catgCusItem.getFragmentHead()
                                    : GC_Constants.gc_HeaderFragmentDefault);
                    model.addAttribute("dynamicTemplateTitle", GC_Constants.gc_TitleFragments);
                    model.addAttribute("dynamicFragmentTitle",
                            (catgCusItem.getFragmentTitle() != null && !catgCusItem.getFragmentTitle().trim().isBlank())
                                    ? catgCusItem.getFragmentTitle()
                                    : GC_Constants.gc_TitleFragmentDefault);

                    model.addAttribute("dynamicTemplateFooter", GC_Constants.gc_FooterFragments);
                    model.addAttribute("dynamicFragmentFooter",
                            (catgCusItem.getFragmentFooter() != null
                                    && !catgCusItem.getFragmentFooter().trim().isBlank())
                                            ? catgCusItem.getFragmentFooter()
                                            : GC_Constants.gc_FooterFragmentDefault);

                    // Check if LoB Specific Case Form is configured
                    if (StringUtils.hasText(catgCusItem.getCaseFormView()))
                    {
                        viewCaseForm = catgCusItem.getCaseFormView();
                    }

                }
                else
                {
                    // Not Within Rate Limit - REdirect to List View
                    viewCaseForm = VWNamesDirectoryLocal.getViewName(EnumVWNames.inbox, true,
                            userSessSrv.getCurrentLOBConfig().getCaseTypeEnum().toString());

                }

            }
            else
            {

                throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                { userSessSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
            }

        }

        return viewCaseForm;
    }

    @PostMapping(value = "/saveCase", params = "action=saveCase")
    public String saveCase(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {

        // REdirect to Cases List View depending on LoB
        String viewName = VWNamesDirectoryLocal.getViewName(EnumVWNames.inbox, true,
                userSessSrv.getCurrentLOBConfig().getCaseTypeEnum().toString());

        if (caseForm != null && userSessSrv != null)
        {
            if (userSessSrv.getUserDetails4mSession().isEmployee())
            {
                caseForm.setEmployee(true);
            }

            log.info("Processing of Case Form - UI layer :Begins....");

            // Any Validation Error(s) on the Form or Submission not possible
            if (!userSessSrv.SubmitCaseForm(caseForm))
            {
                // Redirect to Error Processing of Form
                viewName = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseFormError, true, (String[]) null);
            }
            else
            {
                // Fire Case Submission Event - To be processed Asyncronously

                TY_CaseFormAsync caseFormAsync = userSessSrv.getCurrentForm4Submission();
                // External/Internal User Pass to Asynch Thread as Session Scoped Service would
                // not be accessible in Asynch thread
                caseFormAsync.getCaseForm().setExternal(userSessSrv.getUserDetails4mSession().isExternal());
                caseFormAsync.setDesProps(userSessSrv.getDestinationDetails4mUserSession());
                EV_CaseFormSubmit eventCaseSubmit = new EV_CaseFormSubmit(this, caseFormAsync);
                applicationEventPublisher.publishEvent(eventCaseSubmit);
            }

            log.info("Processing of Case Form - UI layer :Ends....");
        }

        return viewName;

    }

    @PostMapping(value = "/saveCase", params = "action=upload")
    public String uploadAttachments(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {
        String viewName = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseForm, false, (String[]) null);

        List<String> attMsgs = Collections.emptyList();

        if (caseForm != null && attSrv != null && userSessSrv != null)
        {

            log.info("Processing of Case Attachment Upload Form - UI layer :Begins....");
            if (caseForm.getAttachment() != null)
            {
                if (StringUtils.hasText(caseForm.getAttachment().getOriginalFilename()))
                {
                    // Clear Attachment Service Session Messages for subsequent roundtip
                    attSrv.clearSessionMessages();
                    if (!attSrv.addAttachment(caseForm.getAttachment()))
                    {
                        // Attachment to Local Storage Persistence Error
                        attMsgs = attSrv.getSessionMessages();

                    }

                }

            }

            // Clear form for New Attachment as Current Attachment already in Container
            caseForm.setAttachment(null);

            TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                if (userSessSrv.getUserDetails4mSession().isEmployee())
                {
                    caseForm.setEmployee(true);
                }

                // Scan for Template Load
                TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                        cusItem.getCaseTypeEnum());
                if (catgTemplate != null)
                {

                    // Set Questionnaire for Category
                    caseForm.setTemplate(catgTemplate.getQuestionnaire());

                }

                model.addAttribute("caseForm", caseForm);
                // also Place the form in Session
                userSessSrv.setCaseFormB4Submission(caseForm);

                model.addAttribute("formErrors", attMsgs);

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

            log.info("Processing of Case Attachment Upload Form - UI layer :Ends....");
        }

        return viewName;

    }

    @GetMapping("/errForm/")
    public String showErrorCaseForm(Model model)
    {

        String viewCaseForm = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseForm, false, (String[]) null);

        if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations())
                && userSessSrv.getCurrentForm4Submission() != null)
        {

            TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());
                viewCaseForm = (cusItem.getCaseFormView() != null && !cusItem.getCaseFormView().trim().isBlank())
                        ? cusItem.getCaseFormView()
                        : viewCaseForm;

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                // Populate Case Form Details
                TY_Case_Form caseForm = new TY_Case_Form();
                if (userSessSrv.getUserDetails4mSession().isEmployee())
                {
                    caseForm.setAccId(userSessSrv.getUserDetails4mSession().getEmployeeId()); // hidden
                }
                else
                {
                    caseForm.setAccId(userSessSrv.getUserDetails4mSession().getAccountId()); // hidden
                }

                caseForm.setCaseTxnType(cusItem.getCaseType()); // hidden
                caseForm.setLob(cusItem.getCaseTypeEnum().toString()); // hidden
                caseForm.setCatgDesc(userSessSrv.getCurrentForm4Submission().getCaseForm().getCatgDesc()); // Curr Catg
                caseForm.setDescription(userSessSrv.getCurrentForm4Submission().getCaseForm().getDescription()); // Curr
                                                                                                                 // Notes
                caseForm.setSubject(userSessSrv.getCurrentForm4Submission().getCaseForm().getSubject()); // Curr Subject
                caseForm.setAddEmail(userSessSrv.getCurrentForm4Submission().getCaseForm().getAddEmail()); // Curr Add
                                                                                                           // Email

                model.addAttribute("formErrors", userSessSrv.getFormErrors());

                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
                            .filter(e -> e.getMsgType().equals(EnumMessageType.ERR_ATTACHMENT)).findFirst();
                    if (!attErrO.isPresent())
                    {
                        // Attachment able to presist do not remove it from Current Payload
                        caseForm.setAttachment(userSessSrv.getCurrentForm4Submission().getCaseForm().getAttachment());

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
                { userSessSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
            }

        }

        return viewCaseForm;
    }

    @GetMapping("/success")
    public String showSuccess(Model model)
    {

        return "success";
    }

    @PostMapping(value = "/saveCase", params = "action=catgChange")
    public String refreshCaseForm4Catg(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {

        String viewCaseForm = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseForm, false, (String[]) null);
        if (caseForm != null && userSessSrv != null)
        {

            // Normal Scenario - Catg. chosen Not relevant for Notes Template and/or
            // additional fields

            if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                    || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                    && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
            {

                TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
                if (cusItem != null)
                {

                    model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                    // Populate User Details
                    TY_UserESS userDetails = new TY_UserESS();
                    userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                    model.addAttribute("userInfo", userDetails);

                    // clear Form errors on each refresh or a New Case form request
                    if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
                    {
                        userSessSrv.clearFormErrors();
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
                    { userSessSrv.getCurrentLOBConfig().getCaseTypeEnum() }, Locale.ENGLISH));
                }

            }

        }

        return viewCaseForm;

    }

    @GetMapping("/removeAttachment/{fileName}")
    public String removeAttachmentCaseCreate(@PathVariable String fileName, Model model)
    {
        String caseFormView = null;
        if (StringUtils.hasText(fileName) && attSrv != null && userSessSrv != null)
        {
            if (attSrv.checkIFExists(fileName))
            {
                attSrv.removeAttachmentByName(fileName);
            }

            // Populate the view

            TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                caseFormView = (cusItem.getCaseFormView() != null && !cusItem.getCaseFormView().trim().isBlank())
                        ? cusItem.getCaseFormView()
                        : VWNamesDirectoryLocal.getViewName(EnumVWNames.caseForm, false, (String[]) null);

                // Get Case from Session Service

                TY_Case_Form caseForm = userSessSrv.getCaseFormB4Submission();

                if (caseForm != null)
                {

                    if (userSessSrv.getUserDetails4mSession().isEmployee())
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
        return caseFormView;
    }

    @GetMapping("/caseReply/removeAttachment/{fileName}")
    public String removeAttachmentCaseReply(@PathVariable String fileName, Model model)
    {
        String caseFormReply = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        if (StringUtils.hasText(fileName) && attSrv != null && userSessSrv != null)
        {
            if (attSrv.checkIFExists(fileName))
            {
                attSrv.removeAttachmentByName(fileName);
            }

            // Populate User Details
            TY_UserESS userDetails = new TY_UserESS();
            userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
            model.addAttribute("userInfo", userDetails);

            model.addAttribute("formErrors", userSessSrv.getFormErrors());

            // Get Case Details
            TY_CaseEdit_Form caseEditForm = null;

            // Try to Get Case Edit Form from Upload Form from User Submit
            if (userSessSrv.getCurrentReplyForm4Submission() != null)
            {
                caseEditForm = userSessSrv.getCaseDetails4Edit(
                        userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getReply());
                // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                // But just to handle theoratically in case there is an Error in Attachment
                // Persistence only- Remove the attachment otherwise let it persist
                if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                {
                    Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
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
                caseEditForm = userSessSrv
                        .getCaseDetails4Edit(userSessSrv.getCaseEditFormB4Submission().getCaseDetails().getCaseGuid());
                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(userSessSrv.getCaseEditFormB4Submission().getReply());
            }

            if (caseEditForm != null)
            {

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
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

    @PostMapping(value = "/saveCaseReply", params = "action=saveCaseEdit")
    public String saveCaseReply(@ModelAttribute("caseEditForm") TY_CaseEdit_Form caseReplyForm, Model model)
            throws EX_ESMAPI, IOException
    {

        String viewName = VWNamesDirectoryLocal.getViewName(EnumVWNames.inbox, true,
                userSessSrv.getCurrentLOBConfig().getCaseTypeEnum().toString());
        if (caseReplyForm != null && userSessSrv != null)
        {

            log.info("Processing of Case Reply Form - UI layer :Begins....");

            // Any Validation Error(s) on the Form or Submission not possible
            if (!userSessSrv.SubmitCaseReply(caseReplyForm))
            {
                // Redirect to Error Processing of Form
                viewName = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseReply, true, (String[]) null);
            }
            else
            {
                // Fire Case Submission Event - To be processed Asyncronously
                TY_CaseEditFormAsync caseEditFormAsync = userSessSrv.getCurrentReplyForm4Submission();

                // External/Internal User Pass to Asynch Thread as Session Scoped Service would
                // not be accessible in Asynch thread
                caseEditFormAsync.getCaseReply().setExternal(userSessSrv.getUserDetails4mSession().isExternal());
                caseEditFormAsync.setDesProps(userSessSrv.getDestinationDetails4mUserSession());
                EV_CaseReplySubmit eventCaseReplySubmit = new EV_CaseReplySubmit(this, caseEditFormAsync);
                applicationEventPublisher.publishEvent(eventCaseReplySubmit);
            }

            log.info("Processing of Case Form - UI layer :Ends....");
        }
        return viewName;
    }

    @PostMapping(value = "/saveCaseReply", params = "action=upload")
    public String uploadCaseReplyAttachment(@ModelAttribute("caseEditForm") TY_CaseEdit_Form caseReplyForm, Model model)
    {
        String caseFormReply = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        List<String> attMsgs = Collections.emptyList();
        if (caseReplyForm != null && userSessSrv != null)
        {
            if (StringUtils.hasText(caseReplyForm.getCaseDetails().getCaseGuid()))
            {

                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessSrv
                        .getCaseDetails4Edit(caseReplyForm.getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(caseReplyForm.getReply());

                // Clear form for New Attachment as Current Attachment already in Container
                caseEditForm.setAttachment(null);

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                if (caseReplyForm.getAttachment() != null)
                {
                    if (StringUtils.hasText(caseReplyForm.getAttachment().getOriginalFilename()))
                    {
                        // Clear Attachment Service Session Messages for subsequent roundtip
                        attSrv.clearSessionMessages();
                        if (!attSrv.addAttachment(caseReplyForm.getAttachment()))
                        {
                            // Attachment to Local Storage Persistence Error
                            attMsgs = attSrv.getSessionMessages();

                        }

                    }

                }

                userSessSrv.setCaseEditFormB4Submission(caseEditForm);

                model.addAttribute("caseEditForm", caseEditForm);

                model.addAttribute("formErrors", attMsgs);

                model.addAttribute("attachments", attSrv.getAttachmentNames());
                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

                TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
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
        String caseFormReply = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        if (userSessSrv != null)
        {

            if (userSessSrv.getCurrentReplyForm4Submission() != null && StringUtils.hasText(
                    userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid()))
            {

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                model.addAttribute("formErrors", userSessSrv.getFormErrors());

                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessSrv.getCaseDetails4Edit(
                        userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid());

                if (caseEditForm != null)
                {
                    // Super Impose Reply from User Form 4m Session
                    caseEditForm.setReply(userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getReply());
                    // Not Feasible to have a Validation Error in Form and Attachment Persisted -
                    // But just to handle theoratically in case there is an Error in Attachment
                    // Persistence only- Remove the attachment otherwise let it persist
                    if (CollectionUtils.isNotEmpty(userSessSrv.getMessageStack()))
                    {
                        Optional<TY_Message> attErrO = userSessSrv.getMessageStack().stream()
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

                    TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
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
                { userSessSrv.getCurrentReplyForm4Submission().getCaseReply().getCaseDetails().getCaseGuid() },
                        Locale.ENGLISH));
            }
        }
        return caseFormReply;

    }

    @GetMapping("/caseDetails/{caseID}")
    public String getCaseDetails(@PathVariable String caseID, Model model)
    {
        String caseFormReply = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseReply, false, (String[]) null);
        if (StringUtils.hasText(caseID))
        {
            if (userSessSrv != null)
            {

                try
                {

                    // Populate User Details
                    TY_UserESS userDetails = new TY_UserESS();
                    userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                    model.addAttribute("userInfo", userDetails);

                    // Get Case Details
                    TY_CaseEdit_Form caseEditForm = userSessSrv.getCaseDetails4Edit(caseID);
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

                        TY_CatgCusItem catgCusItem = userSessSrv.getCurrentLOBConfig();
                        if (catgCusItem != null)
                        {
                            // Attachment file Size
                            model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());

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
                        }

                    }
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return caseFormReply;

    }

    @GetMapping("/confirmCase/{caseID}")
    public ModelAndView confirmCase(@PathVariable String caseID, RedirectAttributes attributes)
    {
        String svyUrl = null;
        TY_CaseConfirmPOJO caseDetails;
        if (StringUtils.hasText(caseID) && userSessSrv != null)
        {
            log.info("Triggered Confirmation for case ID : " + caseID);

            try
            {
                svyUrl = userSessSrv.getSurveyUrl4CaseId(caseID);
                // Only now Proceed to Confirm the case
                if (StringUtils.hasText(svyUrl))
                {
                    caseDetails = userSessSrv.getCaseDetails4Confirmation(caseID);
                    if (caseDetails != null)
                    {
                        if (StringUtils.hasText(caseDetails.getETag()))
                        {
                            // Prepare Case Confirm Event and Trigger the same
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
                    userSessSrv.addSessionMessage(e.getMessage());
                    attributes.addFlashAttribute("message", e.getMessage());
                    return new ModelAndView(new RedirectView("/poclocal/errorConfirm"));

                }
            }

        }

        return new ModelAndView(new RedirectView(svyUrl));
    }

    @GetMapping("/errorConfirm")
    public String redirectWithUsingRedirectView(Model model, @ModelAttribute("message") String message)
    {
        model.addAttribute("message", message);
        return caseConfirmError;

    }

    @PostMapping(value = "/selCatg")
    public @ResponseBody Boolean refreshCaseForm4CatgSel(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {
        Boolean catgChanged = false;

        if (caseForm != null && userSessSrv != null)
        {
            if (!StringUtils.hasText(userSessSrv.getPreviousCategory()))
            {
                if (StringUtils.hasText(caseForm.getCatgDesc()))
                {
                    userSessSrv.setPreviousCategory(caseForm.getCatgDesc());
                    caseForm.setCatgChange(true);
                    log.info("Category changed by User ...");
                }
            }
            else
            {
                if (StringUtils.hasText(caseForm.getCatgDesc()))
                {
                    if (!userSessSrv.getPreviousCategory().equals(caseForm.getCatgDesc()))
                    {
                        userSessSrv.setPreviousCategory(caseForm.getCatgDesc());
                        caseForm.setCatgChange(true);
                        log.info("Category changed by User ...");
                    }
                    else
                    {
                        caseForm.setCatgChange(false);
                        log.info("Category not changed by User ...");
                    }
                }

            }
            userSessSrv.setCaseFormB4Submission(caseForm);

            log.info("Catg : " + caseForm.getCatgDesc());
            catgChanged = caseForm.isCatgChange();
        }

        return catgChanged;

    }

    @GetMapping("/refreshForm4SelCatg")
    public String refreshFormCxtx4SelCatg(HttpServletRequest request, Model model)
    {
        String caseViewForm = VWNamesDirectoryLocal.getViewName(EnumVWNames.caseForm, false, (String[]) null);
        if (userSessSrv != null)
        {
            TY_Case_Form caseForm = userSessSrv.getCaseFormB4Submission();

            if (caseForm != null)
            {
                userSessSrv.setCaseFormB4Submission(null);

                // Normal Scenario - Catg. chosen Not relevant for Notes Template and/or
                // additional fields

                if ((StringUtils.hasText(userSessSrv.getUserDetails4mSession().getAccountId())
                        || StringUtils.hasText(userSessSrv.getUserDetails4mSession().getEmployeeId()))
                        && !CollectionUtils.isEmpty(catgCusSrv.getCustomizations()))
                {

                    TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();

                    if (cusItem != null)
                    {

                        model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                        // Populate User Details
                        TY_UserESS userDetails = new TY_UserESS();
                        userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                        model.addAttribute("userInfo", userDetails);

                        // clear Form errors on each refresh or a New Case form request
                        if (CollectionUtils.isNotEmpty(userSessSrv.getFormErrors()))
                        {
                            userSessSrv.clearFormErrors();
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
                        // Check if LoB Specific Case Form is configured
                        if (StringUtils.hasText(cusItem.getCaseFormView()))
                        {
                            caseViewForm = cusItem.getCaseFormView();
                        }

                    }
                    else
                    {

                        throw new EX_ESMAPI(msgSrc.getMessage("ERR_CASE_TYPE_NOCFG", new Object[]
                        { userSessSrv.getCurrentLOBConfig().getCaseTypeEnum().toString() }, Locale.ENGLISH));
                    }

                }
            }

        }

        return caseViewForm;
    }

    @GetMapping("/refreshForm4AttUpload")
    public String refreshCaseFormPostAttachmentUpload(Model model)
    {
        String viewName = userSessSrv.getCurrentLOBConfig().getCaseFormView();

        if (attSrv != null && userSessSrv != null)
        {
            TY_CatgCusItem cusItem = userSessSrv.getCurrentLOBConfig();
            if (cusItem != null)
            {

                TY_Case_Form caseForm = userSessSrv.getCaseFormB4Submission();

                model.addAttribute("caseTypeStr", cusItem.getCaseTypeEnum().toString());

                // Populate User Details
                TY_UserESS userDetails = new TY_UserESS();
                if (userSessSrv != null)
                {
                    log.info("User Bound in Session..");
                }
                userDetails.setUserDetails(userSessSrv.getUserDetails4mSession());
                model.addAttribute("userInfo", userDetails);

                model.addAttribute("caseForm", caseForm);
                // also Place the form in Session
                userSessSrv.setCaseFormB4Submission(caseForm);

                model.addAttribute("formErrors", attSrv.getSessionMessages());

                // also Upload the Catg. Tree as per Case Type
                model.addAttribute("catgsList",
                        catalogTreeSrv.getCaseCatgTree4LoB(cusItem.getCaseTypeEnum()).getCategories());

                model.addAttribute("attachments", attSrv.getAttachmentNames());

                // Attachment file Size
                model.addAttribute("attSize", rlConfig.getAllowedSizeAttachmentMB());
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
        return viewName;

    }

    @PostMapping(value = "/uploadAJAX")
    public boolean uploadFileAttachments(@ModelAttribute("caseForm") TY_Case_Form caseForm, Model model)
    {
        Boolean uploadSuccess = false;

        if (caseForm != null && attSrv != null && userSessSrv != null)
        {

            log.info("Processing of Case Attachment Upload Form - UI layer :Begins....");
            if (caseForm.getAttachment() != null)
            {
                if (StringUtils.hasText(caseForm.getAttachment().getOriginalFilename()))
                {
                    // Clear Attachment Service Session Messages for subsequent roundtip
                    attSrv.clearSessionMessages();
                    if (!attSrv.addAttachment(caseForm.getAttachment()))
                    {
                        // Attachment to Local Storage Persistence Error
                        uploadSuccess = false;
                        userSessSrv.addFormErrors(msgSrc.getMessage("ERR_ATT_BYTES", new Object[]
                        { caseForm.getAttachment().getOriginalFilename(), attSrv.getSessionMessages().get(0) },
                                Locale.ENGLISH));

                    }
                    else
                    {
                        uploadSuccess = true;
                    }

                }

            }

            // Clear form for New Attachment as Current Attachment already in Container
            caseForm.setAttachment(null);
            caseForm.setCatgChange(false);
            caseForm.setCatgText(null);
            if (userSessSrv.getUserDetails4mSession().isEmployee())
            {
                caseForm.setEmployee(true);
            }
            // Scan for Template Load
            TY_CatgTemplates catgTemplate = catalogTreeSrv.getTemplates4Catg(caseForm.getCatgDesc(),
                    userSessSrv.getCurrentLOBConfig().getCaseTypeEnum());
            if (catgTemplate != null)
            {

                // Set Questionnaire for Category
                caseForm.setTemplate(catgTemplate.getQuestionnaire());

            }
            userSessSrv.setCaseFormB4Submission(caseForm);

            log.info("Processing of Case Attachment Upload Form - UI layer :Ends....");
        }

        return uploadSuccess;
    }

    @PostMapping(value = "/uploadAJAXReply")
    public boolean uploadFileAttachmentsReply(@ModelAttribute("caseEditForm") TY_CaseEdit_Form caseReplyForm)
    {
        boolean uploadSuccess = false;

        if (caseReplyForm != null && userSessSrv != null)
        {
            log.info("Processing of Case Attachment Upload Form - UI layer :Begins....");
            if (StringUtils.hasText(caseReplyForm.getCaseDetails().getCaseGuid()))
            {
                // Get Case Details
                TY_CaseEdit_Form caseEditForm = userSessSrv
                        .getCaseDetails4Edit(caseReplyForm.getCaseDetails().getCaseGuid());

                // Super Impose Reply from User Form 4m Session
                caseEditForm.setReply(caseReplyForm.getReply());

                // Clear form for New Attachment as Current Attachment already in Container
                caseEditForm.setAttachment(null);

                if (caseReplyForm.getAttachment() != null)
                {
                    if (StringUtils.hasText(caseReplyForm.getAttachment().getOriginalFilename()))
                    {
                        // Clear Attachment Service Session Messages for subsequent roundtip
                        attSrv.clearSessionMessages();
                        if (!attSrv.addAttachment(caseReplyForm.getAttachment()))
                        {
                            // Attachment to Local Storage Persistence Error
                            // attMsgs = attSrv.getSessionMessages();
                            uploadSuccess = false;
                            // ERR_ATT_BYTES= Error Accessing Binary Data from attachment - {0}, Details -
                            // {1}. Not able to upload.
                            userSessSrv.addFormErrors(msgSrc.getMessage("ERR_ATT_BYTES", new Object[]
                            { caseReplyForm.getAttachment().getOriginalFilename(), attSrv.getSessionMessages().get(0) },
                                    Locale.ENGLISH));
                            log.error("Attachment to Local Storage Persistence Error");

                        }
                        else
                        {
                            uploadSuccess = true;
                        }

                    }

                }
                userSessSrv.setCaseEditFormB4Submission(caseEditForm);

            }

        }

        return uploadSuccess;

    }

}
