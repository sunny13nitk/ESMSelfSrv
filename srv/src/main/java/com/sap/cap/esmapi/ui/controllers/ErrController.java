package com.sap.cap.esmapi.ui.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/err")
@Slf4j
public class ErrController
{

        @GetMapping("/access-denied")
        public ModelAndView showAccessDenied()
        {
                ModelAndView mv = new ModelAndView();
                mv.setViewName("error");
                mv.addObject("formError",
                                "Role(s)/Authorization to access this application are not assigned to you. Please contact your administrator to get the required role(s) assigned.");
                log.error("Invalid Token! Access to app not possible. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window. ");

                return mv;
        }

        @GetMapping("/exception")
        public ModelAndView showGenericEXception(@RequestParam(name = "msg", required = false) String msg)
        {
                ModelAndView mv = new ModelAndView();
                mv.setViewName("error");
                mv.addObject("formError", msg != null ? msg
                                : "An unexpected error occurred. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window.");
                log.error("Generic Exception occurred. Message: " + (msg != null ? msg
                                : "An unexpected error occurred. Try clearing browser history and cookies and reaccessing the app. You can also try logging in via a private/Incognito window."));

                return mv;
        }
}
