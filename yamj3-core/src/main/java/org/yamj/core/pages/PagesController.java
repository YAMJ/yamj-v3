package org.yamj.core.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.yamj.common.model.YamjInfo;
import org.yamj.core.api.json.SystemInfoController;

@Controller
public class PagesController {

    private static final Logger LOG = LoggerFactory.getLogger(PagesController.class);
    @Autowired
    SystemInfoController sic;

    @RequestMapping(value="/")
    public ModelAndView displayIndex() {
        ModelAndView view = new ModelAndView("index");
        return view;
    }

    @RequestMapping(value = "/system-info")
    public ModelAndView displaySystemInfo() {
        ModelAndView view = new ModelAndView("system-info");
        YamjInfo yi = sic.getYamjInfo("true");
        view.addObject("yi", yi);
        view.addObject("countlist", yi.getCounts());
        return view;
    }
}
