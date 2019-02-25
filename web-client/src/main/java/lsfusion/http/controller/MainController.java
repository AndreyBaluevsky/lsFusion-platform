package lsfusion.http.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.RawFileData;
import lsfusion.base.ServerMessages;
import lsfusion.gwt.server.FileUtils;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.logics.ServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MainController {

    @Autowired
    LogicsProvider logicsProvider;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String processLogin(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getServerSettings(request);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsLogo", getLogicsLogo(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));

        model.addAttribute("jnlpUrls", getJNLPUrls(request, serverSettings));

        String error = serverSettings != null ? BaseUtils.checkClientVersion(serverSettings.platformVersion, serverSettings.apiVersion, BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion()) : null;
        if (error != null) {
            model.addAttribute("error", error);
            return "restricted";
        } else {
            return "login";
        }
    }

    @RequestMapping(value = "/main", method = RequestMethod.GET)
    public String processMain(ModelMap model, HttpServletRequest request) {
        ServerSettings serverSettings = getServerSettings(request);

        model.addAttribute("title", getTitle(serverSettings));
        model.addAttribute("logicsIcon", getLogicsIcon(serverSettings));
        model.addAttribute("logicsName", getLogicsName(serverSettings));

        return "main";
    }

    private ServerSettings getServerSettings(HttpServletRequest request) {
        return logicsProvider.getServerSettings(request);
    }

    private String getTitle(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.displayName != null ? serverSettings.displayName : "lsfusion";
    }

    private String getLogicsLogo(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsLogo != null ? getFileUrl(serverSettings.logicsLogo) : "static/images/logo.png";
    }

    private String getLogicsIcon(ServerSettings serverSettings) {
        return serverSettings != null && serverSettings.logicsIcon != null ? getFileUrl(serverSettings.logicsIcon) : "favicon.ico";
    }
    
    private String getLogicsName(ServerSettings serverSettings) {
        return serverSettings != null ? serverSettings.logicsName : null;
    }

    private String getJNLPUrls(HttpServletRequest request, ServerSettings serverSettings) {
        String jnlpUrls = serverSettings != null && serverSettings.jnlpUrls != null ? serverSettings.jnlpUrls : "";
        return "<details><summary><a href=" + request.getContextPath() + "/exec?action=Security.generateJnlp%5BVARSTRING%5B10%5D,VARSTRING%5B1000%5D%5D>"
                + ServerMessages.getString(request, "run.desktop.client") + "</a></summary>" + jnlpUrls + "</details>";
    }

    private String getFileUrl(RawFileData file) {
        return GwtSharedUtils.getDownloadURL(FileUtils.saveApplicationFile(file), null, null, false);
    }
}